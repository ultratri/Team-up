package com.teamup.server.common.exception;

/**
 * 授权异常
 * 当用户尝试执行未授权的操作时抛出
 */
public class AuthorizationException extends RuntimeException {
    
    public AuthorizationException(String message) {
        super(message);
    }
    
    public AuthorizationException(String message, Throwable cause) {
        super(message, cause);
    }
}
