package com.dma.core.infrastructure.ai;

import com.dma.core.domain.model.scanner.ScanResult;
import com.dma.core.domain.service.AiAdvisor;
import com.dma.core.infrastructure.config.AiConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.*;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class OpenAiCompatibleAdvisor implements AiAdvisor {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleAdvisor.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient;
    private final String dbPath;

    private volatile String apiUrl;
    private volatile String apiKey;
    private volatile String model;
    private volatile boolean available;
    private final String systemPrompt;

    public OpenAiCompatibleAdvisor(AiConfig config,
                                   @Value("${dma.database-path:${user.home}/.dma/dma.db}") String dbPath) {
        this.dbPath = dbPath;
        this.systemPrompt = config.getSystemPrompt();
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

        // Try DB settings first, fall back to application.yml
        if (!loadFromDb()) {
            loadFromConfig(config);
        }
        this.available = checkAvailability();
        log.info("AI Advisor initialized: model={}, url={}, available={}", model, apiUrl, available);
    }

    /** Load settings from SQLite. Returns true if non-default settings found. */
    private boolean loadFromDb() {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT provider, api_url, api_key, model FROM ai_settings WHERE id=1")) {
            if (rs.next() && !"noop".equals(rs.getString("provider"))) {
                this.apiUrl = rs.getString("api_url");
                this.apiKey = rs.getString("api_key");
                this.model = rs.getString("model");
                this.apiUrl = apiUrl.endsWith("/") ? apiUrl + "chat/completions" : apiUrl + "/chat/completions";
                log.info("AI config loaded from database: model={}", model);
                return true;
            }
        } catch (SQLException e) { log.debug("DB AI settings not available, using YML defaults"); }
        return false;
    }

    private void loadFromConfig(AiConfig config) {
        String url, key, mdl;
        switch (config.getProvider()) {
            case "openai" -> { url = config.getOpenai().getUrl(); key = config.getOpenai().getApiKey(); mdl = config.getOpenai().getModel(); }
            case "custom" -> { url = config.getCustom().getUrl(); key = config.getCustom().getApiKey(); mdl = config.getCustom().getModel(); }
            default -> { url = config.getOllama().getUrl(); key = ""; mdl = config.getOllama().getModel(); }
        }
        this.apiUrl = url.endsWith("/") ? url + "chat/completions" : url + "/chat/completions";
        this.apiKey = key;
        this.model = mdl;
    }

    /** Reload configuration from database (called after settings save). */
    public void reload() {
        loadFromDb();
        this.available = checkAvailability();
        log.info("AI config reloaded: model={}, available={}", model, available);
    }

    @Override
    public String generateAdvice(ScanResult result) {
        String prompt = String.format("""
                请分析以下数据库迁移兼容性问题，并给出具体的修改建议：

                规则代码: %s
                问题描述: %s
                原始SQL: %s
                建议SQL: %s
                严重程度: %s
                兼容性级别: %s

                请用中文回答，格式如下：
                1. 问题分析
                2. 修改建议
                3. 注意事项（如有）
                """,
                result.getRuleCode(),
                result.getMessage() != null ? result.getMessage() : "",
                result.getSourceSql() != null ? result.getSourceSql() : "",
                result.getSuggestedSql() != null ? result.getSuggestedSql() : "（无）",
                result.getSeverity(),
                result.getCompatibilityLevel()
        );
        return chat(prompt);
    }

    @Override
    public String generateMigrationPlan(List<ScanResult> issues) {
        if (issues.isEmpty()) return "未发现兼容性问题，无需迁移计划。";

        StringBuilder sb = new StringBuilder();
        sb.append("请根据以下数据库迁移兼容性问题清单，生成一份结构化的迁移计划：\n\n");
        sb.append("## 问题清单\n\n");
        for (int i = 0; i < Math.min(issues.size(), 20); i++) {
            ScanResult r = issues.get(i);
            sb.append(String.format("%d. [%s] %s — %s\n", i + 1,
                    r.getSeverity(), r.getRuleCode(), r.getMessage()));
        }
        if (issues.size() > 20) {
            sb.append(String.format("\n... 还有 %d 个问题\n", issues.size() - 20));
        }
        sb.append("""

                请用中文生成迁移计划，包含：
                1. 总体评估（风险等级、工作量预估）
                2. 按优先级排序的修复步骤
                3. 各阶段的验证方法
                4. 回滚方案建议
                """);
        return chat(sb.toString());
    }

    @Override
    public String convertWithAI(String sourceDdl, String sourceDb, String targetDb) {
        String prompt = String.format("""
                请将以下 %s 的数据库对象 DDL 转换为 %s 语法：

                ```sql
                %s
                ```

                要求：
                1. 保持原有逻辑不变
                2. 适配目标数据库的语法规范
                3. 说明关键修改点
                4. 标注潜在风险
                """, sourceDb, targetDb, sourceDdl);
        return chat(prompt);
    }

    @Override
    public boolean isAvailable() { return available; }

    @Override
    public String getModelName() { return model; }

    @Override
    public String getProviderInfo() {
        return String.format("%s (via %s)", model, available ? "✓ 已连接" : "✗ 未连接");
    }

    // === Private ===

    private String chat(String userMessage) {
        if (!available) return "⚠ AI 服务当前不可用。请确认：\n"
                + "1. 本地模型: 确认 Ollama 已启动 (ollama serve)\n"
                + "2. 云端模型: 确认 API Key 正确配置\n"
                + "3. 内网模型: 确认服务地址可访问";

        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userMessage)
                    ),
                    "temperature", 0.3,
                    "max_tokens", 2048
            );

            String json = mapper.writeValueAsString(body);
            var requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(120))
                    .POST(HttpRequest.BodyPublishers.ofString(json));

            if (apiKey != null && !apiKey.isBlank()) {
                requestBuilder.header("Authorization", "Bearer " + apiKey);
            }

            HttpResponse<String> resp = httpClient.send(requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() == 200) {
                // 解析 OpenAI 格式响应
                var node = mapper.readTree(resp.body());
                return node.path("choices").get(0)
                        .path("message").path("content").asText();
            } else {
                log.warn("AI API returned {}: {}", resp.statusCode(), resp.body());
                return "AI 服务返回错误 (HTTP " + resp.statusCode() + "): " + resp.body();
            }
        } catch (Exception e) {
            log.error("AI request failed", e);
            return "AI 请求失败: " + e.getMessage();
        }
    }

    private boolean checkAvailability() {
        try {
            var requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl.replace("/chat/completions", "/models")))
                    .timeout(Duration.ofSeconds(5))
                    .GET();

            if (apiKey != null && !apiKey.isBlank()) {
                requestBuilder.header("Authorization", "Bearer " + apiKey);
            }

            HttpResponse<String> resp = httpClient.send(requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString());
            return resp.statusCode() == 200;
        } catch (Exception e) {
            log.debug("AI availability check failed: {}", e.getMessage());
            return false;
        }
    }
}
