package com.zriyo.aicodemother.model;

/**
 * 坐标点结构（用于服务端生成 pointJson 或接收明文坐标）
 * 坐标为前端基准尺寸换算后的整数：基准宽 310，高 155
 */
public class CaptchaPoint {
    /**
     * 横坐标（滑块为横向位移；点击为点击位置 X）
     */
    private int x;
    /**
     * 纵坐标（滑块固定为 5；点击为点击位置 Y）
     */
    private int y;

    public CaptchaPoint() {}

    public CaptchaPoint(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }
}
