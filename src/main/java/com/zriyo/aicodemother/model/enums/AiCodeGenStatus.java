package com.zriyo.aicodemother.model.enums;

/**
 * AI 代码生成任务的状态
 */
public enum AiCodeGenStatus {
    RUNNING("RUNNING", "运行中"),
    SUCCESS("SUCCESS", "成功"),
    FAILED("FAILED", "失败"),
    CANCELLED("CANCELLED", "已取消");

    private final String value;
    private final String description;

    AiCodeGenStatus(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    // 可选：根据字符串值反查枚举（用于数据库映射）
    public static AiCodeGenStatus fromValue(String value) {
        for (AiCodeGenStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown status: " + value);
    }
}
