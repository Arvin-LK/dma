package com.dma.core.infrastructure.repository;
import com.dma.core.domain.model.connection.ConnectionId;
import com.dma.core.domain.model.connection.DatabaseConnection;
import com.dma.core.domain.repository.ConnectionRepository;
import org.springframework.stereotype.Repository;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** MVP in-memory implementation. Replaced by SqliteConnectionRepository. */
// @Repository
public class InMemoryConnectionRepository implements ConnectionRepository {
    private final Map<ConnectionId, DatabaseConnection> store = new ConcurrentHashMap<>();
    private final AtomicLong idSeq = new AtomicLong(1);

    @Override public Optional<DatabaseConnection> findById(ConnectionId id) { return Optional.ofNullable(store.get(id)); }
    @Override public List<DatabaseConnection> findAll(int page, int size) {
        return store.values().stream().skip((long)(page-1)*size).limit(size).toList();
    }
    @Override public long count() { return store.size(); }
    @Override public ConnectionId save(DatabaseConnection conn) {
        ConnectionId id = new ConnectionId(idSeq.getAndIncrement());
        conn.setId(id); store.put(id, conn); return id;
    }
    @Override public void update(DatabaseConnection conn) { store.put(conn.getId(), conn); }
    @Override public void delete(ConnectionId id) { store.remove(id); }
    @Override public boolean existsByName(String name) { return store.values().stream().anyMatch(c -> c.getName().equals(name)); }
}
