package com.dma.common.exception;

/** 规则相关异常基类 */
public class RuleException extends DmaException {
    public RuleException(String errorCode, String message) { super(errorCode, message); }
    public RuleException(String errorCode, String message, Throwable cause) { super(errorCode, message, cause); }
}
