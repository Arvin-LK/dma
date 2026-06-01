package com.dma.core.domain.service;
import com.dma.common.enums.DatabaseType;
import com.dma.core.domain.model.scanner.ScanResult;
import java.util.List;

/** 领域服务：SQL 兼容性分析器 */
public interface SqlCompatibilityAnalyzer {
    List<ScanResult> analyze(String sql, DatabaseType source, DatabaseType target);
}
