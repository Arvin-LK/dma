package com.dma.core.infrastructure.scanner;

import com.dma.common.enums.DatabaseType;
import com.dma.core.domain.service.JdbcMetadataExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * SQL Server 数据库元数据提取器。
 * 使用 sys.sql_modules + OBJECT_DEFINITION 提取对象 DDL。
 */
@Component
public class SqlServerMetadataExtractor implements JdbcMetadataExtractor {

    private static final Logger log = LoggerFactory.getLogger(SqlServerMetadataExtractor.class);

    @Override
    public boolean supports(DatabaseType dbType) {
        return dbType == DatabaseType.SQLSERVER;
    }

    @Override
    public List<String> extractStoredProcedures(Connection conn, String schema) throws SQLException {
        List<String> procedures = new ArrayList<>();
        String effectiveSchema = schema != null ? schema : "dbo";
        String sql = """
                SELECT s.name AS schema_name, p.name AS proc_name
                FROM sys.procedures p
                JOIN sys.schemas s ON p.schema_id = s.schema_id
                WHERE s.name = ?
                ORDER BY p.name
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, effectiveSchema);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String procName = rs.getString("proc_name");
                    String ddl = getObjectDefinition(conn, effectiveSchema, procName, "P");
                    if (ddl != null && !ddl.isBlank()) {
                        procedures.add(ddl);
                    }
                }
            }
        }
        log.info("SQLServer: extracted {} stored procedures from schema '{}'", procedures.size(), effectiveSchema);
        return procedures;
    }

    @Override
    public List<String> extractFunctions(Connection conn, String schema) throws SQLException {
        List<String> functions = new ArrayList<>();
        String effectiveSchema = schema != null ? schema : "dbo";

        // Scalar functions
        String scalarSql = """
                SELECT s.name AS schema_name, o.name AS func_name
                FROM sys.objects o
                JOIN sys.schemas s ON o.schema_id = s.schema_id
                WHERE o.type IN ('FN', 'IF', 'TF')
                AND s.name = ?
                ORDER BY o.name
                """;
        try (PreparedStatement ps = conn.prepareStatement(scalarSql)) {
            ps.setString(1, effectiveSchema);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String funcName = rs.getString("func_name");
                    String ddl = getObjectDefinition(conn, effectiveSchema, funcName, "FN");
                    if (ddl != null && !ddl.isBlank()) {
                        functions.add(ddl);
                    }
                }
            }
        }
        log.info("SQLServer: extracted {} functions from schema '{}'", functions.size(), effectiveSchema);
        return functions;
    }

    @Override
    public List<String> extractTableDDLs(Connection conn, String schema) throws SQLException {
        List<String> tables = new ArrayList<>();
        String effectiveSchema = schema != null ? schema : "dbo";

        String sql = """
                SELECT s.name AS schema_name, t.name AS table_name
                FROM sys.tables t
                JOIN sys.schemas s ON t.schema_id = s.schema_id
                WHERE s.name = ?
                AND t.is_ms_shipped = 0
                ORDER BY t.name
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, effectiveSchema);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String tableName = rs.getString("table_name");
                    // 尝试用 sp_help 或生成 CREATE TABLE 骨架
                    String ddl = generateTableDDL(conn, effectiveSchema, tableName);
                    if (ddl != null && !ddl.isBlank()) {
                        tables.add(ddl);
                    }
                }
            }
        }
        log.info("SQLServer: extracted {} tables from schema '{}'", tables.size(), effectiveSchema);
        return tables;
    }

    @Override
    public List<String> extractViewDDLs(Connection conn, String schema) throws SQLException {
        List<String> views = new ArrayList<>();
        String effectiveSchema = schema != null ? schema : "dbo";

        String sql = """
                SELECT s.name AS schema_name, v.name AS view_name
                FROM sys.views v
                JOIN sys.schemas s ON v.schema_id = s.schema_id
                WHERE s.name = ?
                AND v.is_ms_shipped = 0
                ORDER BY v.name
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, effectiveSchema);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String viewName = rs.getString("view_name");
                    String ddl = getObjectDefinition(conn, effectiveSchema, viewName, "V");
                    if (ddl != null && !ddl.isBlank()) {
                        views.add(ddl);
                    }
                }
            }
        }
        log.info("SQLServer: extracted {} views from schema '{}'", views.size(), effectiveSchema);
        return views;
    }

    @Override
    public int getObjectCount(Connection conn, String schema) throws SQLException {
        String effectiveSchema = schema != null ? schema : "dbo";
        String sql = """
                SELECT COUNT(*)
                FROM sys.objects o
                JOIN sys.schemas s ON o.schema_id = s.schema_id
                WHERE s.name = ?
                AND o.type IN ('U', 'V', 'P', 'FN', 'IF', 'TF', 'TR')
                AND o.is_ms_shipped = 0
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, effectiveSchema);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    // === Private helpers ===

    /**
     * 通过 OBJECT_DEFINITION 获取对象的创建脚本。
     * SQL Server 内置函数，返回对象的原始 T-SQL 定义文本。
     */
    private String getObjectDefinition(Connection conn, String schema, String objectName, String type) {
        String sql = "SELECT OBJECT_DEFINITION(OBJECT_ID(?))";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, schema + "." + objectName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String def = rs.getString(1);
                    if (def != null && !def.isBlank()) {
                        // 给定义加上 CREATE 头，因为 OBJECT_DEFINITION 只返回 body
                        return wrapCreateHeader(def, schema, objectName, type);
                    }
                }
            }
        } catch (SQLException e) {
            log.debug("Cannot get definition for {} '{}': {}", type, objectName, e.getMessage());
        }
        return null;
    }

    /**
     * 为 OBJECT_DEFINITION 返回的内容加上 CREATE 头部。
     * OBJECT_DEFINITION 只返回 ALTER/BEGIN...END 体，不含 CREATE 语句。
     */
    private String wrapCreateHeader(String definition, String schema, String objectName, String type) {
        String header = switch (type) {
            case "P"  -> String.format("CREATE PROCEDURE [%s].[%s]\n", schema, objectName);
            case "FN" -> String.format("CREATE FUNCTION [%s].[%s]\n", schema, objectName);
            case "V"  -> String.format("CREATE VIEW [%s].[%s] AS\n", schema, objectName);
            default   -> "";
        };
        // 如果定义已以 CREATE 开头，直接返回
        if (definition.trim().toUpperCase().startsWith("CREATE")) {
            return definition;
        }
        return header + definition;
    }

    /**
     * 生成表的 DDL 骨架。
     * 通过 INFORMATION_SCHEMA.COLUMNS 获取列信息并拼接 CREATE TABLE 语句。
     */
    private String generateTableDDL(Connection conn, String schema, String tableName) {
        StringBuilder ddl = new StringBuilder();
        ddl.append(String.format("CREATE TABLE [%s].[%s] (\n", schema, tableName));

        // 获取列信息
        String colSql = """
                SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH,
                       NUMERIC_PRECISION, NUMERIC_SCALE, IS_NULLABLE,
                       COLUMN_DEFAULT
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?
                ORDER BY ORDINAL_POSITION
                """;
        List<String> columns = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(colSql)) {
            ps.setString(1, schema);
            ps.setString(2, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String colName = rs.getString("COLUMN_NAME");
                    String dataType = rs.getString("DATA_TYPE");
                    int maxLen = rs.getInt("CHARACTER_MAXIMUM_LENGTH");
                    int numPrec = rs.getInt("NUMERIC_PRECISION");
                    int numScale = rs.getInt("NUMERIC_SCALE");
                    String nullable = "YES".equals(rs.getString("IS_NULLABLE")) ? "NULL" : "NOT NULL";
                    String defaultVal = rs.getString("COLUMN_DEFAULT");

                    StringBuilder colDef = new StringBuilder();
                    colDef.append(String.format("    [%s] %s", colName, dataType));

                    // 长度/精度
                    if (!rs.wasNull() && maxLen > 0) {
                        colDef.append(String.format("(%d)", maxLen));
                    } else if (!rs.wasNull() && numPrec > 0) {
                        colDef.append(String.format("(%d,%d)", numPrec, numScale));
                    }

                    colDef.append(" ").append(nullable);

                    if (defaultVal != null) {
                        colDef.append(" DEFAULT ").append(defaultVal);
                    }

                    columns.add(colDef.toString());
                }
            }
        } catch (SQLException e) {
            log.debug("Cannot get columns for table '{}': {}", tableName, e.getMessage());
            return String.format("CREATE TABLE [%s].[%s] (...); -- 无法获取列信息: %s",
                    schema, tableName, e.getMessage());
        }

        ddl.append(String.join(",\n", columns));
        ddl.append("\n);");

        // 获取主键信息
        try {
            String pkSql = """
                    SELECT c.COLUMN_NAME
                    FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc
                    JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE c
                      ON tc.CONSTRAINT_NAME = c.CONSTRAINT_NAME
                      AND tc.TABLE_SCHEMA = c.TABLE_SCHEMA
                    WHERE tc.CONSTRAINT_TYPE = 'PRIMARY KEY'
                      AND tc.TABLE_SCHEMA = ?
                      AND tc.TABLE_NAME = ?
                    ORDER BY c.ORDINAL_POSITION
                    """;
            try (PreparedStatement ps = conn.prepareStatement(pkSql)) {
                ps.setString(1, schema);
                ps.setString(2, tableName);
                try (ResultSet rs = ps.executeQuery()) {
                    List<String> pkCols = new ArrayList<>();
                    while (rs.next()) pkCols.add("[" + rs.getString("COLUMN_NAME") + "]");
                    if (!pkCols.isEmpty()) {
                        // 将 ); 替换为 PK 约束 + );
                        ddl.setLength(ddl.length() - 2); // 去掉 );
                        ddl.append(String.format(",\n    CONSTRAINT [PK_%s] PRIMARY KEY (%s)\n);",
                                tableName, String.join(", ", pkCols)));
                    }
                }
            }
        } catch (SQLException e) {
            log.debug("Cannot get PK for table '{}': {}", tableName, e.getMessage());
        }

        return ddl.toString();
    }
}
