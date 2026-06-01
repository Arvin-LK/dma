package com.dma.core.domain.model.rule;

/** 替换模式值对象 */
public record ReplacementPattern(String template) {
    public static final ReplacementPattern EMPTY = new ReplacementPattern("");
}
