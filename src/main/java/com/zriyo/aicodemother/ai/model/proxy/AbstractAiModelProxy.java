package com.zriyo.aicodemother.ai.model.proxy;

import com.zriyo.aicodemother.ai.key.RedisApiKeyScheduler;
import com.zriyo.aicodemother.core.handler.AiContextHolder;
import com.zriyo.aicodemother.model.MonitorContext;
import com.zriyo.aicodemother.model.enums.AiCodeGenStage;
import dev.langchain4j.model.ModelProvider;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public abstract class AbstractAiModelProxy {

    protected final RedisApiKeyScheduler apiKeyScheduler;
    protected final AiCodeGenStage stage;
    protected final List<ChatModelListener> listeners;
    protected static final int MAX_RETRY = 5;

    protected AbstractAiModelProxy(RedisApiKeyScheduler apiKeyScheduler, AiCodeGenStage stage, List<ChatModelListener> listeners) {
        this.apiKeyScheduler = apiKeyScheduler;
        this.stage = stage;
        this.listeners = listeners;
    }

    /**
     * 强迫子类提供其配置的模型名称，用于监控统计
     */
    protected abstract String getModelName();

    /**
     * 核心同步逻辑：将身份信息和模型名安全地刷入 attributes
     */
    private void syncContextToAttributes(ChatRequest request, Map<Object, Object> attributes) {
        MonitorContext context = AiContextHolder.get();
        attributes.put("userId", context != null ? Objects.toString(context.getUserId(), "0") : "0");
        attributes.put("appId", context != null ? Objects.toString(context.getAppId(), "0") : "0");

        String mName = (String) attributes.get("modelName");
        if (mName == null || "null".equalsIgnoreCase(mName) || mName.isEmpty()) {
            mName = (request != null) ? request.modelName() : null;
        }
        attributes.put("modelName", (mName == null || "null".equalsIgnoreCase(mName)) ? "unknown_model" : mName);
    }
    protected Map<Object, Object> initContext(ChatRequest request) {
        Map<Object, Object> attributes = new ConcurrentHashMap<>();
        triggerOnRequest(request, attributes);
        return attributes;
    }
    protected void triggerOnRequest(ChatRequest request, Map<Object, Object> attributes) {
        if (listeners == null) return;

        attributes.putIfAbsent("modelName", Objects.toString(getModelName(), "unknown_model"));
        syncContextToAttributes(request, attributes);

        ChatModelRequestContext ctx = new ChatModelRequestContext(request, ModelProvider.OPEN_AI, attributes);
        listeners.forEach(l -> {
            try {
                l.onRequest(ctx);
            } catch (Exception e) {
                log.warn("[Monitor] onRequest 触发异常: {}", e.getMessage());
            }
        });
    }

    protected void triggerOnResponse(ChatResponse response, ChatRequest request, Map<Object, Object> attributes) {
        if (listeners == null) return;
        syncContextToAttributes(request, attributes);
        ChatModelResponseContext ctx = new ChatModelResponseContext(response, request, ModelProvider.OPEN_AI, attributes);
        listeners.forEach(l -> {
            try { l.onResponse(ctx); } catch (Exception e) { log.warn("[Monitor] onResponse 触发异常", e); }
        });
    }

    protected void triggerOnError(Throwable error, ChatRequest request, Map<Object, Object> attributes) {
        if (listeners == null) return;
        syncContextToAttributes(request, attributes);
        ChatModelErrorContext ctx = new ChatModelErrorContext(error, request, ModelProvider.OPEN_AI, attributes);
        listeners.forEach(l -> {
            try { l.onError(ctx); } catch (Exception e) { log.warn("[Monitor] onError 触发异常", e); }
        });
    }
}
