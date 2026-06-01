package com.dma.core.infrastructure.repository;
import com.dma.common.enums.TaskStatus;
import com.dma.core.domain.model.migration.MigrationTask;
import com.dma.core.domain.model.migration.TaskId;
import com.dma.core.domain.repository.MigrationTaskRepository;
import org.springframework.stereotype.Repository;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryTaskRepository implements MigrationTaskRepository {
    private final Map<TaskId, MigrationTask> store = new ConcurrentHashMap<>();
    private final AtomicLong idSeq = new AtomicLong(1);

    @Override public Optional<MigrationTask> findById(TaskId id) { return Optional.ofNullable(store.get(id)); }
    @Override public List<MigrationTask> findAll(int page, int size) {
        return store.values().stream().skip((long)(page-1)*size).limit(size).toList();
    }
    @Override public List<MigrationTask> findByStatus(TaskStatus status) {
        return store.values().stream().filter(t -> t.getStatus() == status).toList();
    }
    @Override public long count() { return store.size(); }
    @Override public TaskId save(MigrationTask task) {
        TaskId id = new TaskId(idSeq.getAndIncrement());
        task.setId(id); store.put(id, task); return id;
    }
    @Override public void update(MigrationTask task) { store.put(task.getId(), task); }
    @Override public void delete(TaskId id) { store.remove(id); }
}
