package com.dma.core.domain.repository;
import com.dma.core.domain.model.migration.MigrationTask;
import com.dma.core.domain.model.migration.TaskId;
import com.dma.common.enums.TaskStatus;
import java.util.List;
import java.util.Optional;

public interface MigrationTaskRepository {
    Optional<MigrationTask> findById(TaskId id);
    List<MigrationTask> findAll(int page, int size);
    List<MigrationTask> findByStatus(TaskStatus status);
    long count();
    TaskId save(MigrationTask task);
    void update(MigrationTask task);
    void delete(TaskId id);
}
