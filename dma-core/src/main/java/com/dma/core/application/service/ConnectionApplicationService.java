package com.dma.core.application.service;
import com.dma.common.enums.DatabaseType;
import com.dma.common.exception.ConnectionException;
import com.dma.common.exception.ConnectionTimeoutException;
import com.dma.core.domain.model.connection.ConnectionId;
import com.dma.core.domain.model.connection.DatabaseConnection;
import com.dma.core.domain.repository.ConnectionRepository;
import com.dma.core.infrastructure.util.CryptoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ConnectionApplicationService {
    private static final Logger log = LoggerFactory.getLogger(ConnectionApplicationService.class);
    private final ConnectionRepository repository;
    private final CryptoUtil crypto;

    public ConnectionApplicationService(ConnectionRepository repository, CryptoUtil crypto) {
        this.repository = repository;
        this.crypto = crypto;
    }

    public ConnectionId create(String name, String dbType, String host, int port, String username, String password, String database) {
        if (repository.existsByName(name)) throw new ConnectionException("CONN_004", "连接名已存在: " + name);
        String encrypted = crypto.encrypt(password);
        DatabaseConnection conn = new DatabaseConnection(name, DatabaseType.valueOf(dbType), host, port, username, encrypted, database);
        return repository.save(conn);
    }

    public List<DatabaseConnection> list(int page, int size) { return repository.findAll(page, size); }
    public DatabaseConnection get(ConnectionId id) { return repository.findById(id).orElseThrow(() -> new ConnectionException("CONN_005", "连接不存在")); }
    public void delete(ConnectionId id) { repository.delete(id); }

    public boolean test(String dbType, String host, int port, String username, String password, String database) {
        try {
            DatabaseType type = DatabaseType.valueOf(dbType);
            String jdbcUrl = buildJdbcUrl(type, host, port, database);
            java.sql.Connection conn = java.sql.DriverManager.getConnection(jdbcUrl, username, password);
            conn.close();
            log.info("Connection test successful: {}:{}", host, port);
            return true;
        } catch (Exception e) {
            log.warn("Connection test failed: {}:{}", host, port, e);
            throw new ConnectionTimeoutException(host, port);
        }
    }

    private String buildJdbcUrl(DatabaseType dbType, String host, int port, String database) {
        return switch (dbType) {
            case MYSQL -> String.format("jdbc:mysql://%s:%d/%s?useSSL=false&connectTimeout=5000", host, port, database != null ? database : "");
            case POSTGRESQL, GAUSSDB -> String.format("jdbc:postgresql://%s:%d/%s?connectTimeout=5", host, port, database != null ? database : "");
            case ORACLE -> String.format("jdbc:oracle:thin:@%s:%d:%s", host, port, database != null ? database : "");
            case SQLSERVER -> String.format("jdbc:sqlserver://%s:%d;databaseName=%s;encrypt=false;trustServerCertificate=true", host, port, database != null ? database : "");
            case DAMENG -> String.format("jdbc:dm://%s:%d/%s", host, port, database != null ? database : "");
            case OCEANBASE, GOLDENDB -> String.format("jdbc:mysql://%s:%d/%s?useSSL=false&connectTimeout=5000", host, port, database != null ? database : "");
        };
    }
}
