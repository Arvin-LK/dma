package com.dma.core.infrastructure.scanner;

import com.dma.core.domain.model.scanner.ScanResult;
import com.dma.core.domain.model.scanner.ScanSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * SQL 保留关键字检测器。
 * 检测 SQL 中使用的表名/列名是否为目标数据库的保留关键字。
 */
@Component
public class ReservedKeywordDetector {

    private static final Logger log = LoggerFactory.getLogger(ReservedKeywordDetector.class);

    // PostgreSQL 保留关键字（部分常用）
    private static final Set<String> PG_KEYWORDS = Set.of(
            "USER", "ORDER", "GROUP", "TABLE", "SELECT", "FROM", "WHERE",
            "CREATE", "ALTER", "DROP", "INSERT", "UPDATE", "DELETE",
            "GRANT", "REVOKE", "COMMIT", "ROLLBACK", "SAVEPOINT",
            "PRIMARY", "FOREIGN", "KEY", "REFERENCES", "CHECK", "UNIQUE",
            "INDEX", "VIEW", "TRIGGER", "FUNCTION", "PROCEDURE",
            "CASE", "WHEN", "THEN", "ELSE", "END", "NULL", "TRUE", "FALSE",
            "AND", "OR", "NOT", "IN", "EXISTS", "BETWEEN", "LIKE",
            "JOIN", "LEFT", "RIGHT", "INNER", "OUTER", "CROSS", "ON",
            "AS", "CAST", "COALESCE", "NULLIF", "GREATEST", "LEAST",
            "ARRAY", "BYTEA", "TEXT", "VARCHAR", "INTEGER", "NUMERIC",
            "TIMESTAMP", "DATE", "BOOLEAN", "SERIAL", "BIGSERIAL"
    );

    // 达梦保留关键字（部分常用）
    private static final Set<String> DM_KEYWORDS = Set.of(
            "USER", "ORDER", "GROUP", "TABLE", "SELECT", "FROM",
            "COMMENT", "AUDIT", "NOAUDIT", "CONNECT", "RESOURCE",
            "EXCLUSIVE", "SHARE", "NOWAIT", "VALIDATE", "PRIOR",
            "START", "LEVEL", "SIZE", "NEXT", "SESSION", "SYSTEM"
    );

    // MySQL 到 PG 需要引号的关键字（检测表名/列名）
    private static final Set<String> RISKY_IDENTIFIERS = Set.of(
            "USER", "ORDER", "GROUP", "TABLE", "COMMENT", "ARRAY",
            "TYPE", "NAME", "VALUE", "KEY", "INDEX", "VIEW",
            "START", "END", "STATE", "STATUS", "LEVEL", "SIZE"
    );

    // 这些关键字在 DDL/DML 中作为标识符使用时会出问题
    private static final Map<String, String> RISK_LEVEL = Map.ofEntries(
            // 高风险：会导致语法错误
            Map.entry("USER", "HIGH"),
            Map.entry("ORDER", "HIGH"),
            Map.entry("GROUP", "HIGH"),
            Map.entry("TABLE", "HIGH"),
            Map.entry("ARRAY", "HIGH"),
            Map.entry("TYPE", "HIGH"),
            // 中风险：可能不兼容
            Map.entry("COMMENT", "MEDIUM"),
            Map.entry("KEY", "MEDIUM"),
            Map.entry("INDEX", "MEDIUM"),
            Map.entry("VIEW", "MEDIUM"),
            Map.entry("START", "MEDIUM"),
            Map.entry("END", "MEDIUM"),
            // 低风险：建议加引号
            Map.entry("NAME", "LOW"),
            Map.entry("VALUE", "LOW"),
            Map.entry("STATE", "LOW"),
            Map.entry("STATUS", "LOW"),
            Map.entry("LEVEL", "LOW"),
            Map.entry("SIZE", "LOW")
    );

    /**
     * 检测 SQL 中是否使用了保留关键字作为标识符。
     */
    public List<ScanResult> detect(String sql, String filePath, int lineNumber) {
        List<ScanResult> results = new ArrayList<>();
        String upper = sql.toUpperCase();

        for (String keyword : RISKY_IDENTIFIERS) {
            // 检测模式: 关键字出现在表名/列名位置
            // 简化检测: 在 CREATE TABLE / ALTER TABLE / SELECT FROM / INSERT INTO 上下文中
            if (upper.contains(" " + keyword + " ") || upper.contains("." + keyword + " ")
                    || upper.contains("(" + keyword + " ") || upper.contains(" " + keyword + ",")
                    || upper.contains(" " + keyword + ")")) {

                String risk = RISK_LEVEL.getOrDefault(keyword, "LOW");
                String severity = "HIGH".equals(risk) ? "ERROR" : ("MEDIUM".equals(risk) ? "WARNING" : "INFO");

                ScanResult result = new ScanResult(
                        "KW-" + keyword,
                        sql.length() > 200 ? sql.substring(0, 200) + "..." : sql,
                        "MANUAL_REVIEW",
                        severity,
                        "⚠ 标识符 '" + keyword + "' 是目标数据库的保留关键字，建议使用双引号包围或重命名",
                        ScanSource.SQL_FILE
                );
                result.setFilePath(filePath);
                result.setLineNumber(lineNumber);
                results.add(result);
            }
        }

        return results;
    }

    /**
     * 获取目标数据库的保留关键字数量。
     */
    public int getKeywordCount(String dbType) {
        return switch (dbType.toUpperCase()) {
            case "POSTGRESQL", "GAUSSDB" -> PG_KEYWORDS.size();
            case "DAMENG" -> DM_KEYWORDS.size();
            default -> PG_KEYWORDS.size();
        };
    }
}
