package com.dma.core.infrastructure.engine;

import com.dma.common.enums.DatabaseType;
import com.dma.core.domain.model.rule.MigrationRule;
import com.dma.core.domain.service.RuleLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 规则引擎核心。
 * 管理规则加载、缓存和查询。支持多 RuleLoader 合并加载。
 */
@Component
public class RuleEngine {

    private static final Logger log = LoggerFactory.getLogger(RuleEngine.class);

    private final List<RuleLoader> loaders;
    private final Map<String, List<MigrationRule>> cache = new ConcurrentHashMap<>();

    public RuleEngine(List<RuleLoader> loaders) {
        this.loaders = loaders;
        // 按优先级排序
        this.loaders.sort((a, b) -> Integer.compare(a.getPriority(), b.getPriority()));
        log.info("RuleEngine initialized with {} loader(s)", loaders.size());
    }

    /**
     * 加载指定源→目标的全部规则（合并所有 RuleLoader 的结果）。
     */
    public List<MigrationRule> loadRules(DatabaseType source, DatabaseType target) {
        String cacheKey = cacheKey(source, target);
        return cache.computeIfAbsent(cacheKey, k -> {
            List<MigrationRule> allRules = new ArrayList<>();
            for (RuleLoader loader : loaders) {
                try {
                    List<MigrationRule> rules = loader.load(source, target);
                    if (rules != null) allRules.addAll(rules);
                } catch (Exception e) {
                    log.warn("RuleLoader {} failed: {}", loader.getClass().getSimpleName(), e.getMessage());
                }
            }
            return allRules;
        });
    }

    /**
     * 获取所有已启用的规则。
     */
    public List<MigrationRule> getEnabledRules(DatabaseType source, DatabaseType target) {
        return loadRules(source, target).stream()
                .filter(MigrationRule::isEnabled)
                .toList();
    }

    /**
     * 清除指定路径的规则缓存。
     */
    public void clearCache(DatabaseType source, DatabaseType target) {
        cache.remove(cacheKey(source, target));
    }

    /**
     * 清除全部规则缓存。
     */
    public void clearAllCache() {
        cache.clear();
    }

    /**
     * 获取已加载的规则路径数量。
     */
    public int getCacheSize() {
        return cache.size();
    }

    private String cacheKey(DatabaseType source, DatabaseType target) {
        return source.name() + "->" + target.name();
    }
}
