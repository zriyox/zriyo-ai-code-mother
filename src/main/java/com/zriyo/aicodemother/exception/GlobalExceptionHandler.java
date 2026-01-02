package com.zriyo.aicodemother.exception;

import cn.dev33.satoken.exception.NotLoginException;
import com.zriyo.aicodemother.common.BaseResponse;
import com.zriyo.aicodemother.common.ResultUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;


@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e) {
        log.error("BusinessException", e);
        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e) {
        log.error("RuntimeException", e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public BaseResponse<?> handle400(HttpMessageNotReadableException e) {
        log.error("HttpMessageNotReadableException", e);
        return ResultUtils.error(ErrorCode.PARAMS_ERROR);
    }

    /**
     * 处理参数校验失败异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public BaseResponse<?> handleValidationException(MethodArgumentNotValidException e) {
        String errorMsg = Optional.ofNullable(e.getBindingResult().getFieldError())
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .orElse("参数错误");

        log.warn("参数校验失败: {}", errorMsg);

        return ResultUtils.error(ErrorCode.PARAMS_ERROR, errorMsg);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    @ResponseBody
    public Map<String, Object> handle404(HttpServletRequest request) {
        return Map.of(
                "error", "Not Found",
                "state", "405",
                "path", request.getRequestURI(),
                "timestamp", ZonedDateTime.now(ZoneId.of("Asia/Shanghai")).toOffsetDateTime().toString()

        );
    }
    @ExceptionHandler(NotLoginException.class)
    public BaseResponse<?> notLoginExceptionHandler(NotLoginException e) {
        return ResultUtils.error(ErrorCode.NOT_LOGIN_ERROR);
    }
}
