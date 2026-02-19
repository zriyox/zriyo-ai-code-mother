package com.zriyo.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户登录请求
 *
 * @author Zriyo AI
 * @since 2025-02-17
 */
@Data
public class UserLoginRequest {

    /**
     * 用户名或邮箱
     */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;

    /**
     * 验证码（可选）
     */
    private String captcha;

    /**
     * 记住我
     */
    private Boolean rememberMe = false;
}
