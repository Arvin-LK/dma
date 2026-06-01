package com.dma.core.infrastructure.converter;
import com.dma.core.domain.model.scanner.ScanResult;
import com.dma.core.domain.service.SqlConvertStrategy;
import org.springframework.stereotype.Component;
import java.util.regex.Pattern;

/** Specifically handles Oracle NVL function conversion */
@Component
public class NvlToCoalesceConverter implements SqlConvertStrategy {
    private static final Pattern NVL_PATTERN = Pattern.compile("NVL\\s*\\(\\s*(.+?)\\s*,\\s*(.+?)\\s*\\)", Pattern.CASE_INSENSITIVE);
    @Override
    public boolean supports(ScanResult issue) { return issue.getSourceSql().toUpperCase().contains("NVL("); }
    @Override
    public String convert(String sql) { return NVL_PATTERN.matcher(sql).replaceAll("COALESCE($1, $2)"); }
}
