package com.zriyo.common.sse.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * SSE 事件基类
 * <p>
 * 所有 SSE 事件的基础字段
 * </p>
 *
 * @author Zriyo AI
 * @since 2025-02-17
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class SseEvent {

    /**
     * 追踪 ID - 全链路追踪
     */
    protected String traceId;

    /**
     * 事件 ID - 事件唯一标识
     */
    protected String eventId;

    /**
     * 事件类型
     */
    protected String eventType;

    /**
     * 时间戳
     */
    protected Instant timestamp;

    /**
     * 获取 SSE event 字段值
     */
    public String getSseEventName() {
        return eventType;
    }

    /**
     * 获取 SSE data 字段值 (JSON)
     */
    public String getSseData() {
        return toJson();
    }

    /**
     * 转换为 JSON (由子类实现具体序列化)
     */
    protected String toJson() {
        // 默认实现，子类可以覆盖
        return toString();
    }
}
