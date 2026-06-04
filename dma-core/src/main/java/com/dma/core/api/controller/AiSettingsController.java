package com.dma.core.api.controller;

import com.dma.common.dto.ApiResponse;
import com.dma.core.infrastructure.ai.OpenAiCompatibleAdvisor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI 配置管理 API。
 * 读写 SQLite 中的 ai_settings 表，支持页面内配置。
 */
@RestController
@RequestMapping("/api/v1/ai")
public class AiSettingsController {

    private static final Logger log = LoggerFactory.getLogger(AiSettingsController.class);
    private final String jdbcUrl;

    @Autowired(required = false)
    private OpenAiCompatibleAdvisor advisor;

    public AiSettingsController(@Value("${dma.database-path:${user.home}/.dma/dma.db}") String dbPath) {
        this.jdbcUrl = "jdbc:sqlite:" + dbPath;
    }

    /** 获取当前 AI 配置 */
    @GetMapping("/settings")
    public ApiResponse<Map<String, Object>> getSettings() {
        String sql = "SELECT provider, api_url, api_key, model, timeout_seconds FROM ai_settings WHERE id = 1";
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                Map<String, Object> settings = new LinkedHashMap<>();
                settings.put("provider", rs.getString("provider"));
                settings.put("apiUrl", rs.getString("api_url"));
                settings.put("apiKey", maskKey(rs.getString("api_key")));
                settings.put("model", rs.getString("model"));
                settings.put("timeout", rs.getInt("timeout_seconds"));
                return ApiResponse.success(settings);
            }
        } catch (SQLException e) {
            log.error("Failed to read AI settings", e);
        }
        return ApiResponse.success(Map.of("provider", "noop"));
    }

    /** 更新 AI 配置 */
    @PostMapping("/settings")
    public ApiResponse<String> saveSettings(@RequestBody Map<String, Object> body) {
        String provider = (String) body.getOrDefault("provider", "noop");
        String apiUrl = (String) body.getOrDefault("apiUrl", "http://localhost:11434/v1");
        String apiKey = (String) body.getOrDefault("apiKey", "");
        String model = (String) body.getOrDefault("model", "qwen2.5:7b");
        int timeout = body.containsKey("timeout") ? ((Number) body.get("timeout")).intValue() : 120;

        String sql = """
            UPDATE ai_settings SET provider=?, api_url=?, api_key=?, model=?, timeout_seconds=?,
            updated_at=datetime('now','localtime') WHERE id=1
            """;
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, provider);
            ps.setString(2, apiUrl);
            ps.setString(3, apiKey);
            ps.setString(4, model);
            ps.setInt(5, timeout);
            ps.executeUpdate();
            log.info("AI settings updated: provider={}, model={}", provider, model);
            // 触发 Advisor 重新加载配置
            if (advisor != null) advisor.reload();
            return ApiResponse.success("AI 配置已保存");
        } catch (SQLException e) {
            log.error("Failed to save AI settings", e);
            return ApiResponse.error(500, "保存失败: " + e.getMessage());
        }
    }

    /** 测试 AI 连接 */
    @PostMapping("/test")
    public ApiResponse<Map<String, Object>> testConnection(@RequestBody Map<String, Object> body) {
        String apiUrl = (String) body.get("apiUrl");
        String apiKey = (String) body.get("apiKey");

        Map<String, Object> result = new LinkedHashMap<>();
        try {
            java.net.URI uri = java.net.URI.create(apiUrl.endsWith("/") ? apiUrl + "models" : apiUrl + "/models");
            java.net.http.HttpRequest.Builder builder = java.net.http.HttpRequest.newBuilder()
                    .uri(uri).timeout(java.time.Duration.ofSeconds(5)).GET();

            if (apiKey != null && !apiKey.isBlank()) {
                builder.header("Authorization", "Bearer " + apiKey);
            }

            java.net.http.HttpResponse<String> resp = java.net.http.HttpClient.newHttpClient()
                    .send(builder.build(), java.net.http.HttpResponse.BodyHandlers.ofString());

            result.put("success", resp.statusCode() == 200);
            result.put("statusCode", resp.statusCode());
            result.put("message", resp.statusCode() == 200 ? "连接成功" : "HTTP " + resp.statusCode());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return ApiResponse.success(result);
    }

    private String maskKey(String key) {
        if (key == null || key.length() <= 8) return "***";
        return key.substring(0, 4) + "****" + key.substring(key.length() - 4);
    }
}
