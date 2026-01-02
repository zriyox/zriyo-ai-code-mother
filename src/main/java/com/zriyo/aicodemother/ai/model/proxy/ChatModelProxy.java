package com.zriyo.aicodemother.ai.model.proxy;

import com.zriyo.aicodemother.ai.model.config.ChatModelConfig;
import com.zriyo.aicodemother.model.enums.AiCodeGenStage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class ChatModelProxy extends AbstractAiModelProxy implements ChatModel {

    private final ChatModelConfig config;
    private final AiModelExecutor executor;

    public ChatModelProxy(AiModelExecutor executor, ChatModelConfig config,
                          AiCodeGenStage stage, List<ChatModelListener> listeners) {
        super(executor.getApiKeyScheduler(), stage, listeners);
        this.executor = executor;
        this.config = config;
    }

    @Override
    public ChatResponse chat(ChatRequest chatRequest) {
        Map<Object, Object> attributes = initContext(chatRequest);

        try {
            AtomicReference<ChatResponse> result = new AtomicReference<>();
            executor.executeWithRetry(stage, MAX_RETRY, (apiKey) -> {
                ChatModel delegate = OpenAiChatModel.builder()
                        .apiKey(apiKey)
                        .baseUrl(config.getBaseUrl())
                        .modelName(config.getModelName())
                        .timeout(config.parseDuration())
                        .build();
                result.set(delegate.chat(chatRequest));
                return true;
            });
            ChatResponse response = result.get();
            triggerOnResponse(response, chatRequest, attributes);
            return response;
        } catch (Exception e) {
            triggerOnError(e, chatRequest, attributes);
            throw e;
        }
    }

    @Override public ChatResponse doChat(ChatRequest chatRequest) { return chat(chatRequest); }
    @Override public List<ChatModelListener> listeners() { return listeners; }

    @Override
    protected String getModelName() {
        return config.getModelName();
    }
}
