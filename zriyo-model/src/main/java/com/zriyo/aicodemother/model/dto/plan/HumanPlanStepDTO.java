package com.zriyo.aicodemother.model.dto.plan;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 人类可读计划步骤
 *
 * 字段说明：
 * - stepId: 步骤 ID（如 S1/S2）
 * - title: 步骤标题
 * - description: 步骤描述
 * - files: 该步骤关联的文件列表
 *
 * @author Zriyo AI
 * @since 2025-02-22
 */
@Data
public class HumanPlanStepDTO {

    @JsonProperty("step_id")
    private String stepId;

    private String title;

    private String description;

    private List<String> files;
}
