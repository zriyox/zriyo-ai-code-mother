package com.zriyo.aicodemother.model.dto.plan;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 规划文件信息
 *
 * 字段说明：
 * - path: 文件路径（相对项目根目录）
 * - type: 文件类型（typescript/vue-component/…）
 * - description: 文件用途说明
 * - stepId: 对应 human_plan.step_id（用于双向映射）
 * - dependencies: 依赖的其他文件路径
 * - template: 模板名称（可选）
 * - contentHint: 内容提示（可选）
 * - metadata: 额外元数据
 *
 * @author Zriyo AI
 * @since 2025-02-22
 */
@Data
public class PlanFileInfo {

    private String path;

    private String type;

    private String description;

    @JsonProperty("step_id")
    private String stepId;

    private List<String> dependencies;

    private String template;

    @JsonProperty("content_hint")
    private String contentHint;

    private Map<String, Object> metadata;
}
