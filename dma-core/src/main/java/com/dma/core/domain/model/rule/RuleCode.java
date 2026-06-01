package com.dma.core.domain.model.rule;

/** 规则编码值对象，如 M2PG-FN-001 */
public record RuleCode(String value) {
    public RuleCode {
        if (value == null || !value.matches("^[A-Z]\\d+[A-Z]+-\\w+-\\d{3}$"))
            throw new IllegalArgumentException("Invalid rule code: " + value);
    }
}
