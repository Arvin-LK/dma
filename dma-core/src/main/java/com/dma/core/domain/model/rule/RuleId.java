package com.dma.core.domain.model.rule;

/** 规则唯一标识 */
public record RuleId(Long value) {
    public RuleId { if (value == null || value <= 0) throw new IllegalArgumentException("RuleId must be positive"); }
}
