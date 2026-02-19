package com.zriyo.common.sse.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 错误事件
 *
 * @author Zriyo AI
 * @since 2025-02-17
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorEvent extends SseEvent {

    /**
     * 错误代码
     */
    private String code;

    /**
     * 错误消息
     */
    private String message;

    /**
     * 错误详情
     */
    private String details;

    /**
     * 是否可恢复
     */
    private Boolean recoverable;

    /**
     * 错误类型
     */
    private String errorType;

    /**
     * 建议的重试延迟 (毫秒)
     */
    private Long retryDelay;

    public ErrorEvent(String traceId, String eventId, String code, String message, Boolean recoverable) {
        this.traceId = traceId;
        this.eventId = eventId;
        this.eventType = "error";
        this.timestamp = Instant.now();
        this.code = code;
        this.message = message;
        this.recoverable = recoverable;
    }
}
