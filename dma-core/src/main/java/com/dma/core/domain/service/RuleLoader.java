package com.dma.core.domain.service;
import com.dma.common.enums.DatabaseType;
import com.dma.core.domain.model.rule.MigrationRule;
import java.util.List;

/** 领域服务：规则加载器 */
public interface RuleLoader {
    List<MigrationRule> load(DatabaseType source, DatabaseType target);
    default int getPriority() { return 100; }
    default boolean supportsHotReload() { return false; }
}
