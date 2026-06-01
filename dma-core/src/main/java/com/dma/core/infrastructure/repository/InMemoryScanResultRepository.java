package com.dma.core.infrastructure.repository;
import com.dma.core.domain.model.migration.TaskId;
import com.dma.core.domain.model.scanner.ScanResult;
import com.dma.core.domain.model.scanner.ScanResultId;
import com.dma.core.domain.repository.ScanResultRepository;
import org.springframework.stereotype.Repository;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryScanResultRepository implements ScanResultRepository {
    private final Map<TaskId, List<ScanResult>> store = new ConcurrentHashMap<>();
    private final AtomicLong idSeq = new AtomicLong(1);

    @Override public List<ScanResult> findByTaskId(TaskId taskId) { return store.getOrDefault(taskId, List.of()); }
    @Override public ScanResultId save(TaskId taskId, ScanResult result) {
        ScanResultId id = new ScanResultId(idSeq.getAndIncrement());
        result.setId(id);
        store.computeIfAbsent(taskId, k -> new ArrayList<>()).add(result);
        return id;
    }
    @Override public void update(ScanResult result) {}
    @Override public void deleteByTaskId(TaskId taskId) { store.remove(taskId); }
}
