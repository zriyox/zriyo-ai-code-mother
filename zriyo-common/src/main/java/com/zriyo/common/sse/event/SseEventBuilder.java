package com.zriyo.common.sse.event;

import java.util.UUID;

/**
 * SSE 事件构建器
 * <p>
 * 简化 SSE 事件的创建，自动生成 traceId 和 eventId
 * </p>
 *
 * @author Zriyo AI
 * @since 2025-02-17
 */
public class SseEventBuilder {

    private static final ThreadLocal<String> TRACE_ID_HOLDER = new ThreadLocal<>();

    /**
     * 设置当前线程的 traceId
     */
    public static void setTraceId(String traceId) {
        TRACE_ID_HOLDER.set(traceId);
    }

    /**
     * 获取当前线程的 traceId
     */
    public static String getTraceId() {
        return TRACE_ID_HOLDER.get();
    }

    /**
     * 生成新的 traceId
     */
    public static String newTraceId() {
        return "trace_" + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 生成新的 eventId
     */
    public static String newEventId() {
        return "evt_" + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 清除当前线程的 traceId
     */
    public static void clearTraceId() {
        TRACE_ID_HOLDER.remove();
    }

    // ========== 请求生命周期 ==========

    public static RequestStartEvent requestStart(String requestId, Long userId, Long appId, String message) {
        return new RequestStartEvent(getTraceId(), newEventId(), requestId, userId, appId, message);
    }

    public static RequestCompleteEvent requestComplete(String requestId, String status) {
        return new RequestCompleteEvent(getTraceId(), newEventId(), requestId, status);
    }

    // ========== Agent 编排 ==========

    public static AgentThoughtEvent agentThought(String agentRunId, String agentType, String thought) {
        return new AgentThoughtEvent(getTraceId(), newEventId(), agentRunId, agentType, thought);
    }

    public static AgentSwitchEvent agentSwitch(String from, String to, String reason) {
        return new AgentSwitchEvent(getTraceId(), newEventId(), from, to, reason);
    }

    // ========== Tool 执行 ==========

    public static ToolCallEvent toolCall(String toolRunId, String tool, java.util.Map<String, Object> args) {
        return new ToolCallEvent(getTraceId(), newEventId(), toolRunId, tool, args);
    }

    public static ToolResultEvent toolResult(String toolRunId, String tool, String status, Object result) {
        return new ToolResultEvent(getTraceId(), newEventId(), toolRunId, tool, status, result);
    }

    // ========== 进度更新 ==========

    public static ProgressEvent progress(String stage, double progress, String message) {
        return new ProgressEvent(getTraceId(), newEventId(), stage, progress, message);
    }

    // ========== 流式输出 ==========

    public static TextChunkEvent textChunk(String content) {
        return new TextChunkEvent(getTraceId(), newEventId(), content);
    }

    // ========== 错误 ==========

    public static ErrorEvent error(String code, String message, boolean recoverable) {
        return new ErrorEvent(getTraceId(), newEventId(), code, message, recoverable);
    }

    // ========== 文件事件 ==========

    public static FileEvent fileCreated(String path) {
        return new FileEvent(getTraceId(), newEventId(), path, "created");
    }

    public static FileEvent fileUpdated(String path) {
        return new FileEvent(getTraceId(), newEventId(), path, "updated");
    }
}
