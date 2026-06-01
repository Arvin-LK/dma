package com.dma.common.exception;
public class RuleNotFoundException extends RuleException {
    public RuleNotFoundException(String ruleCode) { super("RULE_001", "规则未找到: " + ruleCode); }
}
