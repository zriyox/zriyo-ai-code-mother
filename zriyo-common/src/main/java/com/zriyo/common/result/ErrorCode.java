package com.zriyo.common.result;

import lombok.Getter;

/**
 * 错误码枚举
 *
 * @author Zriyo AI
 * @since 2025-02-17
 */
@Getter
public enum ErrorCode {

    // 通用错误码 1xxx
    SUCCESS(200, "成功"),
    SYSTEM_ERROR(1000, "系统错误"),
    PARAM_ERROR(1001, "参数错误"),
    PARAM_MISSING(1002, "缺少必要参数"),
    OPERATION_FAILED(1003, "操作失败"),

    // 用户相关 2xxx
    USER_NOT_FOUND(2001, "用户不存在"),
    USER_ALREADY_EXISTS(2002, "用户已存在"),
    USER_LOGIN_FAILED(2003, "用户名或密码错误"),
    USER_ACCOUNT_DISABLED(2004, "账号已被禁用"),
    USER_TOKEN_EXPIRED(2005, "Token 已过期"),
    USER_TOKEN_INVALID(2006, "Token 无效"),
    NOT_LOGIN_ERROR(2007, "未登录"),
    NO_AUTH_ERROR(2008, "无权限"),

    // 权限相关 3xxx
    PERMISSION_DENIED(3001, "权限不足"),
    PERMISSION_NOT_FOUND(3002, "权限不存在"),
    ROLE_NOT_FOUND(3003, "角色不存在"),

    // 业务相关 4xxx
    BUSINESS_ERROR(4000, "业务处理失败"),
    RESOURCE_NOT_FOUND(4004, "资源不存在"),
    RESOURCE_ALREADY_EXISTS(4005, "资源已存在"),
    OPERATION_ERROR(4006, "操作失败"),
    NOT_FOUND_ERROR(4007, "请求数据不存在"),
    FORBIDDEN_ERROR(4008, "禁止访问"),
    TOO_MANY_REQUEST(4009, "验证码获取频繁,请一分钟后再试"),
    CHECK_CODE_ERROR(4010, "邮箱验证码错误"),
    CHECK_CAPTCHA_ERROR(4011, "行为验证码校验失败"),
    TOO_FAST(4012, "点击过快,请稍后再试"),
    EXECUTING(4013, "正在生成项目中,请勿重复操作"),
    INSUFFICIENT_POINTS(4014, "积分不足"),
    CDK_ALREADY_EXCHANGED(4015, "CDK已经兑换"),

    // Python 服务相关 5xxx
    PYTHON_SERVICE_ERROR(5001, "Python 服务异常"),
    PYTHON_SERVICE_UNAVAILABLE(5002, "Python 服务不可用"),
    PYTHON_SERVICE_TIMEOUT(5003, "Python 服务超时"),
    PYTHON_START_FAILED(5004, "Python 服务启动失败"),

    // Agent 相关 6xxx
    AGENT_NOT_FOUND(6001, "Agent 不存在"),
    AGENT_EXECUTION_FAILED(6002, "Agent 执行失败"),
    AGENT_TIMEOUT(6003, "Agent 执行超时"),
    ;

    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
