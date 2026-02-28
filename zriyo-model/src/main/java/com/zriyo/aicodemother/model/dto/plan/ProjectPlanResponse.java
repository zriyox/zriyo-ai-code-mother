package com.zriyo.aicodemother.model.dto.plan;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 项目规划响应（对应 Python ProjectPlan）
 *
 * 包含：项目信息、文件规划、执行顺序、人类可读步骤与复杂度评估。
 *
 * @author Zriyo AI
 * @since 2025-02-22
 */
@Data
public class ProjectPlanResponse {

    /**
     * 规划 ID（如 plan_20250220_001）
     */
    @JsonProperty("plan_id")
    private String planId;

    /**
     * 创建时间（ISO 字符串）
     */
    @JsonProperty("created_at")
    private String createdAt;

    /**
     * 项目信息
     */
    private ProjectInfoDTO project;

    /**
     * 文件规划列表（包含 stepId）
     */
    private List<PlanFileInfo> files;

    /**
     * 执行顺序（含动作类型）
     */
    @JsonProperty("execution_order")
    private List<ExecutionStepDTO> executionOrder;

    /**
     * 人类可读计划步骤
     */
    @JsonProperty("human_plan")
    private List<HumanPlanStepDTO> humanPlan;

    /**
     * 预估文件数量
     */
    @JsonProperty("estimated_files")
    private Integer estimatedFiles;

    /**
     * 预估复杂度（simple/medium/complex）
     */
    @JsonProperty("estimated_complexity")
    private String estimatedComplexity;
}
