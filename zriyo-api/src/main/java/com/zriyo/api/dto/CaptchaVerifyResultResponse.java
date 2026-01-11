// com.zriyo.aicodemother.model.dto.CaptchaVerifyResultResponse.java
package com.zriyo.api.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.zriyo.api.enums.CaptchaType;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * 验证码校验结果响应（校验成功/失败后的返回）
 */
@Data
@NoArgsConstructor // 👈 生成无参构造
@AllArgsConstructor // 可选
public class CaptchaVerifyResultResponse {

    /**
     * 验证码类型
     */
    private CaptchaType captchaType;

    /**
     * 本次验证的唯一 token
     */
    @NotBlank
    private String token;

    /**
     * 校验是否通过
     */
    private boolean result;

    /**
     * 是否为管理员操作（调试模式）
     */
    private boolean opAdmin;

    /**
     * 自定义反序列化构造函数，支持字符串转枚举
     */
    @JsonCreator
    public CaptchaVerifyResultResponse(
            @JsonProperty("captchaType") String captchaType,
            @JsonProperty("token") String token,
            @JsonProperty("result") Boolean result,
            @JsonProperty("opAdmin") Boolean opAdmin) {
        this.captchaType = captchaType != null ? CaptchaType.fromValue(captchaType) : null;
        this.token = token;
        this.result = result != null && result;
        this.opAdmin = opAdmin != null && opAdmin;
    }
}
