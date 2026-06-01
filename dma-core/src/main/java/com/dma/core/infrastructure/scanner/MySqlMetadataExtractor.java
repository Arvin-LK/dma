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
 * MySQL 数据库元数据提取器。
 * 使用 JDBC DatabaseMetaData + SHOW CREATE 语句提取对象 DDL。
 */
@Component
public class MySqlMetadataExtractor implements JdbcMetadataExtractor {

    private static final Logger log = LoggerFactory.getLogger(MySqlMetadataExtractor.class);

    @Override
    public boolean supports(DatabaseType dbType) {
        return dbType == DatabaseType.MYSQL;
    }

    @Override
    public List<String> extractStoredProcedures(Connection conn, String schema) throws SQLException {
        List<String> procedures = new ArrayList<>();
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getProcedures(schema, null, null)) {
            while (rs.next()) {
                String name = rs.getString("PROCEDURE_NAME");
                String type = rs.getString("PROCEDURE_TYPE");
                // PROCEDURE_TYPE = 2 表示存储过程（1=函数）
                if (name != null && type != null && Integer.parseInt(type) == DatabaseMetaData.procedureNoResult) {
                    String ddl = getProcedureDDL(conn, schema, name, "PROCEDURE");
                    if (ddl != null && !ddl.isBlank()) {
                        procedures.add(ddl);
                    }
                }
            }
        }
        log.info("MySQL: extracted {} stored procedures from schema '{}'", procedures.size(), schema);
        return procedures;
    }

    @Override
    public List<String> extractFunctions(Connection conn, String schema) throws SQLException {
        List<String> functions = new ArrayList<>();
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getFunctions(schema, null, null)) {
            while (rs.next()) {
                String name = rs.getString("FUNCTION_NAME");
                if (name != null) {
                    String ddl = getProcedureDDL(conn, schema, name, "FUNCTION");
                    if (ddl != null && !ddl.isBlank()) {
                        functions.add(ddl);
                    }
                }
            }
        }
        // 也检查 PROCEDURES 中 FUNCTION_TYPE 的记录
        try (ResultSet rs = meta.getProcedures(schema, null, null)) {
            while (rs.next()) {
                String name = rs.getString("PROCEDURE_NAME");
                String type = rs.getString("PROCEDURE_TYPE");
                if (name != null && type != null && Integer.parseInt(type) == DatabaseMetaData.functionResultUnknown) {
                    if (functions.stream().noneMatch(f -> f.contains(name))) {
                        String ddl = getProcedureDDL(conn, schema, name, "FUNCTION");
                        if (ddl != null && !ddl.isBlank()) functions.add(ddl);
                    }
                }
            }
        }
        log.info("MySQL: extracted {} functions from schema '{}'", functions.size(), schema);
        return functions;
    }

    @Override
    public List<String> extractTableDDLs(Connection conn, String schema) throws SQLException {
        List<String> tables = new ArrayList<>();
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getTables(schema, null, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                if (tableName != null) {
                    String ddl = getCreateStatement(conn, schema, tableName, "TABLE");
                    if (ddl != null && !ddl.isBlank()) {
                        tables.add(ddl);
                    }
                }
            }
        }
        log.info("MySQL: extracted {} tables from schema '{}'", tables.size(), schema);
        return tables;
    }

    @Override
    public List<String> extractViewDDLs(Connection conn, String schema) throws SQLException {
        List<String> views = new ArrayList<>();
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getTables(schema, null, "%", new String[]{"VIEW"})) {
            while (rs.next()) {
                String viewName = rs.getString("TABLE_NAME");
                if (viewName != null) {
                    String ddl = getCreateStatement(conn, schema, viewName, "VIEW");
                    if (ddl != null && !ddl.isBlank()) {
                        views.add(ddl);
                    }
                }
            }
        }
        log.info("MySQL: extracted {} views from schema '{}'", views.size(), schema);
        return views;
    }

    @Override
    public int getObjectCount(Connection conn, String schema) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        int count = 0;
        try (ResultSet rs = meta.getTables(schema, null, "%", new String[]{"TABLE", "VIEW"})) {
            while (rs.next()) count++;
        }
        try (ResultSet rs = meta.getProcedures(schema, null, null)) {
            while (rs.next()) count++;
        }
        try (ResultSet rs = meta.getFunctions(schema, null, null)) {
            while (rs.next()) count++;
        }
        return count;
    }

    // === Private helpers ===

    /** 通过 SHOW CREATE 获取存储过程/函数的 DDL */
    private String getProcedureDDL(Connection conn, String schema, String name, String type) {
        try (Statement stmt = conn.createStatement()) {
            String sql = String.format("SHOW CREATE %s `%s`.`%s`", type, schema, name);
            try (ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    // SHOW CREATE PROCEDURE/FUNCTION 返回的第二个字段是 Create Procedure/Function
                    String ddl = rs.getString(2);
                    return ddl != null ? ddl : "";
                }
            }
        } catch (SQLException e) {
            log.debug("Cannot get DDL for {} '{}'.'{}': {}", type, schema, name, e.getMessage());
        }
        return null;
    }

    /** 通过 SHOW CREATE TABLE/VIEW 获取对象的 DDL */
    private String getCreateStatement(Connection conn, String schema, String tableName, String objectType) {
        try (Statement stmt = conn.createStatement()) {
            String sql = String.format("SHOW CREATE %s `%s`.`%s`", objectType, schema, tableName);
            try (ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    return rs.getString(2);
                }
            }
        } catch (SQLException e) {
            log.debug("Cannot get DDL for {} '{}'.'{}': {}", objectType, schema, tableName, e.getMessage());
        }
        return null;
    }
}
