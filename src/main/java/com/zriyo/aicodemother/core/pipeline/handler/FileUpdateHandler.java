package com.zriyo.aicodemother.core.pipeline.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zriyo.aicodemother.ai.factory.AiCodeGeneratorServiceFactoryV2;
import com.zriyo.aicodemother.ai.service.AiCodeGenTypeRoutingServiceImpl;
import com.zriyo.aicodemother.core.handler.AiContextHolder;
import com.zriyo.aicodemother.core.pipeline.GenerationContext;
import com.zriyo.aicodemother.core.pipeline.service.CodeGenRecordService;
import com.zriyo.aicodemother.exception.BusinessException;
import com.zriyo.aicodemother.exception.ErrorCode;
import com.zriyo.aicodemother.model.AppConstant;
import com.zriyo.aicodemother.model.MonitorContext;
import com.zriyo.aicodemother.model.dto.InvestigationResult;
import com.zriyo.aicodemother.model.dto.ModificationPlanDTO;
import com.zriyo.aicodemother.model.dto.ProjectSkeletonDTO;
import com.zriyo.aicodemother.model.enums.AiCodeGenStage;
import com.zriyo.aicodemother.model.message.StreamMessageTypeEnum;
import com.zriyo.aicodemother.service.AiToolLogService;
import com.zriyo.aicodemother.service.ChatHistoryService;
import com.zriyo.aicodemother.util.SseEventBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.annotation.Order;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Map;

@Slf4j
@Component
@Order(2)
public class FileUpdateHandler extends AbstractCodeGenHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public FileUpdateHandler(CodeGenRecordService codeGenRecordService,
                             ChatHistoryService chatHistoryService,
                             AiCodeGenTypeRoutingServiceImpl aiCodeGenTypeRoutingService,
                             AiToolLogService aiToolLogService, ApplicationEventPublisher publisher, AiCodeGeneratorServiceFactoryV2 aiCodeGeneratorServiceFactory) {
        super(codeGenRecordService, chatHistoryService, aiToolLogService, publisher, aiCodeGenTypeRoutingService);
    }

    @Override
    protected AiCodeGenStage getStage() {
        return AiCodeGenStage.MODIFY;
    }

    @Override
    protected boolean shouldSkip(GenerationContext context) {
        return Boolean.TRUE.equals(context.getIsFirstBuild()) || context.getRuntimeFeedback() != null;
    }

    @Override
    protected Flux<ServerSentEvent<Object>> doExecute(GenerationContext context) {
        ProjectSkeletonDTO skeleton = context.getSkeleton();
        String message = context.getMessage();

        return Flux.<ServerSentEvent<Object>>create(sink -> {
            AiContextHolder.set(MonitorContext.builder()
                    .appId(String.valueOf(context.getAppId()))
                    .userId(String.valueOf(context.getUserId()))
                    .build());

            try {
                StringBuilder sb = new StringBuilder();
                sb.append("【1. 当前项目文件骨架（只读，不允许虚构文件）】\n");
                sb.append(objectMapper.writeValueAsString(skeleton)).append("\n\n");
                sb.append("【2. 用户需求】\n").append(message).append("\n\n");

                sink.next(SseEventBuilder.of(
                        StreamMessageTypeEnum.TOOL_REQUEST,
                        "正在分析项目结构与修改需求..."
                ));

                InvestigationResult fileLst = (InvestigationResult) super.invokeCodeGenType(context, INVESTIGATE, sb.toString());
                String path = AppConstant.TMP_DIR + "/" + AppConstant.APP_GEN_FILE_PATH + "/" + AppConstant.VUE_PROJECT_PREFIX + context.getAppId();
                sb.append("【3. Source Code Context (已读取的源代码原文)】\n");

                if (fileLst.getFiles() != null && !fileLst.getFiles().isEmpty()) {
                    for (String relativePath : fileLst.getFiles()) {
                        String content = this.readFile(path , relativePath);
                        sb.append("--- File: ").append(relativePath).append(" ---\n");
                        sb.append(content).append("\n\n");
                    }
                } else {
                    sb.append("(未读取到相关文件内容，请基于骨架进行通用逻辑设计)\n");
                }
                ModificationPlanDTO plan = (ModificationPlanDTO) super.invokeCodeGenType(context, UPDATE, sb.toString());

                Long toolMessageId = updateMessage(context, plan);

                context.setToolMassageId(toolMessageId);
                stopGeneration(context);
                syncSkeletonWithPlan(skeleton, plan);

                context.setModificationPlan(plan);

                if (plan.getThought() != null) {
                    sink.next(SseEventBuilder.of(
                            StreamMessageTypeEnum.TOOL_PROCESS,
                            plan.getThought()
                    ));
                }

                sink.next(SseEventBuilder.of(
                        StreamMessageTypeEnum.TOOL_EXECUTED,
                        "修改方案规划完毕，准备执行..."
                ));

                chatHistoryService.updateSkeleton(
                        skeleton,
                        context.getSkeletonId()
                );

                sink.complete();

            } catch (JsonProcessingException e) {
                log.error("JSON 转换错误", e);
                sink.error(new BusinessException(ErrorCode.SYSTEM_ERROR, "JSON 转换错误"));
            } catch (Exception e) {
                context.setIsError(true);
                context.setTerminated(true);
                log.error("修改分析阶段失败", e);
                sink.error(new BusinessException(ErrorCode.PARAMS_ERROR, "修改分析失败: " + e.getMessage()));
            }
        });
    }

    /**
     * 同步 AI 修改计划到项目骨架
     */
    private void syncSkeletonWithPlan(ProjectSkeletonDTO skeleton, ModificationPlanDTO plan) {
        if (plan == null || plan.getTasks() == null) return;
        if (skeleton == null || skeleton.getFiles() == null) return;

        Map<String, ProjectSkeletonDTO.FileInfo> files = skeleton.getFiles();

        for (ModificationPlanDTO.FileTask task : plan.getTasks()) {
            if (task == null) continue;

            String filePath = task.getFilePath();
            ModificationPlanDTO.ActionType action = task.getAction();

            if (filePath == null || action == null) continue;

            switch (action) {
                case CREATE: {
                    if (task.getInterfaceDef() == null) {
                        throw new IllegalStateException("CREATE 文件必须提供 interfaceDef: " + filePath);
                    }
                    ProjectSkeletonDTO.FileInfo newFile = new ProjectSkeletonDTO.FileInfo();
                    newFile.setFilePath(filePath);
                    newFile.setDescription(task.getFileDescription() != null ? task.getFileDescription() : "Auto generated file");
                    newFile.setType(task.getFileType() != null ? task.getFileType() : guessFileType(filePath));
                    newFile.setExports(task.getExports());
                    newFile.setInterfaceDef(task.getInterfaceDef());
                    newFile.setImports(new ArrayList<>());
                    newFile.setDependencies(new ArrayList<>());
                    newFile.setLocalDependencies(new ArrayList<>());
                    files.put(filePath, newFile);
                    break;
                }
                case MODIFY: {
                    ProjectSkeletonDTO.FileInfo existing = files.get(filePath);
                    if (existing == null) break;
                    if (task.getFileDescription() != null) existing.setDescription(task.getFileDescription());
                    if (task.getExports() != null) existing.setExports(task.getExports());
                    if (task.getInterfaceDef() != null) existing.setInterfaceDef(task.getInterfaceDef());
                    break;
                }
                default:
                    break;
            }
        }
    }

    private String guessFileType(String filePath) {
        if (filePath == null) return "unknown";
        if (filePath.endsWith(".vue")) return "vue";
        if (filePath.endsWith(".js")) return "js";
        if (filePath.endsWith(".ts")) return "ts";
        if (filePath.endsWith(".css")) return "css";
        if (filePath.endsWith(".html")) return "html";
        if (filePath.endsWith(".json")) return "json";
        return "file";
    }


    private String readFile(String fullPath, String relativePath) {
        try {
            String cleanRelPath = relativePath.startsWith("@/") ? relativePath.replace("@/", "src/") : relativePath;
            java.nio.file.Path finalPath = java.nio.file.Paths.get(fullPath, cleanRelPath);

            if (!java.nio.file.Files.exists(finalPath)) return "[Error: File Not Found]";

            // 1. 读取原文
            String raw = java.nio.file.Files.readString(finalPath, java.nio.charset.StandardCharsets.UTF_8);

            // 2. 基础脱水处理 (解决冲突 + 移除干扰)
            String processed = raw.replace("${", "$ {")
                    .replace("{{", "{ {")
                    .replaceAll("(?s)/\\*.*?\\*/|(?<!http:|https:)//.*", "") // 移除注释
                    .replaceAll("(?m)^\\s*\\r?\\n", "")                   // 移除空行
                    .trim();

            // 3. 针对 Vue 的智能切片策略
            // 单文件字符上限，约 1.5k Token
            int MAX_FILE_CHARS = 5000;
            if (processed.length() > MAX_FILE_CHARS && cleanRelPath.endsWith(".vue")) {
                // 尝试匹配 <script> 和 <template>
                String script = extractTag(processed, "script");
                String template = extractTag(processed, "template");

                // 生产级容错：如果正则没抓到内容，则进行物理硬截断
                if (script.isEmpty() && template.isEmpty()) {
                    return processed.substring(0, MAX_FILE_CHARS) + "\n...[Content Hard Truncated]";
                }

                return "[Content Strategy: Style Omitted]\n" + script + "\n" + template;
            }

            // 4. 针对普通 JS/CSS 的硬截断
            if (processed.length() > MAX_FILE_CHARS) {
                return processed.substring(0, MAX_FILE_CHARS) + "\n...[Content Hard Truncated]";
            }

            return processed;
        } catch (java.io.IOException e) {
            return "[Error: " + e.getMessage() + "]";
        }
    }

    /**
     * 鲁棒性更强的标签提取正则：支持 <script setup lang="ts"> 等复杂属性
     */
    private String extractTag(String content, String tag) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "(<" + tag + "[^>]*>.*?</" + tag + ">)",
                java.util.regex.Pattern.DOTALL | java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher m = p.matcher(content);
        return m.find() ? m.group(1) : "";
    }
}
