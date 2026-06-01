package com.dma.test.converter;

import com.dma.core.domain.model.scanner.ScanResult;
import com.dma.core.domain.model.scanner.ScanSource;
import com.dma.core.infrastructure.converter.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConverterTest {

    @Test
    void shouldConvertIfnullToCoalesce() {
        var converter = new FunctionNameConverter();
        String sql = "SELECT IFNULL(name, 'N/A') FROM users;";
        String result = converter.convert(sql);
        assertTrue(result.contains("COALESCE("));
        assertFalse(result.contains("IFNULL("));
    }

    @Test
    void shouldConvertLimitOffset() {
        var converter = new LimitClauseConverter();
        String sql = "SELECT * FROM users LIMIT 10, 20;";
        String result = converter.convert(sql);
        assertTrue(result.contains("LIMIT 20 OFFSET 10"));
    }

    @Test
    void shouldConvertBacktickToQuote() {
        var converter = new IdentifierQuoteConverter();
        String sql = "SELECT `id`, `name` FROM `users`;";
        String result = converter.convert(sql);
        assertFalse(result.contains("`"));
        assertTrue(result.contains("\""));
    }

    @Test
    void shouldConvertNvlToCoalesce() {
        var converter = new NvlToCoalesceConverter();
        String sql = "SELECT NVL(name, 'N/A') FROM users;";
        String result = converter.convert(sql);
        assertTrue(result.contains("COALESCE"));
        assertFalse(result.contains("NVL"));
    }

    @Test
    void shouldConvertDataType() {
        var converter = new DataTypeConverter();
        String sql = "CREATE TABLE t (created_at DATETIME, flag TINYINT);";
        String result = converter.convert(sql);
        assertTrue(result.contains("TIMESTAMP"));
        assertTrue(result.contains("SMALLINT"));
    }

    @Test
    void shouldConvertAutoIncrement() {
        var converter = new AutoIncrementConverter();
        String sql = "CREATE TABLE t (id INT AUTO_INCREMENT PRIMARY KEY);";
        String result = converter.convert(sql);
        assertTrue(result.contains("SERIAL"));
    }

    @Test
    void sqlConvertManagerShouldUseCorrectStrategy() {
        var manager = new SqlConvertManager(java.util.List.of(
            new FunctionNameConverter(), new LimitClauseConverter(),
            new IdentifierQuoteConverter(), new NvlToCoalesceConverter(),
            new DataTypeConverter(), new AutoIncrementConverter()
        ));
        assertEquals(6, manager.getStrategies().size());

        var issue = new ScanResult("M2PG-FN-001", "SELECT IFNULL(name, 'N/A') FROM users;",
            "AUTO_CONVERTIBLE", "WARNING", "test", ScanSource.MANUAL_INPUT);
        String result = manager.convert(issue.getSourceSql(), issue);
        assertTrue(result.contains("COALESCE"));
    }
}
