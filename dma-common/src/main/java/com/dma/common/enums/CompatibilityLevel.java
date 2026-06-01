package com.dma.common.enums;

/**
 * SQL 兼容性级别。
 */
public enum CompatibilityLevel {

    /** 完全兼容，无需修改 */
    COMPATIBLE,

    /** 可自动转换（规则引擎可直接替换） */
    AUTO_CONVERTIBLE,

    /** 需要人工审核（转换有风险或不确定） */
    MANUAL_REVIEW,

    /** 不兼容且无法自动转换 */
    INCOMPATIBLE,

    /** SQL 无法解析 */
    PARSE_ERROR
}
