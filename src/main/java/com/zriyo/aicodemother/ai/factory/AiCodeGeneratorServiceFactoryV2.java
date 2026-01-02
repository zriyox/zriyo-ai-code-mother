package com.zriyo.aicodemother.ai.factory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zriyo.aicodemother.ai.AiCodeGeneratorServiceV2;
import com.zriyo.aicodemother.ai.AiCodeOptimizerService;
import com.zriyo.aicodemother.ai.guardrail.PromptSafetyInputGuardrail;
import com.zriyo.aicodemother.ai.tools.CodeReadTool;
import com.zriyo.aicodemother.ai.tools.CodeWriteTool;
import com.zriyo.aicodemother.ai.tools.RuntimeFixTool;
import com.zriyo.aicodemother.exception.BusinessException;
import com.zriyo.aicodemother.exception.ErrorCode;
import com.zriyo.aicodemother.model.enums.CodeGenTypeEnum;
import com.zriyo.aicodemother.service.ChatHistoryService;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
public class AiCodeGeneratorServiceFactoryV2 {

    @Autowired
    private RedisChatMemoryStore redisChatMemoryStore;

    @Autowired
    private ChatHistoryService chatHistoryService;

    @Autowired
    private ChatModel chatModel;


    @Autowired
    private StreamingChatModel streamingChatModel;

    /**
     * AI 服务实例缓存策略
     */
    private final Cache<String, AiCodeGeneratorServiceV2> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) -> {
                log.debug("AI 服务实例被移除，key: {}, 原因: {}", key, cause);
            })
            .build();

    public AiCodeGeneratorServiceV2 getAiCodeGeneratorService(String filePath, CodeGenTypeEnum codeGenType, String filePathProject, Long appId) {
        return serviceCache.get(getCacheKey(filePath, codeGenType), key -> createAiCodeGeneratorService(appId, filePath, codeGenType, filePathProject));
    }

    public AiCodeGeneratorServiceV2 getAiCodeService(Long AppId) {
        return serviceCache.get(getCodeCacheKey(AppId), key -> CodeDialogService(AppId));
    }

    public AiCodeGeneratorServiceV2 getAiErrorCodeGeneratorService(String filePath, CodeGenTypeEnum codeGenType, String filePathProject, Long appId) {
        return serviceCache.get(getErrorCacheKey(filePath, codeGenType), key -> createErrorAiCodeGeneratorService(appId, filePath, codeGenType, filePathProject));
    }


    public void invalidateService(String filePath, CodeGenTypeEnum codeGenType) {
        serviceCache.invalidate(getCacheKey(filePath, codeGenType));
        serviceCache.invalidate(getErrorCacheKey(filePath, codeGenType));
        log.info("已失效 AI 服务缓存: {}", filePath);
    }


    private AiCodeGeneratorServiceV2 createAiCodeGeneratorService(Long appId, String filePath, CodeGenTypeEnum codeGenType, String filePathProject) {
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .id("code:" + filePath)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(30)
                .build();

        chatHistoryService.loadChatHistoryToToolMemoryV2(appId, filePath, chatMemory, filePathProject);


        return switch (codeGenType) {
            case VUE_PROJECT -> AiServices.builder(AiCodeGeneratorServiceV2.class)
                    .streamingChatModel(streamingChatModel)
                    .tools(new CodeWriteTool(filePath))
                    .chatMemory(chatMemory)
                    .maxSequentialToolsInvocations(20)
                    .hallucinatedToolNameStrategy(req ->
                            ToolExecutionResultMessage.from(req, "Error: there is no tool called " + req.name()))
                    .inputGuardrails(new PromptSafetyInputGuardrail())
                    .build();
            default ->
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的代码生成类型: " + codeGenType.getValue());
        };
    }

    private AiCodeGeneratorServiceV2 CodeDialogService(Long appId) {
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .id("codeDialog:" + appId + System.currentTimeMillis())
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(5)
                .build();


        return AiServices.builder(AiCodeGeneratorServiceV2.class)
                .streamingChatModel(streamingChatModel)
                .chatMemory(chatMemory)
                .hallucinatedToolNameStrategy(req ->
                        ToolExecutionResultMessage.from(req, "Error: there is no tool called " + req.name()))
                .build();

    }

    private AiCodeGeneratorServiceV2 createErrorAiCodeGeneratorService(Long appId, String filePath, CodeGenTypeEnum codeGenType, String filePathProject) {
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .id("errorCode:" + filePath)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(30)
                .build();

        chatHistoryService.loadErrorChatHistoryToToolMemoryV2(appId, filePath, chatMemory, filePathProject);


        return switch (codeGenType) {
            case VUE_PROJECT -> AiServices.builder(AiCodeGeneratorServiceV2.class)
                    .streamingChatModel(streamingChatModel)
                    .tools(new RuntimeFixTool(appId))
                    .chatMemory(chatMemory)
                    .maxSequentialToolsInvocations(20)
                    .inputGuardrails(new PromptSafetyInputGuardrail())
                    .hallucinatedToolNameStrategy(req ->
                            ToolExecutionResultMessage.from(req, "Error: there is no tool called " + req.name()))
                    .build();
            default ->
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的代码生成类型: " + codeGenType.getValue());
        };
    }


    public AiCodeOptimizerService createAiCodeOptimizerService(Long appId) {
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .id("toolCode:" + appId + System.currentTimeMillis())
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(30)
                .build();
        return AiServices.builder(AiCodeOptimizerService.class)
                .chatModel(chatModel)
                .chatMemory(chatMemory)
                .tools(new CodeReadTool(appId))
                .build();
    }

    private String getCacheKey(String filePath, CodeGenTypeEnum codeGenType) {
        return filePath + ":" + codeGenType.getValue();
    }

    private String getCodeCacheKey(Long appId) {
        return "CodeDialog" + ":" + appId;
    }

    private String getErrorCacheKey(String filePath, CodeGenTypeEnum codeGenType) {
        return filePath + "error" + ":" + codeGenType.getValue();
    }
}
