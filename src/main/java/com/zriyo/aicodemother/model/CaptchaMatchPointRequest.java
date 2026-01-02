package com.zriyo.aicodemother.model;

import java.util.List;

/**
 * 提交验证码校验的请求体（结构化点位版）
 * 若在服务端生成 pointJson（或校验坐标明文），使用该结构更清晰
 */
public class CaptchaMatchPointRequest {
    /**
     * 验证码类型："blockPuzzle"（滑块拼图）或 "clickWord"（点击文字）
     */
    private String captchaType;
    /**
     * 后端返回的令牌（来自获取验证码接口 repData.token）
     */
    private String token;
    /**
     * 坐标点列表：
     * - 滑块：仅一个点（x 为位移，y 固定为 5）
     * - 点击：三个点，按 wordList 顺序
     */
    private List<CaptchaPoint> points;

    public CaptchaMatchPointRequest() {}

    public CaptchaMatchPointRequest(String captchaType, String token, List<CaptchaPoint> points) {
        this.captchaType = captchaType;
        this.token = token;
        this.points = points;
    }

    public String getCaptchaType() {
        return captchaType;
    }

    public void setCaptchaType(String captchaType) {
        this.captchaType = captchaType;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public List<CaptchaPoint> getPoints() {
        return points;
    }

    public void setPoints(List<CaptchaPoint> points) {
        this.points = points;
    }
}
