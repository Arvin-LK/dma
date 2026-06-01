package com.dma.core.infrastructure.engine;

import com.dma.common.enums.DatabaseType;
import com.dma.common.enums.PatternType;
import com.dma.common.enums.Severity;
import com.dma.common.exception.RuleFormatException;
import com.dma.core.domain.model.rule.MatchPattern;
import com.dma.core.domain.model.rule.MigrationRule;
import com.dma.core.domain.model.rule.ReplacementPattern;
import com.dma.core.domain.model.rule.RuleCode;
import com.dma.core.domain.service.RuleLoader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 从 classpath:rules/*.json 加载内置规则。
 */
@Component
public class JsonFileRuleLoader implements RuleLoader {

    private static final Logger log = LoggerFactory.getLogger(JsonFileRuleLoader.class);
    private static final String RULES_LOCATION = "classpath:rules/*.json";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<MigrationRule> load(DatabaseType source, DatabaseType target) {
        List<MigrationRule> rules = new ArrayList<>();
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources(RULES_LOCATION);
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                String expectedName = source.name().toLowerCase() + "-"
                        + target.name().toLowerCase() + ".json";
                if (filename != null && filename.equalsIgnoreCase(expectedName)) {
                    rules.addAll(parseFile(resource));
                }
            }
        } catch (IOException e) {
            log.error("Failed to load rule files", e);
        }
        log.info("Loaded {} rules for {} -> {}", rules.size(), source, target);
        return rules;
    }

    private List<MigrationRule> parseFile(Resource resource) throws IOException {
        List<MigrationRule> rules = new ArrayList<>();
        JsonNode root = objectMapper.readTree(resource.getInputStream());
        JsonNode rulesNode = root.get("rules");
        if (rulesNode == null || !rulesNode.isArray()) {
            throw new RuleFormatException(resource.getFilename(), null);
        }
        for (JsonNode node : rulesNode) {
            rules.add(parseRule(node));
        }
        return rules;
    }

    private MigrationRule parseRule(JsonNode node) {
        DatabaseType sourceDb = DatabaseType.valueOf(node.get("sourceDbType").asText());
        DatabaseType targetDb = DatabaseType.valueOf(node.get("targetDbType").asText());
        var rule = new MigrationRule(
                new RuleCode(node.get("code").asText()),
                sourceDb, targetDb,
                node.get("name").asText(),
                node.get("category").asText(),
                Severity.valueOf(node.get("severity").asText()),
                PatternType.valueOf(node.get("patternType").asText()),
                new MatchPattern(node.get("matchPattern").asText()),
                node.has("replacementPattern") && !node.get("replacementPattern").isNull()
                        ? new ReplacementPattern(node.get("replacementPattern").asText())
                        : ReplacementPattern.EMPTY
        );
        if (node.has("description")) rule.setDescription(node.get("description").asText());
        if (node.has("exampleSqlSource")) rule.setExampleSqlSource(node.get("exampleSqlSource").asText());
        if (node.has("exampleSqlTarget")) rule.setExampleSqlTarget(node.get("exampleSqlTarget").asText());
        if (node.has("enabled")) rule.setEnabled(node.get("enabled").asBoolean());
        return rule;
    }

    @Override
    public int getPriority() { return 10; }
}
