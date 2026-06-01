package com.dma.core.infrastructure.scanner;
import com.dma.common.enums.DatabaseType;
import com.dma.core.domain.model.scanner.ScanResult;
import com.dma.core.domain.service.SqlCompatibilityAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/** Scans MyBatis Mapper XML files for SQL statements */
@Component
public class MyBatisXmlScanner {
    private static final Logger log = LoggerFactory.getLogger(MyBatisXmlScanner.class);
    private static final Set<String> SQL_TAGS = Set.of("select", "insert", "update", "delete");
    private final SqlCompatibilityAnalyzer analyzer;

    public MyBatisXmlScanner(SqlCompatibilityAnalyzer analyzer) { this.analyzer = analyzer; }

    public List<ScanResult> scan(Path projectRoot, DatabaseType source, DatabaseType target) {
        List<ScanResult> results = new ArrayList<>();
        try (Stream<Path> files = Files.walk(projectRoot)) {
            files.filter(p -> p.toString().endsWith("Mapper.xml") || p.toString().endsWith(".xml"))
                 .forEach(xmlFile -> results.addAll(scanXmlFile(xmlFile, source, target)));
        } catch (IOException e) { log.error("XML scan failed", e); }
        log.info("MyBatis XML scan complete: {} issues found in {}", results.size(), projectRoot);
        return results;
    }

    private List<ScanResult> scanXmlFile(Path xmlFile, DatabaseType source, DatabaseType target) {
        List<ScanResult> results = new ArrayList<>();
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile.toFile());
            for (String tag : SQL_TAGS) {
                NodeList nodes = doc.getElementsByTagName(tag);
                for (int i = 0; i < nodes.getLength(); i++) {
                    String sql = nodes.item(i).getTextContent().trim();
                    String id = ((Element) nodes.item(i)).getAttribute("id");
                    if (!sql.isBlank()) {
                        sql = sql.replaceAll("#\\{[^}]+\\}", "?")
                                .replaceAll("\\$\\{[^}]+\\}", "'placeholder'");
                        List<ScanResult> scanResults = analyzer.analyze(sql, source, target);
                        for (ScanResult r : scanResults) {
                            r.setFilePath(xmlFile.toString() + " [" + tag + " id='" + id + "']");
                        }
                        results.addAll(scanResults);
                    }
                }
            }
        } catch (Exception e) { log.debug("Cannot parse XML: {}", xmlFile, e); }
        return results;
    }

    public Set<String> supportedExtensions() { return Set.of(".xml"); }
}
