package com.zriyo.aicodemother.model.dto.project;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 项目初始化响应（兼容原 Python /api/v1/project/generate 出参）
 *
 * @author Zriyo AI
 * @since 2026-03-04
 */
@Data
public class GenerateProjectResponse {

    private boolean success;

    @JsonProperty("project_path")
    private String projectPath;
}
