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
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * 从 classpath 加载内置规则（支持 JAR 内和文件系统两种来源）。
 */
@Component
public class JsonFileRuleLoader implements RuleLoader {

    private static final Logger log = LoggerFactory.getLogger(JsonFileRuleLoader.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<MigrationRule> load(DatabaseType source, DatabaseType target) {
        List<MigrationRule> rules = new ArrayList<>();
        String expectedName = source.name().toLowerCase() + "-" + target.name().toLowerCase() + ".json";

        try {
            // 方法1: 从 classpath 搜索
            Enumeration<URL> resources = Thread.currentThread().getContextClassLoader()
                    .getResources("rules/");
            while (resources.hasMoreElements()) {
                URL dirUrl = resources.nextElement();
                try {
                    Path dirPath = Paths.get(dirUrl.toURI());
                    if (Files.isDirectory(dirPath)) {
                        try (var stream = Files.list(dirPath)) {
                            stream.filter(p -> p.getFileName().toString().equalsIgnoreCase(expectedName))
                                  .forEach(p -> {
                                      try (InputStream is = Files.newInputStream(p)) {
                                          rules.addAll(parseStream(is, expectedName));
                                      } catch (IOException e) { log.warn("Cannot read: {}", p, e); }
                                  });
                        }
                    }
                } catch (Exception e) {
                    // JAR 内的资源：尝试直接加载
                    URL fileUrl = Thread.currentThread().getContextClassLoader()
                            .getResource("rules/" + expectedName);
                    if (fileUrl != null) {
                        try (InputStream is = fileUrl.openStream()) {
                            rules.addAll(parseStream(is, expectedName));
                        } catch (IOException ex) { log.warn("Cannot load from JAR: {}", fileUrl, ex); }
                    }
                }
            }
        } catch (IOException e) {
            log.error("Failed to load rule files", e);
        }

        if (rules.isEmpty()) {
            log.warn("No rules loaded for {} -> {} (expected file: {})", source, target, expectedName);
        } else {
            log.info("Loaded {} rules for {} -> {}", rules.size(), source, target);
        }
        return rules;
    }

    private List<MigrationRule> parseStream(InputStream is, String filename) throws IOException {
        List<MigrationRule> rules = new ArrayList<>();
        JsonNode root = objectMapper.readTree(is);
        JsonNode rulesNode = root.get("rules");
        if (rulesNode == null || !rulesNode.isArray()) {
            throw new RuleFormatException(filename, null);
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
