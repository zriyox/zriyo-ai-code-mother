package com.zriyo.common.enums;

import lombok.Getter;

/**
 * 验证码类型枚举
 *
 * @author Zriyo AI
 * @since 2025-02-17
 */
@Getter
public enum CaptchaType {

    /**
     * 滑块拼图验证码
     */
    blockPuzzle("blockPuzzle"),

    /**
     * 点选文字验证码
     */
    clickWord("clickWord");

    private final String value;

    CaptchaType(String value) {
        this.value = value;
    }

    public static CaptchaType fromValue(String value) {
        for (CaptchaType type : CaptchaType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("不支持的验证码类型: " + value);
    }
}
