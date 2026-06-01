package com.dma.core.domain.model.rule;

import com.dma.common.enums.DatabaseType;
import com.dma.common.enums.PatternType;
import com.dma.common.enums.Severity;

/** 兼容性规则聚合根 */
public class MigrationRule {
    private RuleId id;
    private RuleCode code;
    private DatabaseType sourceDbType;
    private DatabaseType targetDbType;
    private String ruleName;
    private String category;
    private Severity severity;
    private PatternType patternType;
    private MatchPattern matchPattern;
    private ReplacementPattern replacementPattern;
    private String description;
    private String exampleSqlSource;
    private String exampleSqlTarget;
    private boolean enabled;

    public MigrationRule(RuleCode code, DatabaseType sourceDbType, DatabaseType targetDbType,
                         String ruleName, String category, Severity severity, PatternType patternType,
                         MatchPattern matchPattern, ReplacementPattern replacementPattern) {
        this.code = code; this.sourceDbType = sourceDbType; this.targetDbType = targetDbType;
        this.ruleName = ruleName; this.category = category; this.severity = severity;
        this.patternType = patternType; this.matchPattern = matchPattern;
        this.replacementPattern = replacementPattern; this.enabled = true;
    }

    public boolean matches(String sql) { return enabled && sql != null && sql.contains(matchPattern.expression()); }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean e) { this.enabled = e; }
    public RuleId getId() { return id; }
    public void setId(RuleId id) { this.id = id; }
    public RuleCode getCode() { return code; }
    public DatabaseType getSourceDbType() { return sourceDbType; }
    public DatabaseType getTargetDbType() { return targetDbType; }
    public String getRuleName() { return ruleName; }
    public String getCategory() { return category; }
    public Severity getSeverity() { return severity; }
    public PatternType getPatternType() { return patternType; }
    public MatchPattern getMatchPattern() { return matchPattern; }
    public ReplacementPattern getReplacementPattern() { return replacementPattern; }
    public String getDescription() { return description; }
    public void setDescription(String d) { this.description = d; }
    public String getExampleSqlSource() { return exampleSqlSource; }
    public void setExampleSqlSource(String s) { this.exampleSqlSource = s; }
    public String getExampleSqlTarget() { return exampleSqlTarget; }
    public void setExampleSqlTarget(String s) { this.exampleSqlTarget = s; }
}
