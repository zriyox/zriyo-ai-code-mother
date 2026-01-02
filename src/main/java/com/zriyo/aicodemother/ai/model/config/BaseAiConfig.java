package com.zriyo.aicodemother.ai.model.config;

import lombok.Data;

import java.time.Duration;
import java.util.List;

@Data
public abstract class BaseAiConfig {
    protected List<String> apiKeys;
    protected String baseUrl;
    protected String modelName;
    protected Integer maxTokens;
    protected Double temperature;
    protected String timeout = "60s";
    protected Boolean logRequests = false;
    protected Boolean logResponses = false;

    public Duration parseDuration() {
        if (timeout == null || timeout.isEmpty()) return Duration.ofSeconds(60);
        String unit = timeout.substring(timeout.length() - 1);
        long value = Long.parseLong(timeout.replaceAll("[^0-9]", ""));
        return switch (unit) {
            case "m" -> Duration.ofMinutes(value);
            case "h" -> Duration.ofHours(value);
            default -> Duration.ofSeconds(value);
        };
    }
}
