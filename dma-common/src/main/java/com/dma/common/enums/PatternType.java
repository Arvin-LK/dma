package com.dma.common.enums;

/**
 * 规则匹配模式类型。
 * 决定了规则引擎使用何种匹配算法。
 */
public enum PatternType {

    /** 内置函数名称替换 */
    FUNCTION,

    /** SQL 语法结构差异 */
    SYNTAX,

    /** 数据类型映射 */
    DATATYPE,

    /** 关键词差异 */
    KEYWORD,

    /** 标识符引用差异 */
    BUILTIN
}
