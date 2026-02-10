package com.teamup.server.common.exception;

/**
 * 验证异常
 * 当数据验证失败时抛出
 */
public class ValidationException extends RuntimeException {
    
    public ValidationException(String message) {
        super(message);
    }
    
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
