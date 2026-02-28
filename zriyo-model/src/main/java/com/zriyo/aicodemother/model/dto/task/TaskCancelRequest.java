package com.zriyo.aicodemother.model.dto.task;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 任务取消请求（对应 Python /api/internal/task/cancel）
 *
 * @author Zriyo AI
 * @since 2026-02-25
 */
@Data
public class TaskCancelRequest {

    @JsonProperty("task_id")
    private String taskId;
}
