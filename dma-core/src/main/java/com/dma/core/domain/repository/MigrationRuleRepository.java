package com.dma.core.domain.repository;
import com.dma.common.enums.DatabaseType;
import com.dma.core.domain.model.rule.MigrationRule;
import com.dma.core.domain.model.rule.RuleCode;
import java.util.List;

public interface MigrationRuleRepository {
    List<MigrationRule> findBySourceAndTarget(DatabaseType source, DatabaseType target);
    List<MigrationRule> findAllEnabled();
    void save(MigrationRule rule);
    void delete(RuleCode code);
}
