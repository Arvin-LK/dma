package com.dma.core.infrastructure.converter;
import com.dma.core.domain.model.scanner.ScanResult;
import com.dma.core.domain.service.SqlConvertStrategy;
import org.springframework.stereotype.Component;
import java.util.Map;

/** Converts data type names between different databases */
@Component
public class DataTypeConverter implements SqlConvertStrategy {
    private static final Map<String, String> TYPE_MAP = Map.of(
        "DATETIME", "TIMESTAMP",
        "TINYINT", "SMALLINT",
        "MEDIUMTEXT", "TEXT",
        "BLOB", "BYTEA",
        "VARCHAR2", "VARCHAR",
        "NUMBER", "NUMERIC",
        "CLOB", "TEXT"
    );
    @Override
    public boolean supports(ScanResult issue) { return TYPE_MAP.keySet().stream().anyMatch(t -> issue.getSourceSql().toUpperCase().contains(t)); }
    @Override
    public String convert(String sql) {
        String result = sql;
        for (var entry : TYPE_MAP.entrySet()) {
            result = result.replaceAll("(?i)\\b" + entry.getKey() + "\\b", entry.getValue());
        }
        return result;
    }
}
