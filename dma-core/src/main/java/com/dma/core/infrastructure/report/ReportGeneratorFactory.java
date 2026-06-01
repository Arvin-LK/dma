package com.dma.core.infrastructure.report;
import com.dma.common.enums.ReportFormat;
import com.dma.core.domain.service.ReportGenerator;
import org.springframework.stereotype.Component;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class ReportGeneratorFactory {
    private final Map<ReportFormat, ReportGenerator> generators = new EnumMap<>(ReportFormat.class);

    public ReportGeneratorFactory(List<ReportGenerator> generatorList) {
        for (ReportGenerator gen : generatorList) {
            generators.put(gen.supportedFormat(), gen);
        }
    }

    public ReportGenerator getGenerator(ReportFormat format) {
        ReportGenerator gen = generators.get(format);
        if (gen == null) throw new IllegalArgumentException("No generator for format: " + format);
        return gen;
    }
}
