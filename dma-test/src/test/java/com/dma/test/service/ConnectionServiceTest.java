package com.dma.test.service;

import com.dma.core.application.service.ConnectionApplicationService;
import com.dma.core.domain.repository.ConnectionRepository;
import com.dma.core.infrastructure.util.CryptoUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConnectionServiceTest {

    private CryptoUtil crypto;
    private ConnectionRepository repository;

    @BeforeEach
    void setUp() {
        crypto = new CryptoUtil("test-key-for-unit-tests");
        repository = new ConnectionRepository() {
            private final java.util.Map<Long, com.dma.core.domain.model.connection.DatabaseConnection> store = new java.util.HashMap<>();
            private long seq = 0;

            @Override
            public com.dma.core.domain.model.connection.ConnectionId save(com.dma.core.domain.model.connection.DatabaseConnection conn) {
                conn.setId(new com.dma.core.domain.model.connection.ConnectionId(++seq));
                store.put(seq, conn);
                return conn.getId();
            }
            @Override public java.util.Optional<com.dma.core.domain.model.connection.DatabaseConnection> findById(com.dma.core.domain.model.connection.ConnectionId id) { return java.util.Optional.ofNullable(store.get(id.value())); }
            @Override public java.util.List<com.dma.core.domain.model.connection.DatabaseConnection> findAll(int p, int s) { return new java.util.ArrayList<>(store.values()); }
            @Override public long count() { return store.size(); }
            @Override public void update(com.dma.core.domain.model.connection.DatabaseConnection c) { store.put(c.getId().value(), c); }
            @Override public void delete(com.dma.core.domain.model.connection.ConnectionId id) { store.remove(id.value()); }
            @Override public boolean existsByName(String name) { return store.values().stream().anyMatch(c -> c.getName().equals(name)); }
        };
    }

    @Test
    void shouldCreateConnectionWithEncryptedPassword() {
        var service = new ConnectionApplicationService(repository, crypto);
        var id = service.create("test-db", "MYSQL", "localhost", 3306, "root", "secret123", "mydb");
        assertNotNull(id);
        var saved = repository.findById(id);
        assertTrue(saved.isPresent());
        // Password should be encrypted (not plaintext)
        assertNotEquals("secret123", saved.get().getEncryptedPassword());
    }

    @Test
    void shouldRejectDuplicateConnectionName() {
        var service = new ConnectionApplicationService(repository, crypto);
        service.create("dup", "MYSQL", "localhost", 3306, "root", "p1", "db");
        assertThrows(com.dma.common.exception.ConnectionException.class, () -> service.create("dup", "MYSQL", "h", 1, "u", "p", "d"));
    }

    @Test
    void shouldListConnections() {
        var service = new ConnectionApplicationService(repository, crypto);
        service.create("c1", "MYSQL", "h1", 1, "u", "p1", "d");
        service.create("c2", "ORACLE", "h2", 2, "u", "p2", "d");
        assertEquals(2, service.list(1, 10).size());
    }
}
