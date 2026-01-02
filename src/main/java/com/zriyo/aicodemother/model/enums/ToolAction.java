package com.zriyo.aicodemother.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 工具执行动作类型
 */
public enum ToolAction {


    /**
     * 写入/覆盖文件内容
     */
    WRITE("write", "写入文件"),

    /**
     * 用户手动终止
     */
    STOP("stop","手动终止"),
    /**
     * 修复 bug
     */
    FIX_BUG("fix_bug", "修复bug");



    private final String value;
    private final String text;

    // 主构造函数：必须提供 value，text 可选（但这里我们统一提供）
    ToolAction(String value, String text) {
        this.value = value;
        this.text = text;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    /**
     * 获取中文描述（可用于日志、UI 展示）
     */
    public String getText() {
        return text;
    }

    /**
     * 根据字符串值（如 "write"）反序列化为枚举
     *
     * @param value 枚举的 value 字符串（大小写不敏感）
     * @return 对应的 ToolAction
     * @throws IllegalArgumentException 如果 value 不支持
     */
    public static ToolAction fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Tool action value must not be null or empty");
        }
        for (ToolAction action : values()) {
            if (action.value.equalsIgnoreCase(value.trim())) {
                return action;
            }
        }
        throw new IllegalArgumentException("Unsupported tool action: '" + value + "'");
    }
}
