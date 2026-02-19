package com.zriyo.common.enums;

/**
 * 执行阶段枚举
 *
 * @author Zriyo AI
 * @since 2025-02-17
 */
public enum ExecutionStage {

    /**
     * 初始化
     */
    INITIALIZING("initializing", "初始化"),

    /**
     * 意图识别
     */
    INTENT_RECOGNITION("intent_recognition", "意图识别"),

    /**
     * 规划中
     */
    PLANNING("planning", "规划中"),

    /**
     * 执行中
     */
    EXECUTING("executing", "执行中"),

    /**
     * 验证中
     */
    VALIDATING("validating", "验证中"),

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
}
