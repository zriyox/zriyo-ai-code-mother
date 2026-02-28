package com.zriyo.aicodemother.model.dto.plan;

import lombok.Data;

import java.util.List;

/**
 * 执行步骤
 *
 * 字段说明：
 * - order: 执行顺序（从 1 开始）
 * - action: 动作类型（copy_template/link_node_modules/generate_file）
 * - target: 目标路径或文件
 * - description: 步骤描述
 * - dependencies: 前置步骤依赖
 *
 * @author Zriyo AI
 * @since 2025-02-22
 */
@Data
public class ExecutionStepDTO {

    private Integer order;

    private String action;

    private String target;

    private String description;

    private List<String> dependencies;
}
