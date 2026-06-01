package com.dma.core.domain.repository;
import com.dma.core.domain.model.migration.TaskId;
import com.dma.core.domain.model.scanner.ScanResult;
import com.dma.core.domain.model.scanner.ScanResultId;
import java.util.List;

public interface ScanResultRepository {
    List<ScanResult> findByTaskId(TaskId taskId);
    ScanResultId save(TaskId taskId, ScanResult result);
    void update(ScanResult result);
    void deleteByTaskId(TaskId taskId);
}
