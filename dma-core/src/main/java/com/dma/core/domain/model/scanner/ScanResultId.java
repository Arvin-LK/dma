package com.dma.core.domain.model.scanner;
public record ScanResultId(Long value) {
    public ScanResultId { if (value == null || value <= 0) throw new IllegalArgumentException("ScanResultId must be positive"); }
}
