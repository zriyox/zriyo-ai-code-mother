// com.zriyo.aicodemother.model.dto.CaptchaVerifyRequest.java
package com.zriyo.aicodemother.model.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.zriyo.aicodemother.model.enums.CaptchaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


/**
 * 验证码校验请求参数（前端提交）
 */
@Data
public class CaptchaVerifyRequest {

    /**
     * 验证码类型，如 blockPuzzle
     */
    @NotNull(message = "captchaType 不能为空")
    private CaptchaType captchaType;

    /**
     * AES 加密后的坐标信息（Base64 字符串）
     */
    @NotBlank(message = "pointJson 不能为空")
    private String pointJson;

    /**
     * 初始化验证码时返回的 token
     */
    @NotBlank(message = "token 不能为空")
    private String token;

    /**
     * 自定义反序列化构造函数，支持字符串转枚举
     */
    @JsonCreator
    public CaptchaVerifyRequest(
            @JsonProperty("captchaType") String captchaType,
            @JsonProperty("pointJson") String pointJson,
            @JsonProperty("token") String token) {
        this.captchaType = CaptchaType.fromValue(captchaType);
        this.pointJson = pointJson;
        this.token = token;
    }
}
