package com.zriyo.aicodemother.core.pipeline.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zriyo.aicodemother.ai.AiCodeGeneratorServiceV2;
import com.zriyo.aicodemother.ai.service.AiCodeGenTypeRoutingServiceImpl;
import com.zriyo.aicodemother.core.handler.AiContextHolder;
import com.zriyo.aicodemother.core.pipeline.CodeGenHandler;
import com.zriyo.aicodemother.core.pipeline.GenerationContext;
import com.zriyo.aicodemother.core.pipeline.service.CodeGenRecordService;
import com.zriyo.aicodemother.event.AppEvent;
import com.zriyo.aicodemother.model.AppConstant;
import com.zriyo.aicodemother.model.MonitorContext;
import com.zriyo.aicodemother.model.RedisConstants;
import com.zriyo.aicodemother.model.dto.ModificationPlanDTO;
import com.zriyo.aicodemother.model.dto.chat.ChatMessage;
import com.zriyo.aicodemother.model.entity.AiToolLog;
import com.zriyo.aicodemother.model.enums.AiCodeGenStage;
import com.zriyo.aicodemother.model.enums.ChatHistoryMessageTypeEnum;
import com.zriyo.aicodemother.model.enums.ToolAction;
import com.zriyo.aicodemother.model.message.StreamMessageTypeEnum;
import com.zriyo.aicodemother.service.AiToolLogService;
import com.zriyo.aicodemother.service.ChatHistoryService;
import com.zriyo.aicodemother.util.CodeOutputManager;
import com.zriyo.aicodemother.util.RedisUtils;
import com.zriyo.aicodemother.util.SseEventBuilder;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import jodd.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

@Slf4j
public abstract class AbstractCodeGenHandler extends CodeGenHandler {

    protected final CodeGenRecordService codeGenRecordService;
    protected final ChatHistoryService chatHistoryService;
    protected final AiToolLogService aiToolLogService;
    protected final ApplicationEventPublisher publisher;
    protected final ObjectMapper objectMapper = new ObjectMapper();
    protected final AiCodeGenTypeRoutingServiceImpl aiCodeGenTypeRoutingService;

    protected static final String SKELETON = "skeleton";
    protected static final String UPDATE = "update";
    protected static final String CODE_FILE = "codeFile";
    protected static final String FIX_BUG = "fixBug";
    protected static final String RUNTIME_FIX = "runtimeFix";
    protected static final String INVESTIGATE = "investigate";

    protected AbstractCodeGenHandler(CodeGenRecordService codeGenRecordService,
                                     ChatHistoryService chatHistoryService,
                                     AiToolLogService aiToolLogService,
                                     ApplicationEventPublisher publisher,
                                     AiCodeGenTypeRoutingServiceImpl aiCodeGenTypeRoutingService) {
        this.codeGenRecordService = codeGenRecordService;
        this.chatHistoryService = chatHistoryService;
        this.aiToolLogService = aiToolLogService;
        this.publisher = publisher;
        this.aiCodeGenTypeRoutingService = aiCodeGenTypeRoutingService;
    }

    protected abstract AiCodeGenStage getStage();

    protected boolean stopGeneration(GenerationContext context) {
        Boolean stopFlag = RedisUtils.getCacheObject(RedisConstants.AI_CODE_GEN_TASK_RUNNING + context.getAppId());
        if (Objects.nonNull(stopFlag) && !stopFlag) {
            log.info(">>> 阶段终止: {}", getStage().getValue());
            cleanupOnTermination(context);
            codeGenRecordService.fail(context, "阶段 " + getStage().getValue() + " 终止");
            long statTime = System.currentTimeMillis();
            try {
                savaToolMessage(context, "", "执行停止", "用户手动停止", statTime, statTime, ToolAction.STOP.getValue());
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            context.setTerminated(true);
            RedisUtils.deleteObject(RedisConstants.AI_CODE_GEN_TASK_RUNNING + context.getAppId());
            return true;
        }
        return false;
    }

    protected Long updateMessage(GenerationContext context, ModificationPlanDTO plan) throws JsonProcessingException {
        ChatMessage msg = new ChatMessage();
        msg.setMessage(String.format("✅ **项目就绪**\n———\n• 结构加载：✓\n• 当前操作：**%s**", plan.getThought().trim()));
        msg.setMessageType(ChatHistoryMessageTypeEnum.TOOL.getValue());
        msg.setAppId(context.getAppId());
        msg.setMetaData(objectMapper.writeValueAsString(plan));
        msg.setUserVisible(1);
        return chatHistoryService.addChatMessage(msg, context.getUserId());
    }

    protected Long ErrFixMessage(GenerationContext context) throws JsonProcessingException {
        ChatMessage msg = new ChatMessage();
        msg.setMessage(String.format("✅ **项目就绪**\n———\n• 结构加载：✓\n• 当前操作：**%s**", "执行修复 BUG"));
        msg.setMessageType(ChatHistoryMessageTypeEnum.TOOL.getValue());
        msg.setAppId(context.getAppId());
        msg.setUserVisible(1);
        return chatHistoryService.addChatMessage(msg, context.getUserId());
    }

    /**
     * 通用调用 非流式：确保在执行前锚定上下文
     */
    protected Object invokeCodeGenType(GenerationContext context, String type, String prompt) {
        setContextHolder(context);
        return switch (type) {
            case SKELETON -> aiCodeGenTypeRoutingService.initVueProject(prompt);
            case UPDATE -> aiCodeGenTypeRoutingService.addFeature(prompt);
            case INVESTIGATE -> aiCodeGenTypeRoutingService.investigation(prompt);
            default -> throw new IllegalArgumentException("未知调用类型: " + type);
        };
    }

    /**
     * 流式工具调用：确保在执行前锚定上下文
     */
    protected TokenStream invokeTokenStream(AiCodeGeneratorServiceV2 aiService, GenerationContext context, String prompt, String type) {
        setContextHolder(context);
        return switch (type) {
            case CODE_FILE -> aiService.generateVueProjectCodeTokenStreamTest(prompt);
            case FIX_BUG -> aiService.checkVueProjectBugTokenStream(prompt);
            case RUNTIME_FIX -> aiService.fixRuntimeLogicBugTokenStream(prompt);
            default -> throw new IllegalStateException("Unexpected value: " + type);
        };
    }

    /**
     * 关键方法：设置上下文，方便代理类和监听器获取 ID
     */
    private void setContextHolder(GenerationContext context) {
        MonitorContext build = MonitorContext.builder()
                .appId(String.valueOf(context.getAppId()))
                .userId(String.valueOf(context.getUserId()))
                .build();
        AiContextHolder.set(build);
    }

    protected Long createMessage(GenerationContext context) {
        ChatMessage msg = new ChatMessage();
        String details = "• 目录结构：已生成项目目录结构\n• 配置模板：已写入配置文件模板 \n• 项目状态：项目模板创建成功\n";
        msg.setMessage("✅ **项目就绪**\n———\n" + details);
        msg.setMessageType(ChatHistoryMessageTypeEnum.TOOL.getValue());
        msg.setAppId(context.getAppId());
        msg.setUserVisible(1);
        return chatHistoryService.addChatMessage(msg, context.getUserId());
    }

    protected void savaToolMessage(GenerationContext context, String filePath, String toolName, String description, long startTime, long endTime, String value) throws JsonProcessingException {
        AiToolLog aiToolLog = new AiToolLog();
        aiToolLog.setAiMessageId(context.getToolMassageId());
        aiToolLog.setSummary(description);
        aiToolLog.setToolName(toolName);
        aiToolLog.setFilePath(filePath);
        aiToolLog.setAction(value);
        aiToolLog.setCostTime((int) ((endTime - startTime) / 1000));
        aiToolLogService.save(aiToolLog);
    }

    protected void savaToolLog(GenerationContext context, String filePath, ToolExecution toolExecution, long startTime, ToolAction toolAction) {
        ToolExecutionRequest request = toolExecution.request();
        String toolName = request.name();
        String description = null;
        try {
            JsonNode args = objectMapper.readTree(request.arguments());
            if (args.has("description")) {
                description = args.get("description").asText(null);
            }
        } catch (Exception e) {
            log.warn("解析工具参数失败，toolName: {}, arguments: {}", toolName, request.arguments(), e);
        }
        log.info("【工具调用记录】工具名: {}, 操作描述: {}", toolName, description);
        try {
            long endTime = System.currentTimeMillis();
            if (StringUtil.isNotBlank(description)) {
                savaToolMessage(context, filePath, toolName, description, startTime, endTime, toolAction.getValue());
            }
        } catch (JsonProcessingException e) {
            log.error("保存工具消息出错", e);
        }
    }

    private void cleanupOnTermination(GenerationContext context) {
        log.info(">>> 阶段清理: {}", getStage());
        if (getStage().equals(AiCodeGenStage.SKELETON)) {
            Long appId = context.getAppId();
            Path sourceDir = CodeOutputManager.getSourceDirectory(AppConstant.VUE_PROJECT_PREFIX + appId);
            if (Files.exists(sourceDir)) {
                try {
                    CodeOutputManager.deleteRecursively(sourceDir);
                    log.info("✅ 项目源目录已删除: {}", sourceDir);
                } catch (Exception e) {
                    log.error("❌ 删除项目目录失败: {}", sourceDir, e);
                }
            }
            chatHistoryService.deleteSkeleton(appId, AiCodeGenStage.SKELETON);
        }
    }

    protected Flux<ServerSentEvent<Object>> stopMessage() {
        return Flux.just(SseEventBuilder.of(StreamMessageTypeEnum.CANCEL));
    }

    protected boolean shouldSkip(GenerationContext context) {
        return false;
    }

    protected abstract Flux<ServerSentEvent<Object>> doExecute(GenerationContext context);

    @Override
    protected Flux<ServerSentEvent<Object>> doHandle(GenerationContext context) {
        if (shouldSkip(context)) {
            log.info("Handler 跳过执行: {}", getStage().getValue());
            if (next != null && !context.isTerminated()) {
                return next.handle(context);
            }
            return Flux.empty();
        }

        AiCodeGenStage stage = getStage();
        String stageName = stage.getValue();
        log.info(">>> 阶段开始: {}", stageName);
        codeGenRecordService
                .start(context, stage);

        Flux<ServerSentEvent<Object>> executionFlow = doExecute(context)
                .onErrorResume(e -> {
                    RedisUtils.setCacheObject(RedisConstants.AI_CODE_GEN_TASK_RUNNING + context.getAppId(), false);
                    log.error(">>> 阶段彻底失败: {}", stageName, e);
                    codeGenRecordService.fail(context, "阶段 " + stageName + " 失败: " + e.getMessage());
                    context.setTerminated(true);
                    context.setIsError(true);
                    return Flux.just(SseEventBuilder.of(StreamMessageTypeEnum.ERROR, e.getMessage()));
                });

        return Flux.concat(executionFlow)
                .concatWith(Flux.defer(() -> {
                    if (!context.isTerminated() && next != null) {
                        codeGenRecordService.success(context);
                        return next.handle(context);
                    }
                    if (!context.isTerminated()) {
                        handleFinalEvent(context);
                        return Flux.just(SseEventBuilder.of(StreamMessageTypeEnum.AI_DONE));
                    }
                    return Flux.empty();
                }))
                .doFinally(signal -> AiContextHolder.remove());
    }

    private void handleFinalEvent(GenerationContext context) {
        if (!context.getIsError()) {
            if (context.getIsFirstBuild()) {
                ChatMessage chatMessage = chatHistoryService.buildUserInfo(
                        context.getAppId(),
                        "🎉 **项目已成功生成并验证通过！**\n\n" +
                                "您的 **Vue 3 + Vite** 应用已构建完成。  \n" +
                                "代码已安全保存，可随时部署和浏览。\n\n" +
                                "👇 点击按钮即可预览项目",
                        ChatHistoryMessageTypeEnum.AI,
                        true
                );
                chatHistoryService.addChatMessage(chatMessage, context.getUserId());
                codeGenRecordService.success(context);
            }
        } else {
            ChatMessage chatMessage = chatHistoryService.buildUserInfo(
                    context.getAppId(),
                    "❌ **项目生成失败！**\n\n" +
                            "系统内部错误,请重新生成项目!",
                    ChatHistoryMessageTypeEnum.AI,
                    true
            );
            cleanupOnTermination(context);
            chatHistoryService.addChatMessage(chatMessage, context.getUserId());
            codeGenRecordService.fail(context, "项目生成失败");
        }
        publisher.publishEvent(new AppEvent(this, context.getAppId(), context.getOosUrl()));
        RedisUtils.deleteObject(RedisConstants.AI_CODE_GEN_TASK_RUNNING + context.getAppId());
    }
}
