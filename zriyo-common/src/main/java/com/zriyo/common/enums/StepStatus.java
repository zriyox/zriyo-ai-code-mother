package com.zriyo.common.enums;

/**
 * 执行步骤状态枚举
 *
 * @author Zriyo AI
 * @since 2025-02-17
 */
public enum StepStatus {

    /**
     * 等待中
     */
    PENDING("pending", "等待中"),

    /**
     * 运行中
     */
    RUNNING("running", "运行中"),

    /**
     * 已完成
     */
    COMPLETED("completed", "已完成"),

    /**
     * 已失败
     */
    FAILED("failed", "已失败"),

    /**
     * 已取消
     */
    CANCELLED("cancelled", "已取消"),

    /**
     * 跳过
     */
    SKIPPED("skipped", "跳过");

    private final String code;
    private final String description;

    StepStatus(String code, String description) {
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
