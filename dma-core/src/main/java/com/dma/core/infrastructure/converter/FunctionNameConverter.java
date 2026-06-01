package com.dma.core.infrastructure.converter;
import com.dma.core.domain.model.scanner.ScanResult;
import com.dma.core.domain.service.SqlConvertStrategy;
import org.springframework.stereotype.Component;

/** Converts function names like IFNULL→COALESCE, NVL→COALESCE */
@Component
public class FunctionNameConverter implements SqlConvertStrategy {
    @Override
    public boolean supports(ScanResult issue) { return "FUNCTION".equals(issue.getSeverity()) || issue.getMessage().contains("函数"); }
    @Override
    public String convert(String sql) {
        return sql.replace("IFNULL(", "COALESCE(")
                  .replace("NVL(", "COALESCE(")
                  .replace("NOW()", "CURRENT_TIMESTAMP")
                  .replace("SYSDATE", "CURRENT_TIMESTAMP")
                  .replace("UUID()", "gen_random_uuid()");
    }
}
