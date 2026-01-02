package com.zriyo.aicodemother.model.enums;

import java.util.Arrays;

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

    private final String key;       // 用于 Redis 存储
    private final String description; // 中文描述

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

    /**
     * 根据 key 获取对应的枚举，找不到返回 null
     */
    public static EmailCaptchaType fromKey(String key) {
        if (key == null) return null;
        return Arrays.stream(EmailCaptchaType.values())
                .filter(type -> type.getKey().equalsIgnoreCase(key))
                .findFirst()
                .orElse(null);
    }
}
