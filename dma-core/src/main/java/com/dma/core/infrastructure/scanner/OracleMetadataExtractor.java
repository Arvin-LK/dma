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
 * Oracle 数据库元数据提取器（骨架实现）。
 * 查询 ALL_OBJECTS / ALL_SOURCE 系统视图获取对象信息。
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
        String sql = "SELECT OBJECT_NAME FROM ALL_OBJECTS WHERE OWNER=? AND OBJECT_TYPE='PROCEDURE'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, schema.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("OBJECT_NAME");
                    String ddl = getOracleSource(conn, schema, name, "PROCEDURE");
                    if (ddl != null) procedures.add(ddl);
                }
            }
        }
        log.info("Oracle: extracted {} stored procedures", procedures.size());
        return procedures;
    }

    @Override
    public List<String> extractFunctions(Connection conn, String schema) throws SQLException {
        List<String> functions = new ArrayList<>();
        String sql = "SELECT OBJECT_NAME FROM ALL_OBJECTS WHERE OWNER=? AND OBJECT_TYPE='FUNCTION'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, schema.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("OBJECT_NAME");
                    String ddl = getOracleSource(conn, schema, name, "FUNCTION");
                    if (ddl != null) functions.add(ddl);
                }
            }
        }
        log.info("Oracle: extracted {} functions", functions.size());
        return functions;
    }

    @Override
    public List<String> extractTableDDLs(Connection conn, String schema) throws SQLException {
        List<String> tables = new ArrayList<>();
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getTables(null, schema.toUpperCase(), "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                String name = rs.getString("TABLE_NAME");
                tables.add("CREATE TABLE " + schema + "." + name + " (...); -- 需 DBMS_METADATA 获取完整 DDL");
            }
        }
        log.info("Oracle: extracted {} tables", tables.size());
        return tables;
    }

    @Override
    public List<String> extractViewDDLs(Connection conn, String schema) throws SQLException {
        List<String> views = new ArrayList<>();
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getTables(null, schema.toUpperCase(), "%", new String[]{"VIEW"})) {
            while (rs.next()) {
                String name = rs.getString("TABLE_NAME");
                views.add("CREATE VIEW " + schema + "." + name + " AS ...; -- 需 DBMS_METADATA 获取完整 DDL");
            }
        }
        log.info("Oracle: extracted {} views", views.size());
        return views;
    }

    @Override
    public int getObjectCount(Connection conn, String schema) throws SQLException {
        String sql = "SELECT COUNT(*) FROM ALL_OBJECTS WHERE OWNER=? AND OBJECT_TYPE IN ('TABLE','VIEW','PROCEDURE','FUNCTION')";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, schema.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private String getOracleSource(Connection conn, String schema, String name, String type) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE OR REPLACE ").append(type).append(" ").append(schema).append(".").append(name);
        if ("FUNCTION".equals(type)) sb.append(" RETURN VARCHAR2");
        sb.append(" AS\n");
        String sql = "SELECT TEXT FROM ALL_SOURCE WHERE OWNER=? AND NAME=? AND TYPE=? ORDER BY LINE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, schema.toUpperCase());
            ps.setString(2, name);
            ps.setString(3, type);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) sb.append(rs.getString("TEXT"));
            }
        } catch (SQLException e) {
            log.debug("Cannot get source for {} '{}'", type, name);
            return null;
        }
        sb.append("\nEND;");
        return sb.toString();
    }
}
