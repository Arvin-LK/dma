package com.dma.core.infrastructure.converter;
import com.dma.core.domain.model.scanner.ScanResult;
import com.dma.core.domain.service.SqlConvertStrategy;
import org.springframework.stereotype.Component;

/** Converts MySQL backtick ` to PostgreSQL double-quote " */
@Component
public class IdentifierQuoteConverter implements SqlConvertStrategy {
    @Override
    public boolean supports(ScanResult issue) { return issue.getSourceSql().contains("`"); }
    @Override
    public String convert(String sql) { return sql.replace('`', '"'); }
}
