package com.zriyo.aicodemother.controller;

import com.zriyo.aicodemother.model.dto.task.TaskCancelRequest;
import com.zriyo.aicodemother.model.dto.task.TaskCancelResponse;
import com.zriyo.aicodemother.python.PythonServiceClient;
import com.zriyo.common.result.Result;
import com.zriyo.common.result.ResultUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 任务控制器
 *
 * @author Zriyo AI
 * @since 2026-02-25
 */
@RestController
@RequestMapping("/api/v1/task")
@RequiredArgsConstructor
public class TaskController {

    private final PythonServiceClient pythonServiceClient;

    @PostMapping("/{taskId}/cancel")
    public Result<TaskCancelResponse> cancelTask(@PathVariable String taskId) {
        TaskCancelRequest request = new TaskCancelRequest();
        request.setTaskId(taskId);
        TaskCancelResponse response = pythonServiceClient.cancelTask(request);
        return ResultUtils.success(response);
    }
}
