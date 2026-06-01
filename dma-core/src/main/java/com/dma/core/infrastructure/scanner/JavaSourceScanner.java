package com.dma.core.infrastructure.scanner;
import com.dma.common.enums.DatabaseType;
import com.dma.core.domain.model.scanner.ScanResult;
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
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** Scans .java files for SQL string literals */
@Component
public class JavaSourceScanner {
    private static final Logger log = LoggerFactory.getLogger(JavaSourceScanner.class);
    private static final Pattern SQL_PATTERN = Pattern.compile(
        "(?i)(SELECT\\s|INSERT\\s+INTO|UPDATE\\s+\\w+|DELETE\\s+FROM|CREATE\\s+TABLE|ALTER\\s+TABLE|DROP\\s+TABLE)",
        Pattern.DOTALL);
    private final SqlCompatibilityAnalyzer analyzer;

    public JavaSourceScanner(SqlCompatibilityAnalyzer analyzer) { this.analyzer = analyzer; }

    public List<ScanResult> scan(Path projectRoot, DatabaseType source, DatabaseType target) {
        List<ScanResult> results = new ArrayList<>();
        try (Stream<Path> files = Files.walk(projectRoot)) {
            files.filter(p -> p.toString().endsWith(".java"))
                 .filter(p -> !p.toString().contains("\\target\\") && !p.toString().contains("\\test\\"))
                 .forEach(javaFile -> {
                     try {
                         List<String> lines = Files.readAllLines(javaFile);
                         for (int i = 0; i < lines.size(); i++) {
                             String line = lines.get(i);
                             if (SQL_PATTERN.matcher(line).find()) {
                                 String sql = extractSql(line);
                                 if (sql != null && !sql.isBlank()) {
                                     List<ScanResult> scanResults = analyzer.analyze(sql, source, target);
                                     for (ScanResult r : scanResults) {
                                         r.setFilePath(javaFile.toString());
                                         r.setLineNumber(i + 1);
                                     }
                                     results.addAll(scanResults);
                                 }
                             }
                         }
                     } catch (IOException e) { log.warn("Cannot read: {}", javaFile, e); }
                 });
        } catch (IOException e) { log.error("Project scan failed", e); }
        log.info("Java source scan complete: {} issues found in {}", results.size(), projectRoot);
        return results;
    }

    private String extractSql(String line) {
        int start = line.indexOf('"');
        if (start < 0) return null;
        int end = line.lastIndexOf('"');
        if (end <= start) return null;
        return line.substring(start + 1, end);
    }

    public Set<String> supportedExtensions() { return Set.of(".java"); }
}
