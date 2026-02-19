package com.zriyo.common.sse.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 请求开始事件
 *
 * @author Zriyo AI
 * @since 2025-02-17
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RequestStartEvent extends SseEvent {

    /**
     * 请求 ID
     */
    private String requestId;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 应用 ID
     */
    private Long appId;

    /**
     * 用户消息
     */
    private String message;

    /**
     * 意图类型
     */
    private String intentType;

    public RequestStartEvent(String traceId, String eventId, String requestId, Long userId, Long appId, String message) {
        this.traceId = traceId;
        this.eventId = eventId;
        this.eventType = "request_start";
        this.timestamp = Instant.now();
        this.requestId = requestId;
        this.userId = userId;
        this.appId = appId;
        this.message = message;
    }
}
