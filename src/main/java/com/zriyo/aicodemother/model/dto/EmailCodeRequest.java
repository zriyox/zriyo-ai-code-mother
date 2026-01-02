package com.zriyo.aicodemother.model.dto;

import com.zriyo.aicodemother.model.enums.EmailCaptchaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;

@Data
public class EmailCodeRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 邮箱账号
     */
    @NotBlank(message = "邮箱不能为空")
    @Pattern(
            regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
            message = "邮箱格式不正确"
    )
    private String email;
    /**
     * 行为验证码
     */
    @NotBlank(message = "行为验证码不能为空")
    private String captchaVerification;


    /**
     * 验证码类型
     */
    @NotNull(message = "验证码类型不能为空")
    private EmailCaptchaType captchaType;
}
