package com.zriyo.common.sse.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 请求完成事件
 *
 * @author Zriyo AI
 * @since 2025-02-17
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RequestCompleteEvent extends SseEvent {

    /**
     * 请求 ID
     */
    private String requestId;

    /**
     * 执行状态
     */
    private String status;

    /**
     * Agent 执行轨迹
     */
    private String[] agentTrace;

    /**
     * 生成的文件列表
     */
    private String[] files;

    /**
     * 总执行时长 (毫秒)
     */
    private Long duration;

    /**
     * 最终响应消息
     */
    private String message;

    /**
     * Token 使用情况
     */
    private TokenUsage tokenUsage;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TokenUsage {
        private Integer promptTokens;
        private Integer completionTokens;
        private Integer totalTokens;
    }

    public RequestCompleteEvent(String traceId, String eventId, String requestId, String status) {
        this.traceId = traceId;
        this.eventId = eventId;
        this.eventType = "request_complete";
        this.timestamp = Instant.now();
        this.requestId = requestId;
        this.status = status;
    }
}
