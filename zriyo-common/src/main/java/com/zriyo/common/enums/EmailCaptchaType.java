package com.zriyo.common.enums;

import java.util.Arrays;

/**
 * 邮箱验证码类型枚举
 *
 * @author Zriyo AI
 * @since 2025-02-17
 */
public enum EmailCaptchaType {
    /**
     * 绑定邮箱
     */
    BIND_EMAIL("BIND_EMAIL", "绑定邮箱"),

    /**
     * 重置账户密码
     */
    RESET_PASSWORD("RESET_PASSWORD", "重置账户密码"),

    /**
     * 注册
     */
    REGISTER("REGISTER", "欢迎注册 ZriyoCode 平台"),

    /**
     * 登录校验
     */
    LOGIN_VERIFY("LOGIN_VERIFY", "欢迎回来");

    private final String key;
    private final String description;

    EmailCaptchaType(String key, String description) {
        this.key = key;
        this.description = description;
    }

    public String getKey() {
        return key;
    }

    public String getDescription() {
        return description;
    }

    public static EmailCaptchaType fromKey(String key) {
        if (key == null) return null;
        return Arrays.stream(EmailCaptchaType.values())
                .filter(type -> type.getKey().equalsIgnoreCase(key))
                .findFirst()
                .orElse(null);
    }
}
