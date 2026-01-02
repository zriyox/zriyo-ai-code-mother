package com.zriyo.aicodemother.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    SUCCESS(200, "ok"),
    PARAMS_ERROR(40000, "请求参数错误"),
    NOT_LOGIN_ERROR(40100, "未登录"),
    NO_AUTH_ERROR(40101, "无权限"),
    NOT_FOUND_ERROR(40400, "请求数据不存在"),
    FORBIDDEN_ERROR(40300, "禁止访问"),
    SYSTEM_ERROR(50000, "系统内部异常"),
    BUSINESS_ERROR(50001, "业务错误"),
    //cdk已经兑换
    CDK_ALREADY_EXCHANGED(50007, "cdk已经兑换"),
    //验证码获取频繁
    TOO_MANY_REQUEST(50002, "验证码获取频繁,请一分钟后再试吧!"),
    //校验验证码失败
    CHECK_CODE_ERROR(50003, "邮箱验证码错误"),
    //行为验证码校验失败
    CHECK_CAPTCHA_ERROR(50004, "行为验证码校验失败"),
    //点击过快
    TOO_FAST(50005, "点击过快,请稍后再试!"),
    //正在执行中
    EXECUTING(50005, "正在生成项目中,请勿重复操作!"),
    //积分不足
    INSUFFICIENT_POINTS(50006, "积分不足"),
    OPERATION_ERROR(50001, "操作失败");

    /**
     * 状态码
     */
    private final int code;

    /**
     * 信息
     */
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

}
