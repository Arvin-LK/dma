package com.dma.idea.service;

import com.dma.common.enums.DatabaseType;
import com.dma.core.domain.model.scanner.ScanResult;
import com.dma.core.domain.service.SqlCompatibilityAnalyzer;
import com.dma.core.infrastructure.engine.DefaultSqlCompatibilityAnalyzer;
import com.dma.core.infrastructure.engine.JsonFileRuleLoader;
import com.dma.core.infrastructure.engine.RuleEngine;

import java.util.List;

/**
 * DMA 插件全局服务 — 持有规则引擎单例。
 */
public class DmaPluginService {

    private static volatile DmaPluginService instance;
    private final SqlCompatibilityAnalyzer analyzer;

    public DmaPluginService() {
        RuleEngine engine = new RuleEngine(List.of(new JsonFileRuleLoader()));
        this.analyzer = new DefaultSqlCompatibilityAnalyzer(engine);
    }

    public static DmaPluginService getInstance() {
        if (instance == null) {
            synchronized (DmaPluginService.class) {
                if (instance == null) instance = new DmaPluginService();
            }
        }
        return instance;
    }

    public List<ScanResult> analyze(String sql) {
        return analyzer.analyze(sql, DatabaseType.MYSQL, DatabaseType.POSTGRESQL);
    }
}
