package com.zriyo.common.exception;

import com.zriyo.common.result.ErrorCode;

/**
 * 抛出异常工具类
 */
public class ThrowUtils {

    /**
     * 如果条件成立，抛出业务异常
     */
    public static void throwIf(boolean condition, ErrorCode errorCode) {
        if (condition) {
            throw new BusinessException(errorCode);
        }
    }

    /**
     * 如果条件成立，抛出业务异常（自定义消息）
     */
    public static void throwIf(boolean condition, ErrorCode errorCode, String message) {
        if (condition) {
            throw new BusinessException(errorCode, message);
        }
    }

    /**
     * 如果条件不成立，抛出业务异常
     */
    public static void throwIfNot(boolean condition, ErrorCode errorCode) {
        if (!condition) {
            throw new BusinessException(errorCode);
        }
    }

    /**
     * 如果条件不成立，抛出业务异常（自定义消息）
     */
    public static void throwIfNot(boolean condition, ErrorCode errorCode, String message) {
        if (!condition) {
            throw new BusinessException(errorCode, message);
        }
    }

    /**
     * 如果对象为null，抛出业务异常
     */
    public static <T> T throwIfNull(T obj, ErrorCode errorCode) {
        if (obj == null) {
            throw new BusinessException(errorCode);
        }
        return obj;
    }

    /**
     * 如果对象为null，抛出业务异常（自定义消息）
     */
    public static <T> T throwIfNull(T obj, ErrorCode errorCode, String message) {
        if (obj == null) {
            throw new BusinessException(errorCode, message);
        }
        return obj;
    }

    /**
     * 如果字符串为空白，抛出业务异常
     */
    public static String throwIfBlank(String str, ErrorCode errorCode) {
        if (str == null || str.trim().isEmpty()) {
            throw new BusinessException(errorCode);
        }
        return str;
    }

    /**
     * 如果字符串为空白，抛出业务异常（自定义消息）
     */
    public static String throwIfBlank(String str, ErrorCode errorCode, String message) {
        if (str == null || str.trim().isEmpty()) {
            throw new BusinessException(errorCode, message);
        }
        return str;
    }
}
