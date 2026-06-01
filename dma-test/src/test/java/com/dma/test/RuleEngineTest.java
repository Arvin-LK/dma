package com.dma.test;

import com.dma.common.enums.DatabaseType;
import com.dma.core.domain.model.rule.MigrationRule;
import com.dma.core.infrastructure.engine.JsonFileRuleLoader;
import com.dma.core.infrastructure.engine.RuleEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RuleEngineTest {

    private RuleEngine engine;

    @BeforeEach
    void setUp() {
        engine = new RuleEngine(List.of(new JsonFileRuleLoader()));
    }

    @Test
    void shouldLoadMySqlToPostgresRules() {
        List<MigrationRule> rules = engine.loadRules(DatabaseType.MYSQL, DatabaseType.POSTGRESQL);
        assertNotNull(rules);
        assertTrue(rules.size() >= 30, "Should have at least 30 rules, but found: " + rules.size());
    }

    @Test
    void shouldLoadOracleToPostgresRules() {
        List<MigrationRule> rules = engine.loadRules(DatabaseType.ORACLE, DatabaseType.POSTGRESQL);
        assertNotNull(rules);
        assertTrue(rules.size() >= 25, "Should have at least 25 rules, but found: " + rules.size());
    }

    @Test
    void shouldLoadMySqlToDamengRules() {
        List<MigrationRule> rules = engine.loadRules(DatabaseType.MYSQL, DatabaseType.DAMENG);
        assertNotNull(rules);
        assertTrue(rules.size() >= 25, "Should have at least 25 rules, but found: " + rules.size());
    }

    @Test
    void shouldFilterEnabledRules() {
        List<MigrationRule> rules = engine.getEnabledRules(DatabaseType.MYSQL, DatabaseType.POSTGRESQL);
        assertNotNull(rules);
        assertTrue(rules.stream().allMatch(MigrationRule::isEnabled));
    }

    @Test
    void shouldCacheRules() {
        engine.loadRules(DatabaseType.MYSQL, DatabaseType.POSTGRESQL);
        assertEquals(1, engine.getCacheSize());
        engine.loadRules(DatabaseType.MYSQL, DatabaseType.POSTGRESQL);
        assertEquals(1, engine.getCacheSize()); // Should still be 1 (cached)
    }
}
