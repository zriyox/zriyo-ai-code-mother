package com.zriyo.aicodemother.model.dto.plan;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 项目信息
 *
 * 说明：
 * - name: 项目名称
 * - type: 项目类型（如 admin/dashboard/landing/h5）
 * - description: 项目描述
 * - techStack: 技术栈说明（key/value）
 * - features: 功能点列表
 *
 * @author Zriyo AI
 * @since 2025-02-22
 */
@Data
public class ProjectInfoDTO {

    private String name;

    private String type;

    private String description;

    @JsonProperty("tech_stack")
    private Map<String, String> techStack;

    private List<String> features;
}
