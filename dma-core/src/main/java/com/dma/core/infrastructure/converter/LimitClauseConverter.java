package com.dma.core.infrastructure.converter;
import com.dma.core.domain.model.scanner.ScanResult;
import com.dma.core.domain.service.SqlConvertStrategy;
import org.springframework.stereotype.Component;
import java.util.regex.Pattern;

/** Converts LIMIT offset,count to LIMIT count OFFSET offset */
@Component
public class LimitClauseConverter implements SqlConvertStrategy {
    private static final Pattern LIMIT_PATTERN = Pattern.compile("LIMIT\\s+(\\d+)\\s*,\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    @Override
    public boolean supports(ScanResult issue) { return issue.getSourceSql().toUpperCase().contains("LIMIT") && issue.getSourceSql().contains(","); }
    @Override
    public String convert(String sql) {
        return LIMIT_PATTERN.matcher(sql).replaceAll("LIMIT $2 OFFSET $1");
    }
}
