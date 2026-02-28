package com.zriyo.aicodemother.exception;

import com.zriyo.common.exception.BusinessException;
import com.zriyo.common.result.ErrorCode;
import com.zriyo.common.result.Result;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.dev33.satoken.exception.SaTokenException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;

import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 *
 * @author Zriyo AI
 * @since 2025-02-17
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 参数校验异常 (@Valid)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));
        log.warn("参数校验异常: {}", message);
        return Result.validationError(message);
    }

    /**
     * 参数绑定异常
     */
    @ExceptionHandler(BindException.class)
    public Result<?> handleBindException(BindException e) {
        String message = e.getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));
        log.warn("参数绑定异常: {}", message);
        return Result.validationError(message);
    }

    /**
     * 非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<?> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("非法参数: {}", e.getMessage());
        return Result.validationError(e.getMessage());
    }

    /**
     * 方法参数校验异常 (@Validated)
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<?> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
            .map(v -> v.getMessage())
            .collect(Collectors.joining(", "));
        log.warn("参数校验异常: {}", message);
        return Result.validationError(message);
    }

    /**
     * 缺少必要参数
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<?> handleMissingServletRequestParameter(MissingServletRequestParameterException e) {
        String message = String.format("缺少参数: %s", e.getParameterName());
        log.warn("缺少参数: {}", e.getParameterName());
        return Result.fail(ErrorCode.PARAM_MISSING.getCode(), message);
    }


    /**
     * 请求体解析异常
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<?> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析异常: {}", e.getMessage());
        return Result.validationError("请求体格式错误");
    }

    /**
     * Sa-Token 未登录
     */
    @ExceptionHandler(NotLoginException.class)
    public Result<?> handleNotLogin(NotLoginException e) {
        String type = e.getType();
        if (NotLoginException.TOKEN_TIMEOUT.equals(type)) {
            return Result.fail(ErrorCode.USER_TOKEN_EXPIRED.getCode(), ErrorCode.USER_TOKEN_EXPIRED.getMessage());
        }
        if (NotLoginException.INVALID_TOKEN.equals(type)
            || NotLoginException.BE_REPLACED.equals(type)
            || NotLoginException.KICK_OUT.equals(type)
            || NotLoginException.NOT_TOKEN.equals(type)) {
            return Result.fail(ErrorCode.USER_TOKEN_INVALID.getCode(), ErrorCode.USER_TOKEN_INVALID.getMessage());
        }
        return Result.fail(ErrorCode.NOT_LOGIN_ERROR.getCode(), ErrorCode.NOT_LOGIN_ERROR.getMessage());
    }

    /**
     * Sa-Token 权限不足
     */
    @ExceptionHandler(NotPermissionException.class)
    public Result<?> handleNotPermission(NotPermissionException e) {
        log.warn("权限不足: {}", e.getMessage());
        return Result.fail(ErrorCode.NO_AUTH_ERROR.getCode(), ErrorCode.NO_AUTH_ERROR.getMessage());
    }

    /**
     * Sa-Token 角色不足
     */
    @ExceptionHandler(NotRoleException.class)
    public Result<?> handleNotRole(NotRoleException e) {
        log.warn("角色不足: {}", e.getMessage());
        return Result.fail(ErrorCode.NO_AUTH_ERROR.getCode(), ErrorCode.NO_AUTH_ERROR.getMessage());
    }

    /**
     * Sa-Token 通用异常
     */
    @ExceptionHandler(SaTokenException.class)
    public Result<?> handleSaTokenException(SaTokenException e) {
        log.warn("Sa-Token 异常: {}", e.getMessage());
        return Result.fail(ErrorCode.NO_AUTH_ERROR.getCode(), e.getMessage());
    }

    /**
     * 系统异常
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail(ErrorCode.SYSTEM_ERROR);
    }


}
