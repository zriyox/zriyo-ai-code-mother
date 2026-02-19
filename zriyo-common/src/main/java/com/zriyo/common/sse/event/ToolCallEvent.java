package com.zriyo.common.sse.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Tool 调用事件
 *
 * @author Zriyo AI
 * @since 2025-02-17
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolCallEvent extends SseEvent {

    /**
     * Tool 运行 ID
     */
    private String toolRunId;

    /**
     * Tool 名称
     */
    private String tool;

    /**
     * Tool 显示名称
     */
    private String toolName;

    /**
     * Tool 参数
     */
    private Map<String, Object> args;

    /**
     * 是否为远程 Tool (Python)
     */
    private Boolean remote;

    public ToolCallEvent(String traceId, String eventId, String toolRunId, String tool, Map<String, Object> args) {
        this.traceId = traceId;
        this.eventId = eventId;
        this.eventType = "tool_call";
        this.timestamp = Instant.now();
        this.toolRunId = toolRunId;
        this.tool = tool;
        this.args = args;
    }
}
