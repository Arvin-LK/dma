package com.dma.idea.service;

import com.dma.common.enums.DatabaseType;
import com.dma.core.domain.model.scanner.ScanResult;
import com.dma.core.domain.service.SqlCompatibilityAnalyzer;
import com.dma.core.infrastructure.engine.DefaultSqlCompatibilityAnalyzer;
import com.dma.core.infrastructure.engine.JsonFileRuleLoader;
import com.dma.core.infrastructure.engine.RuleEngine;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * DMA 项目级服务 — 管理规则引擎实例和迁移配置。
 * 每个 IDEA 项目一个实例，配置独立持久化。
 */
@State(name = "DmaSettings", storages = {@Storage("dma-migration.xml")})
public class DmaProjectService implements PersistentStateComponent<DmaProjectService.State>,
                                          SqlCompatibilityAnalyzer {

    private final RuleEngine ruleEngine;
    private final DefaultSqlCompatibilityAnalyzer analyzer;
    private State state = new State();

    public DmaProjectService(Project project) {
        // Initialize rule engine from classpath
        this.ruleEngine = new RuleEngine(java.util.List.of(new JsonFileRuleLoader()));
        this.analyzer = new DefaultSqlCompatibilityAnalyzer(ruleEngine);
    }

    @Override
    public List<ScanResult> analyze(String sql, DatabaseType source, DatabaseType target) {
        return analyzer.analyze(sql, source, target);
    }

    public DatabaseType getSourceDb() {
        try { return DatabaseType.valueOf(state.sourceDbType); }
        catch (Exception e) { return DatabaseType.MYSQL; }
    }

    public DatabaseType getTargetDb() {
        try { return DatabaseType.valueOf(state.targetDbType); }
        catch (Exception e) { return DatabaseType.POSTGRESQL; }
    }

    public void setMigrationPath(String source, String target) {
        state.sourceDbType = source;
        state.targetDbType = target;
    }

    @Override
    public State getState() { return state; }

    @Override
    public void loadState(@NotNull State s) { this.state = s; }

    public static DmaProjectService getInstance(Project project) {
        return project.getService(DmaProjectService.class);
    }

    public static class State {
        public String sourceDbType = "MYSQL";
        public String targetDbType = "POSTGRESQL";
    }
}
