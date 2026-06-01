package com.dma.core.infrastructure.scanner;
import com.dma.common.enums.DatabaseType;
import com.dma.core.domain.model.scanner.ScanResult;
import com.dma.core.domain.service.SourceCodeScanner;
import org.springframework.stereotype.Component;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Default implementation of SourceCodeScanner domain service */
@Component
public class DefaultSourceCodeScanner implements SourceCodeScanner {
    private final JavaSourceScanner javaScanner;
    private final MyBatisXmlScanner xmlScanner;

    public DefaultSourceCodeScanner(JavaSourceScanner javaScanner, MyBatisXmlScanner xmlScanner) {
        this.javaScanner = javaScanner; this.xmlScanner = xmlScanner;
    }

    @Override
    public List<ScanResult> scanProject(Path projectPath, DatabaseType source, DatabaseType target) {
        List<ScanResult> results = new ArrayList<>();
        results.addAll(javaScanner.scan(projectPath, source, target));
        results.addAll(xmlScanner.scan(projectPath, source, target));
        return results;
    }
}
