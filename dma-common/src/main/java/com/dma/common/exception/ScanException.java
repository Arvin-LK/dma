package com.dma.common.exception;
public class ScanException extends DmaException {
    public ScanException(String errorCode, String message) { super(errorCode, message); }
    public ScanException(String errorCode, String message, Throwable cause) { super(errorCode, message, cause); }
}
