package com.zriyo.aicodemother.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ForgetPasswordRequest {

    /**
     * 邮箱 —— 用于接收验证码并定位账户
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Pattern(
            regexp = "^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
            message = "邮箱格式不正确（仅支持字母、数字、点、下划线、横线）"
    )
    @Size(max = 50, message = "邮箱长度不能超过50个字符")
    private String email;

    /**
     * 验证码 —— 6位数字，由邮件发送
     */
    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "\\d{6}", message = "验证码必须为6位数字")
    private String code;

    /**
     * 新密码 —— 需满足安全策略
     */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 20, message = "新密码长度必须在8到20个字符之间")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*#?&]{8,20}$",
            message = "新密码必须包含字母和数字"
    )
    private String newPassword;

    /**
     * 确认密码 —— 必须与新密码一致
     */
    @NotBlank(message = "确认密码不能为空")
    @Size(min = 8, max = 20, message = "确认密码长度必须在8到20个字符之间")
    private String confirmPassword;
}
