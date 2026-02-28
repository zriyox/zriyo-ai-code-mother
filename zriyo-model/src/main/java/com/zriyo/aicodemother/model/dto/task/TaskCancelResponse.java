package com.zriyo.aicodemother.model.dto.task;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 任务取消响应
 *
 * @author Zriyo AI
 * @since 2026-02-25
 */
@Data
public class TaskCancelResponse {

    @JsonProperty("task_id")
    private String taskId;

    private Boolean cancelled;
}
