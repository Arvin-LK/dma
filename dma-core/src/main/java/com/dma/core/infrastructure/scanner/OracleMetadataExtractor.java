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
 * Oracle 数据库元数据提取器。
 * 使用 DBMS_METADATA.GET_DDL 系统包获取完整的 DDL 语句。
 */
@Component
public class OracleMetadataExtractor implements JdbcMetadataExtractor {

    private static final Logger log = LoggerFactory.getLogger(OracleMetadataExtractor.class);

    @Override
    public boolean supports(DatabaseType dbType) {
        return dbType == DatabaseType.ORACLE;
    }

    @Override
    public List<String> extractStoredProcedures(Connection conn, String schema) throws SQLException {
        List<String> procedures = new ArrayList<>();
        String effectiveSchema = schema != null ? schema.toUpperCase() : null;
        String sql = "SELECT OBJECT_NAME FROM ALL_OBJECTS WHERE OWNER=? AND OBJECT_TYPE='PROCEDURE' ORDER BY OBJECT_NAME";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, effectiveSchema);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("OBJECT_NAME");
                    String ddl = getOracleDdl(conn, effectiveSchema, name, "PROCEDURE");
                    if (ddl != null) procedures.add(ddl);
                }
            }
        }
        log.info("Oracle: extracted {} stored procedures from schema '{}'", procedures.size(), effectiveSchema);
        return procedures;
    }

    @Override
    public List<String> extractFunctions(Connection conn, String schema) throws SQLException {
        List<String> functions = new ArrayList<>();
        String effectiveSchema = schema != null ? schema.toUpperCase() : null;
        String sql = "SELECT OBJECT_NAME FROM ALL_OBJECTS WHERE OWNER=? AND OBJECT_TYPE='FUNCTION' ORDER BY OBJECT_NAME";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, effectiveSchema);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("OBJECT_NAME");
                    String ddl = getOracleDdl(conn, effectiveSchema, name, "FUNCTION");
                    if (ddl != null) functions.add(ddl);
                }
            }
        }
        log.info("Oracle: extracted {} functions from schema '{}'", functions.size(), effectiveSchema);
        return functions;
    }

    @Override
    public List<String> extractTableDDLs(Connection conn, String schema) throws SQLException {
        List<String> tables = new ArrayList<>();
        String effectiveSchema = schema != null ? schema.toUpperCase() : null;
        String sql = "SELECT TABLE_NAME FROM ALL_TABLES WHERE OWNER=? ORDER BY TABLE_NAME";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, effectiveSchema);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("TABLE_NAME");
                    String ddl = getOracleDdl(conn, effectiveSchema, name, "TABLE");
                    if (ddl != null) tables.add(ddl);
                }
            }
        }
        log.info("Oracle: extracted {} tables from schema '{}'", tables.size(), effectiveSchema);
        return tables;
    }

    @Override
    public List<String> extractViewDDLs(Connection conn, String schema) throws SQLException {
        List<String> views = new ArrayList<>();
        String effectiveSchema = schema != null ? schema.toUpperCase() : null;
        String sql = "SELECT VIEW_NAME FROM ALL_VIEWS WHERE OWNER=? ORDER BY VIEW_NAME";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, effectiveSchema);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("VIEW_NAME");
                    String ddl = getOracleDdl(conn, effectiveSchema, name, "VIEW");
                    if (ddl != null) views.add(ddl);
                }
            }
        }
        log.info("Oracle: extracted {} views from schema '{}'", views.size(), effectiveSchema);
        return views;
    }

    @Override
    public int getObjectCount(Connection conn, String schema) throws SQLException {
        String effectiveSchema = schema != null ? schema.toUpperCase() : null;
        String sql = "SELECT COUNT(*) FROM ALL_OBJECTS WHERE OWNER=? AND OBJECT_TYPE IN ('TABLE','VIEW','PROCEDURE','FUNCTION')";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, effectiveSchema);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    // === Private helpers ===

    /**
     * 通过 Oracle DBMS_METADATA.GET_DDL 系统包获取完整的 DDL 语句。
     * 相比 ALL_SOURCE 拼接，此方法返回带完整格式和约束的建表/建视图/建过程语句。
     */
    private String getOracleDdl(Connection conn, String schema, String objectName, String objectType) {
        // 先尝试 DBMS_METADATA.GET_DDL（推荐方式，返回完整 DDL）
        try (CallableStatement cs = conn.prepareCall(
                "BEGIN DBMS_METADATA.SET_TRANSFORM_PARAM(DBMS_METADATA.SESSION_TRANSFORM, 'SQLTERMINATOR', TRUE); END;")) {
            cs.execute();
        } catch (SQLException e) {
            log.debug("Cannot set DBMS_METADATA transform params: {}", e.getMessage());
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT DBMS_METADATA.GET_DDL(?, ?, ?) FROM DUAL")) {
            ps.setString(1, objectType);
            ps.setString(2, objectName);
            ps.setString(3, schema);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String ddl = rs.getString(1);
                    if (ddl != null && !ddl.isBlank()) {
                        return ddl.trim();
                    }
                }
            }
        } catch (SQLException e) {
            log.debug("DBMS_METADATA.GET_DDL failed for {} '{}': {}", objectType, objectName, e.getMessage());
        }

        // 回退方案：对 PROCEDURE/FUNCTION 使用 ALL_SOURCE 拼接
        if ("PROCEDURE".equals(objectType) || "FUNCTION".equals(objectType)) {
            return getSourceFromAllSource(conn, schema, objectName, objectType);
        }

        // 表和视图的回退方案：使用简单的骨架
        if ("TABLE".equals(objectType)) {
            return generateTableSkeleton(conn, schema, objectName);
        }

        return null;
    }

    /**
     * 回退方案：从 ALL_SOURCE 系统视图逐行拼接存储过程/函数源码。
     */
    private String getSourceFromAllSource(Connection conn, String schema, String name, String type) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE OR REPLACE ");
        String sql = "SELECT TEXT FROM ALL_SOURCE WHERE OWNER=? AND NAME=? AND TYPE=? ORDER BY LINE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, schema);
            ps.setString(2, name);
            ps.setString(3, type);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                sb.append(rs.getString("TEXT"));
                while (rs.next()) sb.append(rs.getString("TEXT"));
            }
        } catch (SQLException e) {
            log.debug("Cannot get source from ALL_SOURCE for {} '{}'", type, name);
            return null;
        }
        return sb.toString();
    }

    /**
     * 回退方案：通过数据字典生成表结构骨架。
     */
    private String generateTableSkeleton(Connection conn, String schema, String tableName) {
        StringBuilder ddl = new StringBuilder();
        ddl.append(String.format("CREATE TABLE %s.%s (\n", schema, tableName));

        // 从 ALL_TAB_COLUMNS 获取列信息
        String colSql = """
                SELECT COLUMN_NAME, DATA_TYPE, DATA_LENGTH, DATA_PRECISION, DATA_SCALE,
                       NULLABLE, DATA_DEFAULT, COLUMN_ID
                FROM ALL_TAB_COLUMNS
                WHERE OWNER = ? AND TABLE_NAME = ?
                ORDER BY COLUMN_ID
                """;
        List<String> columnDefs = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(colSql)) {
            ps.setString(1, schema);
            ps.setString(2, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String colName = rs.getString("COLUMN_NAME");
                    String dataType = rs.getString("DATA_TYPE");
                    int dataLen = rs.getInt("DATA_LENGTH");
                    int dataPrec = rs.getInt("DATA_PRECISION");
                    int dataScale = rs.getInt("DATA_SCALE");
                    String nullable = "Y".equals(rs.getString("NULLABLE")) ? "NULL" : "NOT NULL";
                    String defaultVal = rs.getString("DATA_DEFAULT");

                    StringBuilder colDef = new StringBuilder();
                    colDef.append(String.format("    %s %s", colName, dataType));

                    // 类型长度
                    if (!rs.wasNull()) {
                        if (dataPrec > 0) {
                            colDef.append(String.format("(%d,%d)", dataPrec, dataScale > 0 ? dataScale : 0));
                        } else if (dataLen > 0) {
                            colDef.append(String.format("(%d)", dataLen));
                        }
                    }

                    colDef.append(" ").append(nullable);
                    if (defaultVal != null) colDef.append(" DEFAULT ").append(defaultVal.trim());
                    columnDefs.add(colDef.toString());
                }
            }
        } catch (SQLException e) {
            log.debug("Cannot get columns for table '{}': {}", tableName, e.getMessage());
            return String.format("CREATE TABLE %s.%s (...); -- 无法获取列信息", schema, tableName);
        }

        ddl.append(String.join(",\n", columnDefs));

        // 获取主键
        try {
            String pkSql = """
                    SELECT cc.COLUMN_NAME
                    FROM ALL_CONSTRAINTS c
                    JOIN ALL_CONS_COLUMNS cc ON c.CONSTRAINT_NAME = cc.CONSTRAINT_NAME
                      AND c.OWNER = cc.OWNER
                    WHERE c.CONSTRAINT_TYPE = 'P'
                      AND c.OWNER = ?
                      AND c.TABLE_NAME = ?
                    ORDER BY cc.POSITION
                    """;
            try (PreparedStatement ps = conn.prepareStatement(pkSql)) {
                ps.setString(1, schema);
                ps.setString(2, tableName);
                try (ResultSet rs = ps.executeQuery()) {
                    List<String> pkCols = new ArrayList<>();
                    while (rs.next()) pkCols.add(rs.getString("COLUMN_NAME"));
                    if (!pkCols.isEmpty()) {
                        ddl.append(String.format(",\n    CONSTRAINT PK_%s PRIMARY KEY (%s)",
                                tableName, String.join(", ", pkCols)));
                    }
                }
            }
        } catch (SQLException e) {
            log.debug("Cannot get PK for table '{}': {}", tableName, e.getMessage());
        }

        ddl.append("\n);");
        return ddl.toString();
    }
}
