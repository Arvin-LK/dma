package com.dma.core.domain.service;
import com.dma.common.enums.ReportFormat;
import com.dma.core.domain.model.report.MigrationReport;
import com.dma.core.domain.model.scanner.ScanResult;
import java.util.List;

/** 领域服务：报告生成器 */
public interface ReportGenerator {
    byte[] generate(MigrationReport report, List<ScanResult> results);
    ReportFormat supportedFormat();
    String fileExtension();
}
