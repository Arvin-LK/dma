package com.dma.core.infrastructure.scanner;

import com.dma.common.enums.DatabaseType;
import com.dma.core.domain.model.scanner.ScanResult;
import com.dma.core.domain.model.scanner.ScanSource;
import com.dma.core.domain.service.SqlCompatibilityAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * SQL 文件扫描器。
 * 扫描 .sql 文件并按分号分割为独立语句进行分析。
 */
@Component
public class SqlFileScanner {

    private static final Logger log = LoggerFactory.getLogger(SqlFileScanner.class);
    private final SqlCompatibilityAnalyzer analyzer;
    private final ReservedKeywordDetector keywordDetector;

    public SqlFileScanner(SqlCompatibilityAnalyzer analyzer, ReservedKeywordDetector keywordDetector) {
        this.analyzer = analyzer;
        this.keywordDetector = keywordDetector;
    }

    public List<ScanResult> scan(Path projectRoot, DatabaseType source, DatabaseType target) {
        List<ScanResult> results = new ArrayList<>();
        try (Stream<Path> files = Files.walk(projectRoot)) {
            files.filter(p -> p.toString().endsWith(".sql"))
                 .filter(p -> !p.toString().contains("\\target\\"))
                 .forEach(sqlFile -> {
                     try {
                         String content = Files.readString(sqlFile);
                         String[] statements = content.split(";");
                         int lineOffset = 0;
                         for (String stmt : statements) {
                             String trimmed = stmt.trim();
                             if (!trimmed.isBlank() && !trimmed.startsWith("--")) {
                                 // 规则引擎分析
                                 List<ScanResult> scanResults = analyzer.analyze(trimmed, source, target);
                                 for (ScanResult r : scanResults) {
                                     r.setFilePath(sqlFile.toString());
                                     r.setLineNumber(lineOffset + 1);
                                     results.add(r);
                                 }
                                 // 关键词检测
                                 List<ScanResult> kwResults = keywordDetector.detect(trimmed, sqlFile.toString(), lineOffset + 1);
                                 results.addAll(kwResults);
                             }
                             lineOffset += stmt.split("\n").length;
                         }
                     } catch (IOException e) {
                         log.warn("Cannot read SQL file: {}", sqlFile, e);
                     }
                 });
        } catch (IOException e) {
            log.error("SQL file scan failed", e);
        }
        log.info("SQL file scan complete: {} issues in {}", results.size(), projectRoot);
        return results;
    }

    public Set<String> supportedExtensions() { return Set.of(".sql"); }
}
