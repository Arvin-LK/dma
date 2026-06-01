package com.dma.core.infrastructure.converter;
import com.dma.core.domain.model.scanner.ScanResult;
import com.dma.core.domain.service.SqlConverter;
import org.springframework.stereotype.Component;
import java.util.List;

/** Default implementation of SqlConverter domain service */
@Component
public class DefaultSqlConverter implements SqlConverter {
    private final SqlConvertManager manager;
    public DefaultSqlConverter(SqlConvertManager manager) { this.manager = manager; }

    @Override
    public String convert(String sql, List<ScanResult> issues) {
        String result = sql;
        for (ScanResult issue : issues) {
            if ("AUTO_CONVERTIBLE".equals(issue.getCompatibilityLevel())) {
                result = manager.convert(result, issue);
            }
        }
        return result;
    }

    @Override
    public ScanResult convertSingle(ScanResult issue) {
        String converted = manager.convert(issue.getSourceSql(), issue);
        issue.setSuggestedSql(converted);
        return issue;
    }
}
