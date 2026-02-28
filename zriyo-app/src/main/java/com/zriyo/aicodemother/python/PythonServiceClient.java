package com.zriyo.aicodemother.python;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.HttpStatusCodeException;

import java.util.List;
import java.util.Map;

import com.zriyo.common.exception.BusinessException;
import com.zriyo.common.result.ErrorCode;
import com.zriyo.aicodemother.model.dto.plan.CreatePlanRequest;
import com.zriyo.aicodemother.model.dto.plan.ProjectPlanResponse;
import com.zriyo.aicodemother.model.dto.task.TaskCancelRequest;
import com.zriyo.aicodemother.model.dto.task.TaskCancelResponse;

/**
 * Python 服务客户端
 * 用于调用 Python 侧的内部 API
 *
 * @author Zriyo AI
 * @since 2025-02-17
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PythonServiceClient {

    @Value("${python.port:8000}")
    private int pythonPort;

    @Value("${python.base-url:}")
    private String pythonBaseUrl;

    private final RestTemplate restTemplate;

    private String getBaseUrl() {
        if (StringUtils.hasText(pythonBaseUrl)) {
            return pythonBaseUrl;
        }
        return "http://127.0.0.1:" + pythonPort;
    }

    /**
     * Ping Python 服务
     */
    public boolean ping() {
        try {
            Map result = restTemplate.getForObject(getBaseUrl() + "/api/internal/ping", Map.class);
            return result != null && Boolean.TRUE.equals(result.get("pong"));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 计算文本的 token 数量
     */
    public int countTokens(String provider, String text) {
        try {
            Map<String, Object> request = Map.of("provider", provider, "text", text);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                getBaseUrl() + "/api/internal/count-tokens",
                entity,
                Map.class
            );

            if (response != null) {
                return (Integer) response.get("token_count");
            }
            return -1;
        } catch (Exception e) {
            log.error("调用 Python token 计数失败: {}", e.getMessage());
            return -1;
        }
    }

    /**
     * 获取 Python 健康状态
     */
    public Map<String, Object> getHealth() {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = restTemplate.getForObject(
                getBaseUrl() + "/health",
                Map.class
            );
            return result;
        } catch (Exception e) {
            return Map.of("status", "unreachable");
        }
    }

    /**
     * 创建规划（Plan）
     */
    public ProjectPlanResponse createPlan(CreatePlanRequest request) {
        return postForObject("/api/v1/plan/create", request, new ParameterizedTypeReference<>() {});
    }

    /**
     * 取消任务（内部）
     */
    public TaskCancelResponse cancelTask(TaskCancelRequest request) {
        return postForObject("/api/internal/task/cancel", request, new ParameterizedTypeReference<>() {});
    }

    private <T> T postForObject(
        String path,
        Object body,
        ParameterizedTypeReference<T> typeRef
    ) {
        String url = getBaseUrl() + path;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Object> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<T> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                typeRef
            );
            return response.getBody();
        } catch (ResourceAccessException e) {
            log.error("Python 服务不可用: {}", e.getMessage());
            throw new BusinessException(ErrorCode.PYTHON_SERVICE_UNAVAILABLE, e.getMessage());
        } catch (HttpStatusCodeException e) {
            log.error("Python 服务错误: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.PYTHON_SERVICE_ERROR, e.getResponseBodyAsString());
        } catch (RestClientException e) {
            log.error("调用 Python 服务失败: {}", e.getMessage());
            throw new BusinessException(ErrorCode.PYTHON_SERVICE_ERROR, e.getMessage());
        }
    }
}
