package com.dma.core.application.service;

import com.dma.common.dto.DatabaseObjectInfo;
import com.dma.common.dto.ScanSummary;
import com.dma.common.enums.CompatibilityLevel;
import com.dma.common.enums.DatabaseType;
import com.dma.core.domain.model.connection.DatabaseConnection;
import com.dma.core.domain.model.scanner.ScanResult;
import com.dma.core.domain.model.scanner.ScanSource;
import com.dma.core.domain.service.JdbcMetadataExtractor;
import com.dma.core.domain.service.SqlCompatibilityAnalyzer;
import com.dma.core.domain.service.SqlConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据库扫描编排服务。
 * 连接源库 → 提取元数据 → 兼容性分析 → 汇总统计。
 */
@Service
public class DatabaseScanService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseScanService.class);

    private final List<JdbcMetadataExtractor> extractors;
    private final SqlCompatibilityAnalyzer analyzer;
    private final SqlConverter converter;

    public DatabaseScanService(List<JdbcMetadataExtractor> extractors,
                                SqlCompatibilityAnalyzer analyzer,
                                SqlConverter converter) {
        this.extractors = extractors;
        this.analyzer = analyzer;
        this.converter = converter;
        log.info("DatabaseScanService initialized with {} extractor(s)", extractors.size());
    }

    /**
     * 执行完整的数据扫描。
     *
     * @param connection 数据库连接信息
     * @param targetDb   目标数据库类型
     * @return 扫描汇总结果
     */
    public ScanSummary scan(DatabaseConnection connection, DatabaseType targetDb) throws SQLException {
        DatabaseType sourceDb = connection.getDbType();
        String schema = connection.getDatabaseName();
        ScanSummary summary = new ScanSummary(sourceDb.name(), targetDb.name(), schema);

        // 1. 获取对应源库的元数据提取器
        JdbcMetadataExtractor extractor = extractors.stream()
                .filter(e -> e.supports(sourceDb))
                .findFirst()
                .orElseThrow(() -> new UnsupportedOperationException(
                        "不支持的源数据库类型: " + sourceDb));

        // 2. 建立 JDBC 连接
        String jdbcUrl = buildJdbcUrl(sourceDb, connection.getHost(), connection.getPort(), schema);
        log.info("Connecting to source database: {}", jdbcUrl);
        try (Connection conn = DriverManager.getConnection(jdbcUrl,
                connection.getUsername(), connection.getEncryptedPassword())) {

            // 3. 提取各类对象
            List<String> procedures = safeExtract(() -> extractor.extractStoredProcedures(conn, schema));
            List<String> functions = safeExtract(() -> extractor.extractFunctions(conn, schema));
            List<String> tables = safeExtract(() -> extractor.extractTableDDLs(conn, schema));
            List<String> views = safeExtract(() -> extractor.extractViewDDLs(conn, schema));

            summary.setStoredProcedureCount(procedures.size());
            summary.setFunctionCount(functions.size());
            summary.setTableCount(tables.size());
            summary.setViewCount(views.size());

            // 4. 对每个对象执行兼容性分析
            analyzeObjects(summary, procedures, "PROCEDURE", sourceDb, targetDb);
            analyzeObjects(summary, functions, "FUNCTION", sourceDb, targetDb);
            analyzeObjects(summary, tables, "TABLE", sourceDb, targetDb);
            analyzeObjects(summary, views, "VIEW", sourceDb, targetDb);
        }

        // 5. 计算兼容率
        summary.calculateRate();
        summary.setTotalIssues(summary.getManualReviewCount() + summary.getIncompatibleCount());

        log.info("Scan complete: {} objects, compatibility rate: {}%",
                summary.getTotalObjects(), summary.getCompatibilityRate());
        return summary;
    }

    // === Private helpers ===

    private void analyzeObjects(ScanSummary summary, List<String> objects, String objectType,
                                 DatabaseType source, DatabaseType target) {
        for (int i = 0; i < objects.size(); i++) {
            String ddl = objects.get(i);
            String objectName = extractObjectName(ddl, objectType);

            DatabaseObjectInfo info = new DatabaseObjectInfo(objectName, objectType, ddl);
            info.setCompatibilityLevel(CompatibilityLevel.COMPATIBLE.name());

            try {
                List<ScanResult> results = analyzer.analyze(ddl, source, target);
                if (results.isEmpty()) {
                    summary.setCompatibleCount(summary.getCompatibleCount() + 1);
                } else {
                    // 取最严重的级别
                    boolean hasError = false, hasWarning = false;
                    for (ScanResult r : results) {
                        info.getIssues().add(r.getMessage());
                        if ("MANUAL_REVIEW".equals(r.getCompatibilityLevel())
                                || "INCOMPATIBLE".equals(r.getCompatibilityLevel())) {
                            hasError = true;
                            info.setRuleCode(r.getRuleCode());
                            info.setDescription(r.getMessage());
                        } else if ("AUTO_CONVERTIBLE".equals(r.getCompatibilityLevel())) {
                            hasWarning = true;
                            String suggested = converter.convertSingle(r).getSuggestedSql();
                            if (info.getSuggestedDdl() == null) info.setSuggestedDdl(suggested);
                        }
                    }
                    if (hasError) {
                        info.setCompatibilityLevel(CompatibilityLevel.MANUAL_REVIEW.name());
                        info.setSeverity("ERROR");
                        summary.setManualReviewCount(summary.getManualReviewCount() + 1);
                    } else if (hasWarning) {
                        info.setCompatibilityLevel(CompatibilityLevel.AUTO_CONVERTIBLE.name());
                        info.setSeverity("WARNING");
                        summary.setAutoConvertibleCount(summary.getAutoConvertibleCount() + 1);
                    } else {
                        summary.setCompatibleCount(summary.getCompatibleCount() + 1);
                    }
                }
            } catch (Exception e) {
                log.debug("Analysis failed for {} '{}': {}", objectType, objectName, e.getMessage());
                info.setCompatibilityLevel(CompatibilityLevel.PARSE_ERROR.name());
                summary.setIncompatibleCount(summary.getIncompatibleCount() + 1);
            }
            summary.addObject(info);
        }
    }

    private String buildJdbcUrl(DatabaseType dbType, String host, int port, String database) {
        return switch (dbType) {
            case MYSQL -> String.format("jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC", host, port, database);
            case ORACLE -> String.format("jdbc:oracle:thin:@%s:%d:%s", host, port, database);
            case SQLSERVER -> String.format("jdbc:sqlserver://%s:%d;databaseName=%s;encrypt=false;trustServerCertificate=true", host, port, database);
            case POSTGRESQL, GAUSSDB -> String.format("jdbc:postgresql://%s:%d/%s", host, port, database);
            case DAMENG -> String.format("jdbc:dm://%s:%d/%s", host, port, database);
            case OCEANBASE, GOLDENDB -> String.format("jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC", host, port, database);
        };
    }

    private String extractObjectName(String ddl, String objectType) {
        try {
            String upper = ddl.toUpperCase();
            if (objectType.equals("PROCEDURE")) {
                int idx = upper.indexOf("PROCEDURE ");
                if (idx >= 0) {
                    String rest = ddl.substring(idx + 10).trim();
                    int end = rest.indexOf('(');
                    return end > 0 ? rest.substring(0, end) : rest.split("\\s+")[0];
                }
            } else if (objectType.equals("FUNCTION")) {
                int idx = upper.indexOf("FUNCTION ");
                if (idx >= 0) {
                    String rest = ddl.substring(idx + 9).trim();
                    int end = rest.indexOf('(');
                    return end > 0 ? rest.substring(0, end) : rest.split("\\s+")[0];
                }
            }
            // TABLE/VIEW: extract from CREATE TABLE/VIEW xxx
            String[] words = ddl.trim().split("\\s+");
            if (words.length >= 3) return words[2].replace("`", "").replace("(", "");
        } catch (Exception e) {
            // fallback
        }
        return objectType + "_" + ddl.hashCode();
    }

    @FunctionalInterface
    private interface SqlSupplier<T> {
        T get() throws SQLException;
    }

    private <T> List<T> safeExtract(SqlSupplier<List<T>> supplier) {
        try {
            return supplier.get();
        } catch (SQLException e) {
            log.warn("Failed to extract objects: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 发现/列出源数据库中可用的 schema（数据库）列表。
     */
    public List<String> discoverSchemas(DatabaseType dbType, String host, int port,
                                         String username, String password) throws SQLException {
        String jdbcUrl = buildJdbcUrl(dbType, host, port, "");
        log.info("Discovering schemas: {}", jdbcUrl);
        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
            return switch (dbType) {
                case MYSQL -> {
                    List<String> schemas = new ArrayList<>();
                    try (Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery("SHOW DATABASES")) {
                        while (rs.next()) {
                            String name = rs.getString(1);
                            // 过滤系统库
                            if (!name.equalsIgnoreCase("information_schema")
                                    && !name.equalsIgnoreCase("performance_schema")
                                    && !name.equalsIgnoreCase("mysql")
                                    && !name.equalsIgnoreCase("sys")) {
                                schemas.add(name);
                            }
                        }
                    }
                    yield schemas;
                }
                case ORACLE -> {
                    List<String> schemas = new ArrayList<>();
                    try (Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery(
                             "SELECT USERNAME FROM ALL_USERS ORDER BY USERNAME")) {
                        while (rs.next()) schemas.add(rs.getString(1));
                    }
                    yield schemas;
                }
                case POSTGRESQL -> {
                    List<String> schemas = new ArrayList<>();
                    try (Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery(
                             "SELECT schema_name FROM information_schema.schemata " +
                             "WHERE schema_name NOT IN ('pg_catalog','information_schema') " +
                             "ORDER BY schema_name")) {
                        while (rs.next()) schemas.add(rs.getString(1));
                    }
                    yield schemas;
                }
                case SQLSERVER -> {
                    List<String> schemas = new ArrayList<>();
                    try (Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery(
                             "SELECT name FROM sys.schemas WHERE name NOT IN ('sys','INFORMATION_SCHEMA','guest','db_owner','db_accessadmin','db_securityadmin','db_ddladmin','db_backupoperator','db_datareader','db_datawriter','db_denydatareader','db_denydatawriter') ORDER BY name")) {
                        while (rs.next()) schemas.add(rs.getString(1));
                    }
                    yield schemas;
                }
                default -> {
                    // 其他数据库尝试用 JDBC metadata
                    List<String> schemas = new ArrayList<>();
                    var meta = conn.getMetaData();
                    try (ResultSet rs = meta.getCatalogs()) {
                        while (rs.next()) schemas.add(rs.getString("TABLE_CAT"));
                    }
                    yield schemas;
                }
            };
        }
    }
}
