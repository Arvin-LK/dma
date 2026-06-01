package com.dma.core.domain.service;
import com.dma.core.domain.model.scanner.ScanResult;
import java.util.List;

/** SPI 扩展点：AI 迁移顾问（MVP 预留接口） */
public interface AiAdvisor {
    default String generateAdvice(ScanResult result) { return "AI 迁移顾问将在后续版本中提供。"; }
    default String generateMigrationPlan(List<ScanResult> issues) { return "AI 迁移计划生成将在后续版本中提供。"; }
}
