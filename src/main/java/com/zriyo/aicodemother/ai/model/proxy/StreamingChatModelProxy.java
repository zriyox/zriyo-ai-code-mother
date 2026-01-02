package com.zriyo.aicodemother.ai.model.proxy;

import com.zriyo.aicodemother.ai.key.RedisApiKeyScheduler;
import com.zriyo.aicodemother.ai.model.config.StreamingChatModelConfig;
import com.zriyo.aicodemother.core.handler.AiContextHolder;
import com.zriyo.aicodemother.model.MonitorContext;
import com.zriyo.aicodemother.model.enums.AiCodeGenStage;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class StreamingChatModelProxy extends AbstractAiModelProxy implements StreamingChatModel {

    private final StreamingChatModelConfig config;
    private static final int MAX_RETRY = 5;

    public StreamingChatModelProxy(RedisApiKeyScheduler apiKeyScheduler, StreamingChatModelConfig config,
                                   AiCodeGenStage stage, List<ChatModelListener> listeners) {
        super(apiKeyScheduler, stage, listeners);
        this.config = config;
    }

    @Override
    public void chat(ChatRequest chatRequest, StreamingChatResponseHandler handler) {
        MonitorContext monitor = AiContextHolder.get();
        if (monitor != null) {
            int steps = monitor.incrementAndGetStep();
            if (steps > 5) {
                log.error("[CircuitBreaker] 交互深度过载 (Step: {}), 正在执行柔性终止...", steps);
                handler.onCompleteResponse(ChatResponse.builder()
                        .aiMessage(AiMessage.from("Task exceeded depth limit, gracefully terminated."))
                        .build());
                return;
            }
        }

        Map<Object, Object> attributes = initContext(chatRequest);
        attemptChatAsync(chatRequest, handler, 0, attributes);
    }

    private void attemptChatAsync(ChatRequest chatRequest, StreamingChatResponseHandler handler,
                                  int attemptCount, Map<Object, Object> attributes) {

        if (attemptCount >= MAX_RETRY) {
            Throwable lastError = (Throwable) attributes.get("LAST_ERROR");
            log.error("API 重试耗尽，执行柔性结束。最后异常: {}", lastError != null ? lastError.getMessage() : "未知");

            handler.onPartialResponse("\n[系统：AI 服务响应异常，已尽力恢复当前代码状态。] ");
            handler.onCompleteResponse(ChatResponse.builder()
                    .aiMessage(AiMessage.from("Retries exhausted, emergency stop."))
                    .build());
            return;
        }

        if (attemptCount > 0) {
            long waitMillis = (long) Math.pow(2, attemptCount - 1) * 1000;
            log.warn("准备进行第 {} 次退避重试，等待 {} ms...", attemptCount, waitMillis);
            try {
                Thread.sleep(waitMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                handler.onCompleteResponse(ChatResponse.builder().aiMessage(AiMessage.from("Interrupted")).build());
                return;
            }
        }

        String apiKey;
        try {
            apiKey = apiKeyScheduler.acquire(stage);
        } catch (Exception e) {
            log.error("API Key 调度失败: {}", e.getMessage());
            handler.onCompleteResponse(ChatResponse.builder().aiMessage(AiMessage.from("No Key available")).build());
            return;
        }

        AtomicBoolean hasDataProduced = new AtomicBoolean(false);

        StreamingChatModel delegate = OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(config.getBaseUrl())
                .modelName(config.getModelName())
                .timeout(config.parseDuration())
                .build();

        log.info("发起流式请求 [Attempt: {}], Model: {}, Key: {}", attemptCount, config.getModelName(), apiKey.substring(0, 8) + "...");

        delegate.chat(chatRequest, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String s) {
                hasDataProduced.set(true);
                handler.onPartialResponse(s);
            }

            @Override
            public void onPartialToolExecutionRequest(int index, ToolExecutionRequest partialToolExecutionRequest) {
                hasDataProduced.set(true);
                handler.onPartialToolExecutionRequest(index, partialToolExecutionRequest);
            }

            @Override
            public void onCompleteToolExecutionRequest(int index, ToolExecutionRequest completeToolExecutionRequest) {
                handler.onCompleteToolExecutionRequest(index, completeToolExecutionRequest);
            }

            @Override
            public void onCompleteResponse(ChatResponse response) {

                apiKeyScheduler.reportSuccess(apiKey);

                apiKeyScheduler.release(stage, apiKey);
                triggerOnResponse(response, chatRequest, attributes);
                handler.onCompleteResponse(response);
            }

            @Override
            public void onError(Throwable error) {
                log.error("API 调用异常 [Key: {}]: {}", apiKey, error.getMessage());
                attributes.put("LAST_ERROR", error);

                apiKeyScheduler.reportFailure(apiKey, stage);
                apiKeyScheduler.release(stage, apiKey);

                if (!hasDataProduced.get()) {
                    // 连接期异常：内部指数退避重试
                    attemptChatAsync(chatRequest, handler, attemptCount + 1, attributes);
                } else {
                    // 传输中异常：柔性结束，保护后续 Handler
                    log.warn("流传输中断，执行柔性收尾。原因: {}", error.getMessage());
                    handler.onPartialResponse("\n[传输中断，正在保存已生成的代码内容...] ");
                    handler.onCompleteResponse(ChatResponse.builder()
                            .aiMessage(AiMessage.from("Connection lost, gracefully terminated."))
                            .build());
                }
            }
        });
    }

    @Override
    public List<ChatModelListener> listeners() {
        return listeners;
    }

    @Override
    protected String getModelName() {
        return config.getModelName();
    }
}
