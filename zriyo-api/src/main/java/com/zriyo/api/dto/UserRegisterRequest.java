package com.zriyo.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户注册请求
 */
@Data
public class UserRegisterRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 邮箱账号 —— 使用正则进一步约束（例如：不允许 + 号、只允许字母数字 . _ -）
     * 常见严格邮箱正则（兼容主流邮箱）：
     *   ^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确") // 兜底标准校验
    @Pattern(
            regexp = "^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
            message = "邮箱格式不正确（仅支持字母、数字、点、下划线、横线）"
    )
    @Size(max = 50, message = "邮箱长度不能超过50个字符")
    private String userAccount;

    @NotBlank(message = "昵称不能为空")
    @Size(min = 2, max = 20, message = "昵称长度必须在2到20个字符之间")
    private String userName;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 20, message = "密码长度必须在8到20个字符之间")
    private String userPassword;

    @NotBlank(message = "确认密码不能为空")
    @Size(min = 8, max = 20, message = "确认密码长度必须在8到20个字符之间")
    private String checkPassword;

    @NotBlank(message = "邮箱验证码不能为空")
    @Pattern(regexp = "\\d{6}", message = "验证码必须为6位数字")
    private String emailCode;
}
