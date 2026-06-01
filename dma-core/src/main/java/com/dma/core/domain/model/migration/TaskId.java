package com.dma.core.domain.model.migration;

/** 迁移任务唯一标识 */
public record TaskId(Long value) {
    public TaskId {
        if (value == null || value <= 0) throw new IllegalArgumentException("TaskId must be positive");
    }
}
