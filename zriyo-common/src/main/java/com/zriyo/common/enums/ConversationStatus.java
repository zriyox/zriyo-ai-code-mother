package com.zriyo.common.enums;

/**
 * 会话状态枚举（对应 conversation_session.status）
 *
 * @author Zriyo AI
 * @since 2026-03-04
 */
public enum ConversationStatus {

    ACTIVE("ACTIVE", "活跃"),
    ARCHIVED("ARCHIVED", "归档"),
    CLOSED("CLOSED", "关闭");

    private final String code;
    private final String description;

    ConversationStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ConversationStatus fromCode(String code) {
        if (code == null || code.isBlank()) {
            return ACTIVE;
        }
        String normalized = code.trim().toUpperCase();
        for (ConversationStatus status : values()) {
            if (status.code.equals(normalized)) {
                return status;
            }
        }
        return ACTIVE;
    }
}
