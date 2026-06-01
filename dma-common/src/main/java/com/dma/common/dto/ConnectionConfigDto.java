package com.dma.common.dto;

public class ConnectionConfigDto {
    private String name;
    private String dbType;
    private String host;
    private int port;
    private String username;
    private String databaseName;

    public ConnectionConfigDto() {}

    public ConnectionConfigDto(String name, String dbType, String host, int port, String username, String databaseName) {
        this.name = name; this.dbType = dbType; this.host = host;
        this.port = port; this.username = username; this.databaseName = databaseName;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDbType() { return dbType; }
    public void setDbType(String dbType) { this.dbType = dbType; }
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getDatabaseName() { return databaseName; }
    public void setDatabaseName(String databaseName) { this.databaseName = databaseName; }
}
