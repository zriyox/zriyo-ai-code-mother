package com.zriyo.common.sse.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Agent 思考事件
 *
 * @author Zriyo AI
 * @since 2025-02-17
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgentThoughtEvent extends SseEvent {

    /**
     * Agent 运行 ID
     */
    private String agentRunId;

    /**
     * Agent 类型
     */
    private String agentType;

    /**
     * Agent 名称
     */
    private String agentName;

    /**
     * 思考内容
     */
    private String thought;

    /**
     * 思考步骤 (1, 2, 3...)
     */
    private Integer step;

    /**
     * 总步骤数
     */
    private Integer totalSteps;

    public AgentThoughtEvent(String traceId, String eventId, String agentRunId, String agentType, String thought) {
        this.traceId = traceId;
        this.eventId = eventId;
        this.eventType = "agent_thought";
        this.timestamp = Instant.now();
        this.agentRunId = agentRunId;
        this.agentType = agentType;
        this.thought = thought;
    }
}
