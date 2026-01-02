package com.zriyo.aicodemother.ai.service;

import com.zriyo.aicodemother.core.handler.AiContextHolder;
import com.zriyo.aicodemother.core.handler.AiModelMetricsCollector;
import com.zriyo.aicodemother.model.MonitorContext;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.output.TokenUsage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * AI 模型监控监听器
 * 修复点：采用“属性优先”策略，解决异步工具调用导致的 TTL 丢失及 NPE 问题
 */
@Component
@Slf4j
public class AiModelMonitorListener implements ChatModelListener {

    private static final String REQUEST_START_TIME_KEY = "request_start_time";
    private static final String DEFAULT_ID = "0";

    @Resource
    private AiModelMetricsCollector aiModelMetricsCollector;

    @Override
    public void onRequest(ChatModelRequestContext requestContext) {
        String userId = getSafeAttribute(requestContext.attributes(), "userId");
        String appId = getSafeAttribute(requestContext.attributes(), "appId");

        // 2. 存入开始时间
        requestContext.attributes().put(REQUEST_START_TIME_KEY, Instant.now());

        String modelName = requestContext.chatRequest().modelName();
        if (modelName == null){
            modelName = requestContext.attributes().get("modelName").toString();
        }
        // 3. 记录指标（这里的参数已确保非 null）
        aiModelMetricsCollector.recordRequest(userId, appId, modelName, "started");
    }

    @Override
    public void onResponse(ChatModelResponseContext responseContext) {
        String userId = getSafeAttribute(responseContext.attributes(), "userId");
        String appId = getSafeAttribute(responseContext.attributes(), "appId");

        if (responseContext.chatResponse() == null) {
            return;
        }

        String modelName = responseContext.chatRequest().modelName();
        if (modelName == null){
            modelName = responseContext.attributes().get("modelName").toString();
        }
        aiModelMetricsCollector.recordRequest(userId, appId, modelName, "success");
        recordResponseTime(responseContext.attributes(), userId, appId, modelName);
        recordTokenUsage(responseContext, userId, appId, modelName);
    }

    @Override
    public void onError(ChatModelErrorContext errorContext) {
        String userId = getSafeAttribute(errorContext.attributes(), "userId");
        String appId = getSafeAttribute(errorContext.attributes(), "appId");

        String modelName = errorContext.chatRequest().modelName();
        if (modelName == null){
            modelName = errorContext.attributes().get("modelName").toString();
        }        String errorMessage = errorContext.error() != null ? errorContext.error().getMessage() : "Unknown Error";

        aiModelMetricsCollector.recordRequest(userId, appId, modelName, "error");
        aiModelMetricsCollector.recordError(userId, appId, modelName, errorMessage);
        recordResponseTime(errorContext.attributes(), userId, appId, modelName);
    }

    /**
     * 安全提取属性的防御性逻辑
     * 解决工具调用第二次回传时 TTL 失效的问题
     */
    private String getSafeAttribute(Map<Object, Object> attributes, String key) {

        Object val = attributes.get(key);
        if (val != null) {
            return val.toString();
        }

        // 2. 兜底：尝试从 TTL 获取（通常仅在第一次 Request 发起时有效）
        MonitorContext context = AiContextHolder.get();
        if (context != null) {
            String ttlVal = "userId".equals(key) ? context.getUserId() : context.getAppId();
            if (ttlVal != null) {
                // 顺手补回 attributes，方便该请求后续流程使用
                attributes.put(key, ttlVal);
                return ttlVal;
            }
        }

        return DEFAULT_ID;
    }

    /**
     * 记录响应耗时
     */
    private void recordResponseTime(Map<Object, Object> attributes, String userId, String appId, String modelName) {
        Instant startTime = (Instant) attributes.get(REQUEST_START_TIME_KEY);
        if (startTime != null) {
            Duration responseTime = Duration.between(startTime, Instant.now());
            aiModelMetricsCollector.recordResponseTime(userId, appId, modelName, responseTime);
        }
    }

    /**
     * 记录 Token 消耗
     */
    private void recordTokenUsage(ChatModelResponseContext responseContext, String userId, String appId, String modelName) {
        if (responseContext.chatResponse().metadata() == null) return;

        TokenUsage tokenUsage = responseContext.chatResponse().metadata().tokenUsage();
        if (tokenUsage != null) {
            aiModelMetricsCollector.recordTokenUsage(userId, appId, modelName, "input",
                    Objects.requireNonNullElse(tokenUsage.inputTokenCount(), 0));
            aiModelMetricsCollector.recordTokenUsage(userId, appId, modelName, "output",
                    Objects.requireNonNullElse(tokenUsage.outputTokenCount(), 0));
            aiModelMetricsCollector.recordTokenUsage(userId, appId, modelName, "total",
                    Objects.requireNonNullElse(tokenUsage.totalTokenCount(), 0));
        }
    }
}
