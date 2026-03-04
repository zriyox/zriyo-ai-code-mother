package com.zriyo.common.enums;

/**
 * 工具执行状态枚举（对应 agent_tool_run.status）
 *
 * @author Zriyo AI
 * @since 2026-03-04
 */
public enum ToolRunStatus {

    RUNNING("RUNNING", "执行中"),
    SUCCESS("SUCCESS", "成功"),
    FAILED("FAILED", "失败"),
    CANCELLED("CANCELLED", "取消");

    private final String code;
    private final String description;

    ToolRunStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ToolRunStatus fromCode(String code) {
        if (code == null || code.isBlank()) {
            return RUNNING;
        }
        String normalized = code.trim().toUpperCase();
        for (ToolRunStatus status : values()) {
            if (status.code.equals(normalized)) {
                return status;
            }
        }
        return RUNNING;
    }
}
