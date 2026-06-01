package com.dma.core.domain.model.migration;

/** 迁移风险评分值对象 (0-100) */
public record RiskScore(int value) {
    public RiskScore {
        if (value < 0 || value > 100) throw new IllegalArgumentException("RiskScore must be 0-100");
    }
    public static final RiskScore ZERO = new RiskScore(0);
    public String toLevel() {
        if (value <= 25) return "低风险";
        if (value <= 50) return "中风险";
        if (value <= 75) return "高风险";
        return "极高风险";
    }
}
