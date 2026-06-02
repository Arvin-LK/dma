package com.dma.core.api.controller;

import com.dma.common.dto.ApiResponse;
import com.dma.common.enums.DatabaseType;
import com.dma.core.domain.model.scanner.ScanResult;
import com.dma.core.domain.model.scanner.ScanSource;
import com.dma.core.domain.service.AiAdvisor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 迁移顾问 API。
 */
@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    private static final Logger log = LoggerFactory.getLogger(AiController.class);
    private final AiAdvisor advisor;

    public AiController(AiAdvisor advisor) {
        this.advisor = advisor;
    }

    /** 检查 AI 服务状态 */
    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> status() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("available", advisor.isAvailable());
        info.put("model", advisor.getModelName());
        info.put("providerInfo", advisor.getProviderInfo());
        return ApiResponse.success(info);
    }

    /** AI 分析单条 SQL 兼容性问题 */
    @PostMapping("/advice")
    public ApiResponse<String> getAdvice(@RequestBody Map<String, Object> body) {
        try {
            ScanResult result = new ScanResult(
                    (String) body.getOrDefault("ruleCode", ""),
                    (String) body.getOrDefault("sourceSql", ""),
                    (String) body.getOrDefault("compatibilityLevel", "MANUAL_REVIEW"),
                    (String) body.getOrDefault("severity", "WARNING"),
                    (String) body.getOrDefault("message", ""),
                    ScanSource.MANUAL_INPUT
            );
            result.setSuggestedSql((String) body.getOrDefault("suggestedSql", ""));
            String advice = advisor.generateAdvice(result);
            return ApiResponse.success(advice);
        } catch (Exception e) {
            return ApiResponse.error(500, "AI 分析失败: " + e.getMessage());
        }
    }

    /** AI 生成迁移计划 */
    @PostMapping("/plan")
    public ApiResponse<String> getPlan(@RequestBody Map<String, Object> body) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> issues = (List<Map<String, Object>>) body.getOrDefault("issues", List.of());
            List<ScanResult> results = new ArrayList<>();
            for (Map<String, Object> m : issues) {
                ScanResult r = new ScanResult(
                        (String) m.getOrDefault("ruleCode", ""),
                        (String) m.getOrDefault("sourceSql", ""),
                        (String) m.getOrDefault("compatibilityLevel", ""),
                        (String) m.getOrDefault("severity", ""),
                        (String) m.getOrDefault("message", ""),
                        ScanSource.MANUAL_INPUT
                );
                results.add(r);
            }
            String plan = advisor.generateMigrationPlan(results);
            return ApiResponse.success(plan);
        } catch (Exception e) {
            return ApiResponse.error(500, "AI 计划生成失败: " + e.getMessage());
        }
    }

    /** AI 辅助转换 DDL */
    @PostMapping("/convert")
    public ApiResponse<String> aiConvert(@RequestBody Map<String, Object> body) {
        try {
            String ddl = (String) body.get("ddl");
            String sourceDb = (String) body.getOrDefault("sourceDbType", "MYSQL");
            String targetDb = (String) body.getOrDefault("targetDbType", "POSTGRESQL");
            String result = advisor.convertWithAI(ddl, sourceDb, targetDb);
            return ApiResponse.success(result);
        } catch (Exception e) {
            return ApiResponse.error(500, "AI 转换失败: " + e.getMessage());
        }
    }
}
