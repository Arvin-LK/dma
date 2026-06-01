package com.dma.core.infrastructure.converter;
import com.dma.core.domain.model.scanner.ScanResult;
import com.dma.core.domain.service.SqlConvertStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.List;

/** Manages SQL conversion strategies in a chain-of-responsibility pattern */
@Component
public class SqlConvertManager {
    private static final Logger log = LoggerFactory.getLogger(SqlConvertManager.class);
    private final List<SqlConvertStrategy> strategies;

    public SqlConvertManager(List<SqlConvertStrategy> strategies) {
        this.strategies = strategies;
        log.info("SqlConvertManager initialized with {} strategies", strategies.size());
    }

    public String convert(String sql, ScanResult issue) {
        for (SqlConvertStrategy strategy : strategies) {
            if (strategy.supports(issue)) {
                log.debug("Using strategy {} for rule {}", strategy.getName(), issue.getRuleCode());
                return strategy.convert(sql);
            }
        }
        log.debug("No converter found for rule {}, returning original SQL", issue.getRuleCode());
        return sql;
    }

    public List<SqlConvertStrategy> getStrategies() { return strategies; }
}
