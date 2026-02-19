package com.zriyo.common.result;

/**
 * 快速构造响应结果的工具类
 *
 * @author Zriyo AI
 * @since 2025-02-17
 */
public class ResultUtils {

    /**
     * 成功（带数据）
     */
    public static <T> Result<T> success(T data) {
        return Result.ok(data);
    }

    /**
     * 成功（无数据）
     */
    public static <T> Result<T> success() {
        return Result.ok();
    }

    /**
     * 成功（带消息和数据）
     */
    public static <T> Result<T> success(String message, T data) {
        return Result.ok(message, data);
    }

    /**
     * 失败（错误码）
     */
    public static Result<?> error(ErrorCode errorCode) {
        return Result.fail(errorCode);
    }

    /**
     * 失败（错误码和消息）
     */
    public static Result<?> error(ErrorCode errorCode, String message) {
        return Result.fail(errorCode.getCode(), message);
    }

    /**
     * 失败（错误码和消息）
     */
    public static Result<?> error(int code, String message) {
        return Result.fail(code, message);
    }

    /**
     * 失败（消息）
     */
    public static Result<?> error(String message) {
        return Result.fail(message);
    }
}
