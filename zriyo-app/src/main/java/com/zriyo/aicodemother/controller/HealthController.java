package com.zriyo.aicodemother.controller;

import com.zriyo.aicodemother.python.PythonProcessStarter;
import com.zriyo.aicodemother.python.PythonServiceClient;
import com.zriyo.common.result.Result;
import com.zriyo.common.result.ResultUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 健康检查控制器
 *
 * @author Zriyo AI
 * @since 2025-02-17
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HealthController {

    private final PythonProcessStarter pythonProcessStarter;
    private final PythonServiceClient pythonServiceClient;

    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        return ResultUtils.success(Map.of(
            "status", "UP",
            "java", "running",
            "python", pythonProcessStarter.isPythonProcessAlive() ? "running" : "stopped",
            "pythonPort", pythonProcessStarter.getPythonPort()
        ));
    }

    @GetMapping("/test/python")
    public Result<Map<String, Object>> testPython() {
        boolean ping = pythonServiceClient.ping();
        Map<String, Object> health = pythonServiceClient.getHealth();

        return ResultUtils.success(Map.of("ping", ping, "health", health));
    }

    @GetMapping("/test/token-count")
    public Result<Map<String, Object>> testTokenCount() {
        String text = "Hello, this is a test message for token counting.";
        int count = pythonServiceClient.countTokens("openai", text);
        return ResultUtils.success(Map.of("text", text, "tokenCount", count));
    }
}
