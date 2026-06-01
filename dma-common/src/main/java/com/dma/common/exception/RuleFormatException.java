package com.dma.common.exception;
public class RuleFormatException extends RuleException {
    public RuleFormatException(String filePath, Throwable cause) { super("RULE_002", "规则格式错误: " + filePath, cause); }
}
