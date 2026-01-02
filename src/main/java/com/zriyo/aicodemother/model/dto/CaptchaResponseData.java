package com.zriyo.aicodemother.model.dto;

import lombok.Data;

/**
 * 验证码校验返回的 repData 内容
 */
@Data
public class CaptchaResponseData {

    /**
     * 底图 Base64（完整背景图）
     */
    private String originalImageBase64;

    /**
     * 滑块缺口坐标（仅在校验时由后端使用，前端通常不接收；但若返回则包含）
     */
    private Point point;

    /**
     * 滑块小图 Base64
     */
    private String jigsawImageBase64;

    /**
     * 本次验证的唯一 token，用于二次校验
     */
    private String token;

    /**
     * AES 加密密钥（16位随机字符串），前端根据此决定是否加密轨迹
     */
    private String secretKey;

    /**
     * 校验结果：true 表示通过，false 表示失败
     */
    private Boolean result;

    /**
     * 是否为管理员操作（调试/绕过用）
     */
    private Boolean opAdmin;
}
