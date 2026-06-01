package com.dma.common.util;

/**
 * SQL 规范化工具。
 * 移除多余空白、注释，标准化 SQL 格式便于规则匹配。
 */
public final class SqlNormalizer {

    private SqlNormalizer() {}

    /** 规范化 SQL：移除多余空白、换行、注释 */
    public static String normalize(String sql) {
        if (sql == null || sql.isBlank()) return "";
        return sql
            .replaceAll("--[^\n]*", " ")     // 移除单行注释
            .replaceAll("/\\*.*?\\*/", " ")  // 移除多行注释
            .replaceAll("\\s+", " ")          // 合并空白
            .trim();
    }
}
