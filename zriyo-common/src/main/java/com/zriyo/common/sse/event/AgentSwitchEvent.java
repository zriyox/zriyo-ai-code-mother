package com.zriyo.common.sse.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Agent 切换事件
 *
 * @author Zriyo AI
 * @since 2025-02-17
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgentSwitchEvent extends SseEvent {

    /**
     * 源 Agent 类型
     */
    private String from;

    /**
     * 目标 Agent 类型
     */
    private String to;

    /**
     * 切换原因
     */
    private String reason;

    /**
     * 切换触发类型
     */
    private String triggerType;

    public AgentSwitchEvent(String traceId, String eventId, String from, String to, String reason) {
        this.traceId = traceId;
        this.eventId = eventId;
        this.eventType = "agent_switch";
        this.timestamp = Instant.now();
        this.from = from;
        this.to = to;
        this.reason = reason;
    }
}
