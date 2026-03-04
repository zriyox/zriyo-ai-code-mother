package com.zriyo.common.enums;

/**
 * 执行模式枚举（对应 agent_run_record.run_mode）
 *
 * @author Zriyo AI
 * @since 2026-03-04
 */
public enum RunMode {

    SYNC("SYNC", "同步"),
    ASYNC("ASYNC", "异步"),
    STREAM("STREAM", "流式");

    private final String code;
    private final String description;

    RunMode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static RunMode fromCode(String code) {
        if (code == null || code.isBlank()) {
            return SYNC;
        }
        String normalized = code.trim().toUpperCase();
        for (RunMode mode : values()) {
            if (mode.code.equals(normalized)) {
                return mode;
            }
        }
        return SYNC;
    }
}
