package com.dma.core.infrastructure.converter;
import com.dma.core.domain.model.scanner.ScanResult;
import com.dma.core.domain.service.SqlConvertStrategy;
import org.springframework.stereotype.Component;

/** Converts AUTO_INCREMENT to SERIAL/IDENTITY */
@Component
public class AutoIncrementConverter implements SqlConvertStrategy {
    @Override
    public boolean supports(ScanResult issue) { return issue.getSourceSql().toUpperCase().contains("AUTO_INCREMENT"); }
    @Override
    public String convert(String sql) {
        return sql.replaceAll("(?i)\\bINT\\b\\s+AUTO_INCREMENT", "SERIAL")
                  .replaceAll("(?i)\\bBIGINT\\b\\s+AUTO_INCREMENT", "BIGSERIAL")
                  .replaceAll("(?i)AUTO_INCREMENT", "IDENTITY(1,1)");
    }
}
