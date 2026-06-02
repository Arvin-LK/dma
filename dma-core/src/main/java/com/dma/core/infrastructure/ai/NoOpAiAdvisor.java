package com.dma.core.infrastructure.ai;

import com.dma.core.domain.model.scanner.ScanResult;
import com.dma.core.domain.service.AiAdvisor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/** MVP 占位——dma.ai.provider=noop 时生效 */
@Component
@ConditionalOnProperty(name = "dma.ai.provider", havingValue = "noop", matchIfMissing = true)
public class NoOpAiAdvisor implements AiAdvisor {

    @Override
    public String generateAdvice(ScanResult result) {
        return "AI 迁移顾问未启用。\n\n启用：修改 application.yml 中 dma.ai.provider=ollama|openai|custom";
    }

    @Override
    public String generateMigrationPlan(List<ScanResult> issues) {
        return generateAdvice(null);
    }

    @Override
    public String convertWithAI(String sourceDdl, String sourceDb, String targetDb) {
        return generateAdvice(null);
    }

    @Override
    public boolean isAvailable() { return false; }

    @Override
    public String getModelName() { return "未启用"; }
}
