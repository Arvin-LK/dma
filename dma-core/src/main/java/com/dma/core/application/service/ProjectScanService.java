package com.dma.core.application.service;

import com.dma.common.enums.DatabaseType;
import com.dma.core.domain.model.scanner.ScanResult;
import com.dma.core.domain.service.SqlCompatibilityAnalyzer;
import com.dma.core.infrastructure.scanner.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 项目源码扫描编排服务。
 * 统一调度所有扫描器，汇总结果并按风险分级。
 */
@Service
public class ProjectScanService {

    private static final Logger log = LoggerFactory.getLogger(ProjectScanService.class);

    private final JavaSourceScanner javaScanner;
    private final MyBatisXmlScanner xmlScanner;
    private final SqlFileScanner sqlFileScanner;
    private final ReservedKeywordDetector keywordDetector;

    public ProjectScanService(JavaSourceScanner javaScanner,
                               MyBatisXmlScanner xmlScanner,
                               SqlFileScanner sqlFileScanner,
                               ReservedKeywordDetector keywordDetector) {
        this.javaScanner = javaScanner;
        this.xmlScanner = xmlScanner;
        this.sqlFileScanner = sqlFileScanner;
        this.keywordDetector = keywordDetector;
    }

    /**
     * 项目扫描结果汇总。
     */
    public record ProjectScanSummary(
            String projectPath,
            String sourceDbType,
            String targetDbType,
            int totalFiles,
            int javaFiles,
            int xmlFiles,
            int sqlFiles,
            int totalIssues,
            int highRisk,
            int mediumRisk,
            int lowRisk,
            double riskScore,          // 0-100
            List<ScanResult> details
    ) {}

    /**
     * 执行完整的项目源码扫描。
     */
    public ProjectScanSummary scan(String projectPath, DatabaseType source, DatabaseType target) {
        Path root = Path.of(projectPath);
        log.info("Starting project scan: {} ({} → {})", projectPath, source, target);

        List<ScanResult> allResults = new ArrayList<>();

        // 统计文件数
        int javaCount = countFiles(root, ".java");
        int xmlCount = countFiles(root, ".xml");
        int sqlCount = countFiles(root, ".sql");

        // 1. Java 源码扫描
        try {
            List<ScanResult> javaResults = javaScanner.scan(root, source, target);
            allResults.addAll(javaResults);
            log.info("Java scan: {} issues in {} files", javaResults.size(), javaCount);
        } catch (Exception e) {
            log.warn("Java scan failed: {}", e.getMessage());
        }

        // 2. MyBatis Mapper XML 扫描
        try {
            List<ScanResult> xmlResults = xmlScanner.scan(root, source, target);
            allResults.addAll(xmlResults);
            log.info("XML scan: {} issues in {} files", xmlResults.size(), xmlCount);
        } catch (Exception e) {
            log.warn("XML scan failed: {}", e.getMessage());
        }

        // 3. SQL 文件扫描
        try {
            List<ScanResult> sqlResults = sqlFileScanner.scan(root, source, target);
            allResults.addAll(sqlResults);
            log.info("SQL scan: {} issues in {} files", sqlResults.size(), sqlCount);
        } catch (Exception e) {
            log.warn("SQL file scan failed: {}", e.getMessage());
        }

        // 4. 风险分级统计
        int highRisk = 0, mediumRisk = 0, lowRisk = 0;
        for (ScanResult r : allResults) {
            String sev = r.getSeverity();
            if ("ERROR".equals(sev)) highRisk++;
            else if ("WARNING".equals(sev)) mediumRisk++;
            else lowRisk++;
        }

        double riskScore = (highRisk * 20.0 + mediumRisk * 5.0 + lowRisk * 1.0);
        riskScore = Math.min(100, Math.round(riskScore * 10) / 10.0);

        int totalFiles = javaCount + xmlCount + sqlCount;

        log.info("Project scan complete: {} files, {} issues (H:{}, M:{}, L:{})",
                totalFiles, allResults.size(), highRisk, mediumRisk, lowRisk);

        return new ProjectScanSummary(
                projectPath, source.name(), target.name(),
                totalFiles, javaCount, xmlCount, sqlCount,
                allResults.size(), highRisk, mediumRisk, lowRisk,
                riskScore, allResults
        );
    }

    private int countFiles(Path root, String extension) {
        try {
            return (int) java.nio.file.Files.walk(root)
                    .filter(p -> p.toString().endsWith(extension))
                    .filter(p -> !p.toString().contains("\\target\\"))
                    .count();
        } catch (Exception e) {
            return 0;
        }
    }
}
