package com.dma.common.exception;

/** 数据库连接异常基类 */
public class ConnectionException extends DmaException {
    public ConnectionException(String errorCode, String message) {
        super(errorCode, message);
    }
    public ConnectionException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
