package com.dma.core.infrastructure.engine;

import com.dma.common.enums.CompatibilityLevel;
import com.dma.common.enums.DatabaseType;
import com.dma.core.domain.model.rule.MigrationRule;
import com.dma.core.domain.model.scanner.ScanResult;
import com.dma.core.domain.model.scanner.ScanSource;
import com.dma.core.domain.service.SqlCompatibilityAnalyzer;
import com.dma.common.util.SqlNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * SQL 兼容性分析器默认实现。
 * 基于规则引擎逐一匹配 SQL 并生成分析结果。
 */
@Component
public class DefaultSqlCompatibilityAnalyzer implements SqlCompatibilityAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(DefaultSqlCompatibilityAnalyzer.class);
    private final RuleEngine ruleEngine;

    public DefaultSqlCompatibilityAnalyzer(RuleEngine ruleEngine) {
        this.ruleEngine = ruleEngine;
    }

    @Override
    public List<ScanResult> analyze(String sql, DatabaseType source, DatabaseType target) {
        if (sql == null || sql.isBlank()) {
            return List.of();
        }

        String normalizedSql = SqlNormalizer.normalize(sql);
        List<MigrationRule> rules = ruleEngine.getEnabledRules(source, target);
        List<ScanResult> results = new ArrayList<>();

        log.debug("Analyzing SQL with {} rules: {} -> {}", rules.size(), source, target);

        for (MigrationRule rule : rules) {
            if (rule.matches(normalizedSql)) {
                ScanResult result = buildResult(rule, normalizedSql);
                results.add(result);
            }
        }

        // 如果没有匹配任何规则，认为完全兼容
        log.debug("Found {} compatibility issues", results.size());
        return results;
    }

    private ScanResult buildResult(MigrationRule rule, String sql) {
        String level = rule.getReplacementPattern() != null
                && !rule.getReplacementPattern().template().isEmpty()
                ? CompatibilityLevel.AUTO_CONVERTIBLE.name()
                : CompatibilityLevel.MANUAL_REVIEW.name();

        ScanResult result = new ScanResult(
                rule.getCode().value(),
                sql,
                level,
                rule.getSeverity().name(),
                rule.getDescription() != null ? rule.getDescription() : rule.getRuleName(),
                ScanSource.MANUAL_INPUT
        );
        return result;
    }
}
