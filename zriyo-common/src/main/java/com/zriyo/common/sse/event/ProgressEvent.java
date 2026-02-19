package com.zriyo.common.sse.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 进度更新事件
 *
 * @author Zriyo AI
 * @since 2025-02-17
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProgressEvent extends SseEvent {

    /**
     * 当前阶段
     */
    private String stage;

    /**
     * 进度百分比 (0.0 - 1.0)
     */
    private Double progress;

    /**
     * 进度消息
     */
    private String message;

    /**
     * 当前步骤描述
     */
    private String currentStep;

    /**
     * 总步骤数
     */
    private Integer totalSteps;

    /**
     * 当前步骤索引
     */
    private Integer currentStepIndex;

    public ProgressEvent(String traceId, String eventId, String stage, Double progress, String message) {
        this.traceId = traceId;
        this.eventId = eventId;
        this.eventType = "progress";
        this.timestamp = Instant.now();
        this.stage = stage;
        this.progress = progress;
        this.message = message;
    }
}
