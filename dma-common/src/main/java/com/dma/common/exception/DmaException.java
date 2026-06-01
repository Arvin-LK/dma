package com.dma.common.exception;

/**
 * DMA 基础异常。
 * 所有自定义异常均继承此类，携带业务错误码。
 */
public class DmaException extends RuntimeException {

    private final String errorCode;

    public DmaException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public DmaException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
