package com.dma.core.domain.repository;
import com.dma.core.domain.model.connection.ConnectionId;
import com.dma.core.domain.model.connection.DatabaseConnection;
import java.util.List;
import java.util.Optional;

public interface ConnectionRepository {
    Optional<DatabaseConnection> findById(ConnectionId id);
    List<DatabaseConnection> findAll(int page, int size);
    long count();
    ConnectionId save(DatabaseConnection connection);
    void update(DatabaseConnection connection);
    void delete(ConnectionId id);
    boolean existsByName(String name);
}
