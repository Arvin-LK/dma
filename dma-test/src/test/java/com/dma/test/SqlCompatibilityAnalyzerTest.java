package com.dma.test;

import com.dma.common.enums.DatabaseType;
import com.dma.core.domain.model.scanner.ScanResult;
import com.dma.core.domain.service.SqlCompatibilityAnalyzer;
import com.dma.core.infrastructure.engine.DefaultSqlCompatibilityAnalyzer;
import com.dma.core.infrastructure.engine.JsonFileRuleLoader;
import com.dma.core.infrastructure.engine.RuleEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SqlCompatibilityAnalyzerTest {

    private SqlCompatibilityAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        var engine = new RuleEngine(List.of(new JsonFileRuleLoader()));
        analyzer = new DefaultSqlCompatibilityAnalyzer(engine);
    }

    @Test
    void shouldDetectIfnullInMySql() {
        String sql = "SELECT IFNULL(name, 'N/A') FROM users;";
        List<ScanResult> results = analyzer.analyze(sql, DatabaseType.MYSQL, DatabaseType.POSTGRESQL);
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(r -> r.getRuleCode().equals("M2PG-FN-001")));
    }

    @Test
    void shouldDetectLimitOffset() {
        String sql = "SELECT * FROM users LIMIT 10, 20;";
        List<ScanResult> results = analyzer.analyze(sql, DatabaseType.MYSQL, DatabaseType.POSTGRESQL);
        assertTrue(results.stream().anyMatch(r -> r.getRuleCode().equals("M2PG-SYN-001")));
    }

    @Test
    void shouldDetectBacktickQuote() {
        String sql = "SELECT `id`, `name` FROM `users`;";
        List<ScanResult> results = analyzer.analyze(sql, DatabaseType.MYSQL, DatabaseType.POSTGRESQL);
        assertTrue(results.stream().anyMatch(r -> r.getRuleCode().equals("M2PG-SYN-002")));
    }

    @Test
    void shouldDetectAutoIncrement() {
        String sql = "CREATE TABLE t (id INT AUTO_INCREMENT PRIMARY KEY);";
        List<ScanResult> results = analyzer.analyze(sql, DatabaseType.MYSQL, DatabaseType.POSTGRESQL);
        assertTrue(results.stream().anyMatch(r -> r.getRuleCode().equals("M2PG-SYN-003")));
    }

    @Test
    void shouldReturnNoErrorForSimpleSelect() {
        String sql = "SELECT 1";
        List<ScanResult> results = analyzer.analyze(sql, DatabaseType.MYSQL, DatabaseType.POSTGRESQL);
        assertTrue(results.stream().noneMatch(r -> "ERROR".equals(r.getSeverity())),
                "Simple SELECT 1 should not have ERROR issues");
    }

    @Test
    void shouldReturnEmptyForBlankSql() {
        assertTrue(analyzer.analyze("", DatabaseType.MYSQL, DatabaseType.POSTGRESQL).isEmpty());
        assertTrue(analyzer.analyze(null, DatabaseType.MYSQL, DatabaseType.POSTGRESQL).isEmpty());
    }

    @Test
    void shouldDetectNvlInOracle() {
        String sql = "SELECT NVL(name, 'N/A') FROM users;";
        List<ScanResult> results = analyzer.analyze(sql, DatabaseType.ORACLE, DatabaseType.POSTGRESQL);
        assertTrue(results.stream().anyMatch(r -> r.getRuleCode().equals("O2PG-FN-001")));
    }
}
