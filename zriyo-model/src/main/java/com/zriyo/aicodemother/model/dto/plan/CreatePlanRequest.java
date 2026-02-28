package com.zriyo.aicodemother.model.dto.plan;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 创建规划请求（对应 Python /api/v1/plan/create）
 *
 * 字段说明：
 * - requirement: 用户需求描述
 * - projectName: 项目名称
 * - appId: 应用 ID（可选）
 * - savePath: 规划文件保存路径（可选）
 * - llmConfig: LLM 配置
 *
 * @author Zriyo AI
 * @since 2025-02-22
 */
@Data
public class CreatePlanRequest {

    private String requirement;

    @JsonProperty("project_name")
    private String projectName;

    @JsonProperty("app_id")
    private Integer appId;

    @JsonProperty("save_path")
    private String savePath;

    @JsonProperty("llm_config")
    private LlmConfigDTO llmConfig;
}
