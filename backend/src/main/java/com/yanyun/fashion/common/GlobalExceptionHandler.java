package com.yanyun.fashion.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

/**
 * 全局异常处理：所有异常均转换为统一响应体，message 为可直接展示的中文。
 *
 * <p>说明：鉴权失败沿用 JeecgBoot 习惯，HTTP 状态返回 200，响应体 {@code code} 为 401/403，
 * 便于前端在统一响应拦截器里处理（小程序端亦不受非 2xx 影响）。</p>
 *
 * @author yanyun backend
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Result<Object> handleBizException(BizException e, HttpServletRequest request) {
        log.warn("[业务异常] {} {} -> code={} message={}", request.getMethod(), request.getRequestURI(),
                e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result<Object> handleValidationException(BindException e) {
        String message = "请求参数不合法";
        FieldError fieldError = e.getBindingResult().getFieldError();
        if (fieldError != null && fieldError.getDefaultMessage() != null) {
            message = fieldError.getDefaultMessage();
        }
        log.warn("[参数校验失败] {}", message);
        return Result.error(message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Object> handleConstraintViolation(ConstraintViolationException e) {
        String message = "请求参数不合法";
        for (ConstraintViolation<?> violation : e.getConstraintViolations()) {
            if (violation.getMessage() != null && !violation.getMessage().isEmpty()) {
                message = violation.getMessage();
                break;
            }
        }
        log.warn("[参数校验失败] {}", message);
        return Result.error(message);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Object> handleMissingParam(MissingServletRequestParameterException e) {
        return Result.error("缺少必要参数：" + e.getParameterName());
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public Result<Object> handleMissingPart(MissingServletRequestPartException e) {
        return Result.error("请选择要上传的文件");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Object> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return Result.error("参数格式不正确：" + e.getName());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Object> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("[请求体解析失败] {}", e.getMessage());
        return Result.error("请求体格式不正确，请检查 JSON 内容");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Object> handleMaxUpload(MaxUploadSizeExceededException e) {
        return Result.error("文件过大，单个文件不得超过 5MB");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Object> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return Result.error("请求方法不支持：" + e.getMethod());
    }

    @ExceptionHandler(Exception.class)
    public Result<Object> handleException(Exception e, HttpServletRequest request) {
        log.error("[系统异常] {} {}", request.getMethod(), request.getRequestURI(), e);
        return Result.error("服务开小差了，请稍后重试");
    }
}
