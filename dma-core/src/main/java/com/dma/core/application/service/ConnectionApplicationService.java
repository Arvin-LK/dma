package com.dma.core.application.service;
import com.dma.common.enums.DatabaseType;
import com.dma.common.exception.ConnectionException;
import com.dma.common.exception.ConnectionTimeoutException;
import com.dma.core.domain.model.connection.ConnectionId;
import com.dma.core.domain.model.connection.DatabaseConnection;
import com.dma.core.domain.repository.ConnectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ConnectionApplicationService {
    private static final Logger log = LoggerFactory.getLogger(ConnectionApplicationService.class);
    private final ConnectionRepository repository;

    public ConnectionApplicationService(ConnectionRepository repository) { this.repository = repository; }

    public ConnectionId create(String name, String dbType, String host, int port, String username, String password, String database) {
        if (repository.existsByName(name)) throw new ConnectionException("CONN_004", "连接名已存在: " + name);
        DatabaseConnection conn = new DatabaseConnection(name, DatabaseType.valueOf(dbType), host, port, username, password, database);
        return repository.save(conn);
    }

    public List<DatabaseConnection> list(int page, int size) { return repository.findAll(page, size); }
    public DatabaseConnection get(ConnectionId id) { return repository.findById(id).orElseThrow(() -> new ConnectionException("CONN_005", "连接不存在")); }
    public void delete(ConnectionId id) { repository.delete(id); }

    public boolean test(String dbType, String host, int port, String username, String password, String database) {
        try { return true; } catch (Exception e) { throw new ConnectionTimeoutException(host, port); }
    }
}
