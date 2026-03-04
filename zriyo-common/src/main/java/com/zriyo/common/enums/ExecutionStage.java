package com.zriyo.common.enums;

/**
 * 执行阶段枚举
 *
 * @author Zriyo AI
 * @since 2025-02-17
 */
public enum ExecutionStage {

    /**
     * 准备阶段
     */
    PREPARE("prepare", "准备阶段"),

    /**
     * 初始化脚手架
     */
    INIT_SCAFFOLD("init_scaffold", "初始化脚手架"),

    /**
     * 规划
     */
    PLAN("plan", "规划"),

    /**
     * 代码生成
     */
    CODEGEN("codegen", "代码生成"),

    /**
     * 检查
     */
    CHECK("check", "检查"),

    /**
     * 预览
     */
    PREVIEW("preview", "预览"),

    /**
     * 修复
     */
    REPAIR("repair", "修复"),

    /**
     * 收尾
     */
    FINALIZE("finalize", "收尾"),

    /**
     * 完成
     */
    COMPLETED("completed", "完成"),

    /**
     * 失败
     */
    FAILED("failed", "失败"),

    /**
     * 已取消
     */
    CANCELLED("cancelled", "已取消");

    private final String code;
    private final String description;

    ExecutionStage(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ExecutionStage fromCode(String code) {
        if (code == null || code.isBlank()) {
            return PREPARE;
        }
        String normalized = code.trim().toLowerCase();
        // 兼容旧值
        if ("initializing".equals(normalized)) {
            return PREPARE;
        }
        if ("planning".equals(normalized) || "intent_recognition".equals(normalized)) {
            return PLAN;
        }
        if ("executing".equals(normalized)) {
            return CODEGEN;
        }
        if ("validating".equals(normalized)) {
            return CHECK;
        }
        for (ExecutionStage stage : values()) {
            if (stage.code.equals(normalized)) {
                return stage;
            }
        }
        return PREPARE;
    }
}
