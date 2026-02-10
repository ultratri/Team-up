package com.teamup.server.common.exception;

import com.teamup.server.common.api.ResultCode;

/**
 * 自定义业务异常
 */
public class BusinessException extends RuntimeException {
    private ResultCode errorCode;

    public BusinessException(ResultCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(ResultCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessException(Throwable cause) {
        super(cause);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }

    public ResultCode getErrorCode() {
        return errorCode;
    }
}
