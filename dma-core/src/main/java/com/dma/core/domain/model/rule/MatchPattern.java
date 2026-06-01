package com.dma.core.domain.model.rule;

/** 匹配模式值对象 */
public record MatchPattern(String expression) {
    public MatchPattern {
        if (expression == null || expression.isBlank()) throw new IllegalArgumentException("MatchPattern must not be blank");
    }
    public boolean isRegex() { return expression.startsWith("regex:"); }
    public String getRegexBody() { return isRegex() ? expression.substring(6) : expression; }
}
