package com.dma.core.domain.model.connection;

import com.dma.common.enums.DatabaseType;
import java.time.LocalDateTime;

/** 数据库连接聚合根 */
public class DatabaseConnection {
    private ConnectionId id;
    private String name;
    private DatabaseType dbType;
    private String host;
    private int port;
    private String username;
    private String encryptedPassword;
    private String databaseName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public DatabaseConnection(String name, DatabaseType dbType, String host, int port, String username, String encryptedPassword, String databaseName) {
        this.name = name; this.dbType = dbType; this.host = host; this.port = port;
        this.username = username; this.encryptedPassword = encryptedPassword;
        this.databaseName = databaseName; this.createdAt = LocalDateTime.now(); this.updatedAt = LocalDateTime.now();
    }

    public void updateConnection(String name, String host, int port, String username, String encryptedPassword, String databaseName) {
        this.name = name; this.host = host; this.port = port; this.username = username;
        this.encryptedPassword = encryptedPassword; this.databaseName = databaseName; this.updatedAt = LocalDateTime.now();
    }

    public ConnectionId getId() { return id; }
    public void setId(ConnectionId id) { this.id = id; }
    public String getName() { return name; }
    public DatabaseType getDbType() { return dbType; }
    public String getHost() { return host; }
    public int getPort() { return port; }
    public String getUsername() { return username; }
    public String getEncryptedPassword() { return encryptedPassword; }
    public String getDatabaseName() { return databaseName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime t) { this.createdAt = t; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime t) { this.updatedAt = t; }
}
