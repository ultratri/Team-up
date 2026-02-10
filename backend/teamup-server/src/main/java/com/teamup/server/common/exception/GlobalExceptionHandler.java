package com.teamup.server.common.exception;

import com.teamup.server.common.api.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 全局异常处理
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ResponseBody
    @ExceptionHandler(value = BusinessException.class)
    public Result<?> handle(BusinessException e) {
        log.warn("Business exception: {}", e.getMessage());
        if (e.getErrorCode() != null) {
            return Result.failed(e.getErrorCode());
        }
        return Result.failed(e.getMessage());
    }

    @ResponseBody
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public Result<?> handleValidException(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        String message = null;
        if (bindingResult.hasErrors()) {
            FieldError fieldError = bindingResult.getFieldError();
            if (fieldError != null) {
                message = fieldError.getField() + ": " + fieldError.getDefaultMessage();
            }
        }
        log.warn("Validation exception: {}", message);
        return Result.validateFailed(message);
    }

    @ResponseBody
    @ExceptionHandler(value = BindException.class)
    public Result<?> handleValidException(BindException e) {
        BindingResult bindingResult = e.getBindingResult();
        String message = null;
        if (bindingResult.hasErrors()) {
            FieldError fieldError = bindingResult.getFieldError();
            if (fieldError != null) {
                message = fieldError.getField() + ": " + fieldError.getDefaultMessage();
            }
        }
        log.warn("Validation exception: {}", message);
        return Result.validateFailed(message);
    }

    @ResponseBody
    @ExceptionHandler(value = ValidationException.class)
    public Result<?> handleValidationException(ValidationException e) {
        log.warn("Validation exception: {}", e.getMessage());
        return Result.error(400, e.getMessage());
    }

    @ResponseBody
    @ExceptionHandler(value = AuthorizationException.class)
    public Result<?> handleAuthorizationException(AuthorizationException e) {
        log.warn("Authorization exception: {}", e.getMessage());
        return Result.error(403, e.getMessage());
    }

    @ResponseBody
    @ExceptionHandler(value = ResourceNotFoundException.class)
    public Result<?> handleResourceNotFoundException(ResourceNotFoundException e) {
        log.warn("Resource not found: {}", e.getMessage());
        return Result.error(404, e.getMessage());
    }

    @ResponseBody
    @ExceptionHandler(value = AccessDeniedException.class)
    public Result<?> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("Access denied: {}", e.getMessage());
        return Result.error(403, "权限不足");
    }

    @ResponseBody
    @ExceptionHandler(value = RedisConnectionFailureException.class)
    public Result<?> handleRedisConnectionFailureException(RedisConnectionFailureException e) {
        // Redis 连接失败时记录警告，但返回成功（缓存降级场景）
        log.warn("Redis connection failed, degrading gracefully: {}", e.getMessage());
        // 返回成功状态，缓存不可用不应影响业务
        return Result.success(null);
    }

    @ResponseBody
    @ExceptionHandler(value = DataAccessException.class)
    public Result<?> handleDataAccessException(DataAccessException e) {
        // 检查是否是 Redis 相关的错误
        String message = e.getMessage();
        Throwable cause = e.getCause();
        String causeMessage = cause != null ? cause.getMessage() : "";
        
        if ((message != null && (message.contains("Redis") || message.contains("redis") || 
            message.contains("Unable to connect"))) ||
            (causeMessage != null && (causeMessage.contains("Redis") || causeMessage.contains("redis")))) {
            // Redis 错误不应该影响业务，只记录警告并返回成功
            log.warn("Redis error (graceful degradation): {}", message);
            return Result.success(null);
        }
        
        log.error("Database error", e);
        return Result.error(500, "数据库操作失败");
    }
    
    @ResponseBody
    @ExceptionHandler(value = Exception.class)
    public Result<?> handle(Exception e) {
        log.error("Unexpected error", e);
        return Result.error(500, "系统错误，请稍后重试");
    }
}
