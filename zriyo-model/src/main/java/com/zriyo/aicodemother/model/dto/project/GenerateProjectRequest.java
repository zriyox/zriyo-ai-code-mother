package com.zriyo.aicodemother.model.dto.project;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 项目初始化请求（兼容原 Python /api/v1/project/generate 入参）
 *
 * @author Zriyo AI
 * @since 2026-03-04
 */
@Data
public class GenerateProjectRequest {

    @NotNull(message = "app_id 不能为空")
    @Min(value = 1, message = "app_id 必须大于 0")
    @JsonProperty("app_id")
    private Long appId;

    private String requirement;

    @JsonProperty("project_name")
    private String projectName;

    @JsonProperty("auto_generate")
    private Boolean autoGenerate;
}
