package com.dma.core.domain.service;
import com.dma.core.domain.model.scanner.ScanResult;

/** SPI 扩展点：SQL 转换策略 */
public interface SqlConvertStrategy {
    boolean supports(ScanResult issue);
    String convert(String sql);
    default String getName() { return getClass().getSimpleName(); }
}
