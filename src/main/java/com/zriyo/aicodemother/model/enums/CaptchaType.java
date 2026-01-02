package com.zriyo.aicodemother.model.enums;

import lombok.Getter;

/**
 * 验证码类型枚举
 */
@Getter
public enum CaptchaType {

    /**
     * 滑块拼图验证码
     */
    blockPuzzle("blockPuzzle"),

    /**
     * 点选文字验证码（可扩展）
     */
    clickWord("clickWord");

    private final String value;

    CaptchaType(String value) {
        this.value = value;
    }

    /**
     * 根据字符串值查找对应的枚举（用于反序列化）
     */
    public static CaptchaType fromValue(String value) {
        for (CaptchaType type : CaptchaType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("不支持的验证码类型: " + value);
    }
}
