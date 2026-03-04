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
     * 进行中
     */
    IN_PROGRESS("in_progress", "进行中"),

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

    public static StepStatus fromCode(String code) {
        if (code == null || code.isBlank()) {
            return PENDING;
        }
        String normalized = code.trim().toLowerCase();
        if ("running".equals(normalized)) {
            return IN_PROGRESS;
        }
        for (StepStatus status : values()) {
            if (status.code.equals(normalized)) {
                return status;
            }
        }
        return PENDING;
    }
}
