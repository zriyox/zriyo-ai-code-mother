package com.zriyo.common.sse.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Tool 结果事件
 *
 * @author Zriyo AI
 * @since 2025-02-17
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolResultEvent extends SseEvent {

    /**
     * Tool 运行 ID
     */
    private String toolRunId;

    /**
     * Tool 名称
     */
    private String tool;

    /**
     * 执行状态
     */
    private String status;

    /**
     * 执行结果
     */
    private Object result;

    /**
     * 错误信息
     */
    private String error;

    /**
     * 执行时长 (毫秒)
     */
    private Long duration;

    public ToolResultEvent(String traceId, String eventId, String toolRunId, String tool, String status, Object result) {
        this.traceId = traceId;
        this.eventId = eventId;
        this.eventType = "tool_result";
        this.timestamp = Instant.now();
        this.toolRunId = toolRunId;
        this.tool = tool;
        this.status = status;
        this.result = result;
    }
}
