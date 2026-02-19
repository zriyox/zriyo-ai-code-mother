package com.zriyo.aicodemother.python;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

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

    private final RestTemplate restTemplate;

    private String getBaseUrl() {
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
}
