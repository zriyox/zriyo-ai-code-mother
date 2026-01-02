package com.zriyo.aicodemother.core.pipeline.handler;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zriyo.aicodemother.ai.AiCodeGeneratorServiceV2;
import com.zriyo.aicodemother.ai.factory.AiCodeGeneratorServiceFactoryV2;
import com.zriyo.aicodemother.ai.service.AiCodeGenTypeRoutingServiceImpl;
import com.zriyo.aicodemother.core.handler.AiContextHolder;
import com.zriyo.aicodemother.core.pipeline.FileGenerationOrder;
import com.zriyo.aicodemother.core.pipeline.GenerationContext;
import com.zriyo.aicodemother.core.pipeline.service.CodeGenRecordService;
import com.zriyo.aicodemother.model.AppConstant;
import com.zriyo.aicodemother.model.MonitorContext;
import com.zriyo.aicodemother.model.dto.ModificationPlanDTO;
import com.zriyo.aicodemother.model.dto.ProjectSkeletonDTO;
import com.zriyo.aicodemother.model.enums.AiCodeGenStage;
import com.zriyo.aicodemother.model.enums.CodeGenTypeEnum;
import com.zriyo.aicodemother.model.enums.ToolAction;
import com.zriyo.aicodemother.model.message.StreamMessageTypeEnum;
import com.zriyo.aicodemother.service.AiToolLogService;
import com.zriyo.aicodemother.service.ChatHistoryService;
import com.zriyo.aicodemother.util.ProjectDoctor;
import com.zriyo.aicodemother.util.RedisUtils;
import com.zriyo.aicodemother.util.SseEventBuilder;
import dev.langchain4j.service.TokenStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.annotation.Order;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * CodeFileWriteHandler - 代码文件生成核心处理器
 * 优化点：支持状态机闭环校验，防止 AI 幻觉和重复工具调用。
 */
@Component
@Order(3)
@Slf4j
public class CodeFileWriteHandler extends AbstractCodeGenHandler {

    private static final String FILE_PATH_IMPORT = "FILE_PATH_IMPORT:";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AiCodeGeneratorServiceFactoryV2 aiCodeGeneratorServiceFactory;

    public CodeFileWriteHandler(AiCodeGeneratorServiceFactoryV2 aiCodeGeneratorServiceFactory,
                                CodeGenRecordService codeGenRecordService,
                                ChatHistoryService chatHistoryService,
                                AiToolLogService aiToolLogService,
                                ApplicationEventPublisher publisher,
                                AiCodeGenTypeRoutingServiceImpl aiCodeGenTypeRoutingService) {
        super(codeGenRecordService, chatHistoryService, aiToolLogService, publisher, aiCodeGenTypeRoutingService);
        this.aiCodeGeneratorServiceFactory = aiCodeGeneratorServiceFactory;
    }

    @Override
    protected AiCodeGenStage getStage() {
        return AiCodeGenStage.CODE_GENERATION;
    }

    @Override
    protected boolean shouldSkip(GenerationContext context) {
        return context.getRuntimeFeedback() != null;
    }

    @Override
    protected Flux<ServerSentEvent<Object>> doExecute(GenerationContext context) {
        // 根据是否为首次构建分发逻辑
        return Boolean.FALSE.equals(context.getIsFirstBuild()) ? handleModification(context) : handleFirstBuild(context);
    }

    /**
     * 分支 1: 首次全量构建逻辑
     */
    private Flux<ServerSentEvent<Object>> handleFirstBuild(GenerationContext context) {
        Long appId = context.getAppId();
        String projectDirName = com.zriyo.aicodemother.model.AppConstant.VUE_PROJECT_PREFIX + appId;
        ProjectSkeletonDTO skeleton = context.getSkeleton();

        if (skeleton == null || skeleton.getFiles() == null) return Flux.empty();

        List<String> filePathsSafeOrder = FileGenerationOrder.computeSafeOrder(skeleton);
        context.setGeneratedFiles(filePathsSafeOrder);

        return Flux.fromIterable(filePathsSafeOrder)
                .concatMap(filePath -> {
                    if (stopGeneration(context)) return stopMessage();
                    String newFilePath = projectDirName + "/" + filePath;
                    ProjectSkeletonDTO.FileInfo fileInfo = skeleton.getFiles().get(filePath);

                    // 1. 注入依赖上下文到 Redis
                    List<String> deps = buildFileList(fileInfo);
                    if (!deps.isEmpty()) RedisUtils.setCacheObject(FILE_PATH_IMPORT + newFilePath, deps);

                    // 2. 准备 Prompt 与清除旧服务实例（确保重试时上下文干净）
                    String fullPrompt = buildFilePrompt(context.getMessage(), fileInfo, skeleton, projectDirName);
                    aiCodeGeneratorServiceFactory.invalidateService(newFilePath, context.getCodeGenType());

                    AiCodeGeneratorServiceV2 aiService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(
                            newFilePath, context.getCodeGenType(), projectDirName, appId);

                    // 3. 执行生成并配置重试策略
                    return generateSingleFileFlux(aiService, fullPrompt, context, filePath)
                            .retryWhen(createRetrySpec(filePath));
                });
    }

    /**
     * 分支 2: 修改/新增逻辑
     */
    private Flux<ServerSentEvent<Object>> handleModification(GenerationContext context) {
        Long appId = context.getAppId();
        String projectDirName = com.zriyo.aicodemother.model.AppConstant.VUE_PROJECT_PREFIX + appId;
        ModificationPlanDTO plan = context.getModificationPlan();

        if (plan == null || plan.getTasks() == null) return Flux.empty();

        return Flux.fromIterable(plan.getTasks())
                .concatMap(task -> {
                    if (stopGeneration(context)) return stopMessage();
                    String filePath = task.getFilePath();
                    String fullPath = projectDirName + "/" + filePath;

                    if (task.getReferenceFiles() != null) {
                        RedisUtils.setCacheObject(FILE_PATH_IMPORT + fullPath, task.getReferenceFiles());
                    }

                    String existingContent = FileUtil.exist(fullPath) ? FileUtil.readUtf8String(fullPath) : null;
                    String fullPrompt = buildModificationPrompt(task, context.getMessage(), context.getSkeleton(), existingContent,context.getAppId());

                    aiCodeGeneratorServiceFactory.invalidateService(fullPath, CodeGenTypeEnum.VUE_PROJECT);
                    AiCodeGeneratorServiceV2 aiService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(
                            fullPath, CodeGenTypeEnum.VUE_PROJECT, projectDirName, appId);

                    return generateSingleFileFlux(aiService, fullPrompt, context, filePath)
                            .retryWhen(createRetrySpec(filePath));
                });
    }

    /**
     * 核心逻辑：异步生成流处理，包含幻觉拦截
     */
    private Flux<ServerSentEvent<Object>> generateSingleFileFlux(
            AiCodeGeneratorServiceV2 aiService,
            String prompt,
            GenerationContext context,
            String filePath) {

        return Flux.create(sink -> {
            AiContextHolder.set(MonitorContext.builder()
                    .userId(String.valueOf(context.getUserId()))
                    .appId(String.valueOf(context.getAppId()))
                    .build());

            long startTime = System.currentTimeMillis();
            // 获取 TokenStream
            TokenStream tokenStream = routeInvokeStream(aiService, context, prompt, filePath);

            // 通知前端：开始准备当前文件
            sink.next(SseEventBuilder.of(StreamMessageTypeEnum.CODE_TOOL_REQUEST, filePath));

            AtomicBoolean toolExecuted = new AtomicBoolean(false);
            AtomicBoolean finishRepairCalled = new AtomicBoolean(false);

            tokenStream
                    // ✅ 必须调用此方法，即使你只关心工具调用。
                    .onPartialResponse(content -> {
                        // 即使不处理文本内容，也必须显式配置此监听器
                        log.debug("AI Text Stream: {}", content);
                    })
                    .onPartialToolExecutionRequest((index, request) -> {
                        toolExecuted.set(true);
                        if ("finishRepair".equals(request.name())) {
                            finishRepairCalled.set(true);
                        }

                        try {
                            JsonNode jsonNode = objectMapper.readTree(request.arguments());
                            if (jsonNode.has("content")) {
                                sink.next(SseEventBuilder.of(StreamMessageTypeEnum.CODE_TOOL_PROCESS, jsonNode.get("content").asText()));
                            }
                        } catch (Exception ignore) {}
                    })
                    .onToolExecuted(exec -> {savaToolLog(context, filePath, exec, startTime, ToolAction.WRITE);})
                    .onCompleteResponse(response -> {
                        String aiText = response.aiMessage() != null ? response.aiMessage().text() : "";

                        // ❌ 判定幻觉：AI 输出了内容但没有调用 writeFile 相关的工具
                        if (!toolExecuted.get() && StrUtil.isNotBlank(aiText) && aiText.trim().length() > 5) {
                            log.error("检测到 AI 幻觉：直接输出文本而非调用工具。文件: [{}]", filePath);
                            sink.error(new IllegalStateException("AI未调用工具内容"));
                            return;
                        }

                        sink.next(SseEventBuilder.of(StreamMessageTypeEnum.TOOL_DONE));
                        sink.complete();
                    })
                    .onError(err -> {
                        log.error("文件 [{}] 生成流异常: {}", filePath, err.getMessage());
                        sink.next(SseEventBuilder.of(StreamMessageTypeEnum.TOOL_ERROR, err.getMessage()));
                        sink.error(err); // 抛出异常触发重试
                    })
                    .start(); // 此时所有配置已就绪，校验将通过
        });
    }

    /**
     * 优化点：根据文件类型分化生成流
     */
    private TokenStream routeInvokeStream(AiCodeGeneratorServiceV2 aiService, GenerationContext context, String prompt, String filePath) {
        if (filePath.contains("main.js") || filePath.contains("App.vue") || filePath.contains("router/")) {
            return aiService.generateInfraCodeStream(prompt);
        }
        return aiService.generateComponentCodeStream(prompt);
    }

    /**
     * 策略定义：仅针对业务幻觉（IllegalStateException）进行 2 次快速重试
     */
    private reactor.util.retry.Retry createRetrySpec(String filePath) {
        return reactor.util.retry.Retry.backoff(2, Duration.ofSeconds(1))
                .filter(e -> e instanceof IllegalStateException)
                .doBeforeRetry(signal -> log.warn("业务层幻觉重试：文件 [{}] 第 {} 次", filePath, signal.totalRetries() + 1));
    }

    private List<String> buildFileList(ProjectSkeletonDTO.FileInfo fileInfo) {
        return (fileInfo == null || fileInfo.getLocalDependencies() == null)
                ? Collections.emptyList()
                : fileInfo.getLocalDependencies();
    }
    /**
     * 构建修改/新增文件的增强版 Prompt
     * 职责：确保 AI 在了解现有代码的基础上，严格遵守 Apple 视觉规范和工具调用协议进行增量修改。
     */
    private String buildModificationPrompt(ModificationPlanDTO.FileTask task, String userReq, ProjectSkeletonDTO skeleton, String existingContent, Long appId) {
        StringBuilder sb = new StringBuilder();
        try {
            // 1. 注入全局项目规范 (让 AI 始终记住设计语言)
            if (skeleton != null && skeleton.getGlobal() != null) {
                ObjectNode globalNode = objectMapper.createObjectNode();
                globalNode.set("styleGuide", objectMapper.valueToTree(skeleton.getGlobal().getStyleGuide()));
                globalNode.set("packageVersions", objectMapper.valueToTree(skeleton.getGlobal().getDependencies()));

                sb.append("【1. 全局项目规范 (必须遵守)】\n");
                sb.append(objectMapper.writeValueAsString(globalNode)).append("\n\n");
            }

            // 1.1 注入依赖上下文 (关键：告知 AI 修改时可以调用哪些现有接口)
            ObjectNode dependencyContextNode = objectMapper.createObjectNode();
            String projectDirName = AppConstant.VUE_PROJECT_PREFIX + appId;
            String projectPath = java.nio.file.Paths.get(ProjectDoctor.TMP_CODE_OUTPUT, projectDirName).toString();

            if (task.getReferenceFiles() != null) {
                for (String depPath : task.getReferenceFiles()) {
                    ProjectSkeletonDTO.FileInfo depInfo = skeleton.getFiles().get(depPath);
                    String depFullPath = projectPath + "/" + depPath;
                    if (depInfo != null && cn.hutool.core.io.FileUtil.exist(depFullPath)) {
                        ObjectNode depSummary = objectMapper.createObjectNode();
                        depSummary.put("filePath", depInfo.getFilePath());
                        depSummary.put("interfaceDef", depInfo.getInterfaceDef());
                        dependencyContextNode.set(depPath, depSummary);
                    }
                }
            }
            if (!dependencyContextNode.isEmpty()) {
                sb.append("【1.1 依赖上下文 (可调用的外部接口)】\n");
                sb.append(objectMapper.writeValueAsString(dependencyContextNode)).append("\n\n");
            }
        } catch (Exception e) {
            log.warn("序列化全局骨架信息失败", e);
        }

        // 2. 明确当前任务
        sb.append("【2. 当前任务描述】\n");
        String actionName = task.getAction() == ModificationPlanDTO.ActionType.CREATE ? "创建新文件" : "修改现有文件";
        sb.append("动作：").append(actionName).append("\n");
        sb.append("目标路径：").append(task.getFilePath()).append("\n");
        sb.append("逻辑契约 (Interface Definition)：").append(task.getInterfaceDef()).append("\n\n");

        // 3. 用户原始需求上下文
        sb.append("【3. 用户原始需求】\n");
        sb.append(userReq).append("\n\n");

        // 4. 核心执行约束 (防止 AI 乱改或只给片段)
        sb.append("【4. 强制执行约束 (Hard Constraints)】\n");
        sb.append("- **全量覆盖**：禁止输出 Diff 片段或局部修改，必须输出修复后的完整代码。\n");
        sb.append("- **单一职责**：只允许修改指定文件，严禁提及或猜测其他文件。\n");
        sb.append("- **视觉一致性**：必须严格沿用全局规范中的 Apple 设计风格及 Tailwind 配置。\n");
        sb.append("- **依赖闭环**：严禁引入项目骨架中未定义的第三方库或本地模块。\n");
        sb.append("- **语言偏好**：**UI 界面内的所有文字、标签、按钮必须使用中文。**\n\n");

        // 5. 注入现有代码内容 (仅在修改模式下)
        if (existingContent != null && !existingContent.isEmpty()) {
            sb.append("【5. 待修改的原始内容 (Reference)】\n");
            sb.append("请基于以下代码进行修改，保持原有正确逻辑不动，仅修复或实现指定功能：\n");
            sb.append("```\n").append(existingContent).append("\n```\n\n");
        }

        // 6. 最终物理协议指令 (防止重复调用和文本幻觉)
        sb.append("==============================================================\n");
        sb.append("⚠️ 最终物理协议 (Final Protocol):\n");
        sb.append("你现在是一个纯粹的函数调用代理 (Function Calling Agent)。\n");
        sb.append("1. **立即调用** `writeFile` 工具提交代码。严禁进行任何自我介绍或解释。\n");
        sb.append("2. **逻辑终结**：在 `writeFile` 成功后，必须立即调用 `finishRepair` 结束任务。\n");
        sb.append("3. **严禁幻觉**：禁止输出任何 Markdown 代码块 (```) 或自然语言文字。\n");
        sb.append("4. **路径锁定**：物理写入路径必须锁定为：").append(task.getFilePath()).append("\n");
        sb.append("==============================================================\n");

        return sb.toString();
    }
    /**
     * 构建首次生成文件的复杂 Prompt
     * 职责：为 AI 提供完整的依赖上下文和逻辑契约，确保生成的代码可直接运行并符合 Apple 视觉规范。
     */
    private String buildFilePrompt(String userMessage,
                                   ProjectSkeletonDTO.FileInfo fileInfo,
                                   ProjectSkeletonDTO skeleton,
                                   String projectDirName) {
        try {
            // 1. 全局规范 (StyleGuide + Dependencies)
            ObjectNode globalNode = objectMapper.createObjectNode();
            if (skeleton.getGlobal() != null) {
                globalNode.set("styleGuide", objectMapper.valueToTree(skeleton.getGlobal().getStyleGuide()));
                globalNode.set("packageVersions", objectMapper.valueToTree(skeleton.getGlobal().getDependencies()));
            }

            // 2. 依赖上下文 (只注入物理文件已存在的依赖接口)
            ObjectNode dependencyContextNode = objectMapper.createObjectNode();
            if (fileInfo.getLocalDependencies() != null) {
                for (String depPath : fileInfo.getLocalDependencies()) {
                    ProjectSkeletonDTO.FileInfo depInfo = skeleton.getFiles().get(depPath);
                    // 仅在物理文件已存在时提供接口定义，防止 AI 对尚未生成的依赖产生幻觉
                    String depFullPath = projectDirName + "/" + depPath;
                    if (depInfo != null && cn.hutool.core.io.FileUtil.exist(depFullPath)) {
                        ObjectNode depSummary = objectMapper.createObjectNode();
                        depSummary.put("filePath", depInfo.getFilePath());
                        depSummary.put("type", depInfo.getType());
                        depSummary.put("interfaceDef", depInfo.getInterfaceDef()); // 核心：告知依赖怎么用
                        depSummary.set("exports", objectMapper.valueToTree(depInfo.getExports()));

                        dependencyContextNode.set(depPath, depSummary);
                    }
                }
            }

            // 3. 当前文件生成规格
            ObjectNode currentFileNode = objectMapper.createObjectNode();
            currentFileNode.put("filePath", fileInfo.getFilePath());
            currentFileNode.put("description", fileInfo.getDescription());
            currentFileNode.put("interfaceDef", fileInfo.getInterfaceDef()); // 核心：告知当前文件要实现成什么样
            currentFileNode.set("imports", objectMapper.valueToTree(fileInfo.getImports()));
            currentFileNode.set("dependencies", objectMapper.valueToTree(fileInfo.getDependencies()));
            currentFileNode.set("templateComponents", objectMapper.valueToTree(fileInfo.getTemplateComponents()));

            StringBuilder sb = new StringBuilder();
            sb.append("【任务指令】\n");
            sb.append("请执行 writeFile 工具，为我生成文件：").append(fileInfo.getFilePath()).append("\n\n");

            sb.append("【1. 全局项目规范】\n");
            sb.append(objectMapper.writeValueAsString(globalNode)).append("\n\n");

            sb.append("【2. 依赖上下文 (可调用的本地模块接口)】\n");
            sb.append("若涉及调用以下模块，请严格遵守其 interfaceDef 定义：\n");
            sb.append(objectMapper.writeValueAsString(dependencyContextNode)).append("\n\n");

            sb.append("【3. 当前文件生成规格 (核心约束)】\n");
            sb.append(objectMapper.writeValueAsString(currentFileNode)).append("\n\n");

            // 优化：仅核心入口文件或逻辑极其复杂时才注入原始长需求
            // 这里判断是否为核心架构文件
            boolean isCoreFile = fileInfo.getFilePath().contains("main.js")
                    || fileInfo.getFilePath().contains("App.vue")
                    || fileInfo.getFilePath().contains("router/");

            if (isCoreFile) {
                sb.append("【4. 原始业务需求内容】\n");
                sb.append(userMessage).append("\n\n");
            } else {
                // 组件文件只提供 description，不再重复发送 5000 字的长需求
                sb.append("【4. 局部业务逻辑】\n");
                sb.append(fileInfo.getDescription()).append("\n\n");
            }

            // 5. 协议硬约束 (防止幻觉的关键)
            sb.append("==============================================================\n");
            sb.append("⚠️ 最终物理执行协议 (Final Protocol):\n");
            sb.append("你现在是一个严格的 Function Calling Agent。\n");
            sb.append("1. **立即调用** `writeFile` 提交代码。禁止任何 Markdown (```) 或解释性回复。\n");
            sb.append("2. **代码完整性**：必须严格实现 `interfaceDef` 中定义的 Props/Emits/函数签名，代码必须 100% 完整。\n");
            sb.append("3. **逻辑闭环**：在 `writeFile` 调用成功后，必须立即调用 `finishRepair` 结束本次会话。\n");
            sb.append("4. **视觉语言**：严格遵循 Global StyleGuide 中的 Apple 视觉规范，禁止使用 @apply。\n");
            sb.append("==============================================================\n");

            return sb.toString();

        } catch (Exception e) {
            log.error("构建首次生成 Prompt 失败: {}", fileInfo.getFilePath(), e);
            throw new RuntimeException("构建文件 prompt 失败", e);
        }
    }
}
