package com.zriyo.aicodemother.model.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.zriyo.aicodemother.model.enums.CaptchaType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * 滑块验证码请求参数实体类（使用枚举）
 */
@Data
@NoArgsConstructor
public class CaptchaRequest {

    /**
     * 验证码类型
     */
    @NotNull(message = "captchaType 不能为空")
    private CaptchaType captchaType;


    /**
     * 时间戳（毫秒）
     */
    @NotNull(message = "ts 不能为空")
    private Long ts;

    /**
     * 自定义反序列化构造函数（兼容 JSON 字符串 -> 枚举）
     */
    @JsonCreator
    public CaptchaRequest(
            @JsonProperty("captchaType") String captchaType,
            @JsonProperty("clientUid") String clientUid,
            @JsonProperty("ts") Long ts) {
        this.captchaType = CaptchaType.fromValue(captchaType);
        this.ts = ts;
    }
}
