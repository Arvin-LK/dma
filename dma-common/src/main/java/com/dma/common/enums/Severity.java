package com.dma.common.enums;

/**
 * 规则严重程度。
 */
public enum Severity {

    /** 错误 — 阻止迁移，必须修复 */
    ERROR,

    /** 警告 — 需要关注，建议修复 */
    WARNING,

    /** 信息 — 仅供参考 */
    INFO
}
