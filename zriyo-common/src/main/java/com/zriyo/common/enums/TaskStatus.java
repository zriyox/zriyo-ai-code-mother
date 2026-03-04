package com.zriyo.common.enums;

/**
 * 任务状态枚举（对应 task.status）
 *
 * @author Zriyo AI
 * @since 2026-03-04
 */
public enum TaskStatus {

    PENDING("PENDING", "待执行"),
    RUNNING("RUNNING", "执行中"),
    RETRYING("RETRYING", "重试中"),
    SUCCESS("SUCCESS", "成功"),
    FAILED("FAILED", "失败"),
    CANCELLED("CANCELLED", "已取消"),
    TIMEOUT("TIMEOUT", "超时");

    private final String code;
    private final String description;

    TaskStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static TaskStatus fromCode(String code) {
        if (code == null || code.isBlank()) {
            return PENDING;
        }
        String normalized = code.trim().toUpperCase();
        if ("COMPLETED".equals(normalized)) {
            return SUCCESS;
        }
        for (TaskStatus status : values()) {
            if (status.code.equals(normalized)) {
                return status;
            }
        }
        return PENDING;
    }
}
