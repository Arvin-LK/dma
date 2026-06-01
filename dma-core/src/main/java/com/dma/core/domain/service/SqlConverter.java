package com.dma.core.domain.service;
import com.dma.core.domain.model.scanner.ScanResult;
import java.util.List;

/** 领域服务：SQL 转换器 */
public interface SqlConverter {
    String convert(String sql, List<ScanResult> issues);
    ScanResult convertSingle(ScanResult issue);
}
