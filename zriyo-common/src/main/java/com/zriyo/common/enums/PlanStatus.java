package com.zriyo.common.enums;

/**
 * 计划状态枚举（对应 plan.status）
 *
 * @author Zriyo AI
 * @since 2026-03-04
 */
public enum PlanStatus {

    ACTIVE("ACTIVE", "生效中"),
    ARCHIVED("ARCHIVED", "已归档");

    private final String code;
    private final String description;

    PlanStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static PlanStatus fromCode(String code) {
        if (code == null || code.isBlank()) {
            return ACTIVE;
        }
        String normalized = code.trim().toUpperCase();
        for (PlanStatus status : values()) {
            if (status.code.equals(normalized)) {
                return status;
            }
        }
        return ACTIVE;
    }
}
