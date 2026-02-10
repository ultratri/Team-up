package com.teamup.server.common.exception;

/**
 * 资源不存在异常
 * 当请求的资源不存在时抛出
 */
public class ResourceNotFoundException extends RuntimeException {
    
    public ResourceNotFoundException(String message) {
        super(message);
    }
    
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
