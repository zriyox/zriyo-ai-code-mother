package com.zriyo.aicodemother.model.dto.plan;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

/**
 * LLM 配置
 *
 * 字段说明：
 * - provider: 提供商（openai/claude/gemini/deepseek/…）
 * - model: 模型名称
 * - apiKey: API Key
 * - baseUrl: 自定义 API Base URL（可选）
 * - temperature: 温度
 * - maxTokens: 最大输出 token
 * - timeout: 超时（秒）
 * - extraParams: 额外参数
 *
 * @author Zriyo AI
 * @since 2025-02-22
 */
@Data
public class LlmConfigDTO {

    private String provider;

    private String model;

    @JsonProperty("api_key")
    private String apiKey;

    @JsonProperty("base_url")
    private String baseUrl;

    private Double temperature;

    @JsonProperty("max_tokens")
    private Integer maxTokens;

    private Integer timeout;

    @JsonProperty("extra_params")
    private Map<String, Object> extraParams;
}
