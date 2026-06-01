package com.dma.common.enums;

/**
 * 数据库类型枚举。
 * 支持通过 SPI 扩展新数据库类型。
 */
public enum DatabaseType {

    // ====== 源库 ======
    MYSQL("MySQL", "com.mysql.cj.jdbc.Driver", 3306),
    ORACLE("Oracle", "oracle.jdbc.OracleDriver", 1521),
    SQLSERVER("SQL Server", "com.microsoft.sqlserver.jdbc.SQLServerDriver", 1433),

    // ====== 目标库 ======
    POSTGRESQL("PostgreSQL", "org.postgresql.Driver", 5432),
    GAUSSDB("GaussDB", "com.huawei.gauss.jdbc.ZenithDriver", 8000),
    GOLDENDB("GoldenDB", "com.mysql.cj.jdbc.Driver", 3306),
    OCEANBASE("OceanBase", "com.alipay.oceanbase.jdbc.Driver", 2883),
    DAMENG("达梦", "dm.jdbc.driver.DmDriver", 5236);

    private final String displayName;
    private final String driverClassName;
    private final int defaultPort;

    DatabaseType(String displayName, String driverClassName, int defaultPort) {
        this.displayName = displayName;
        this.driverClassName = driverClassName;
        this.defaultPort = defaultPort;
    }

    public String getDisplayName() { return displayName; }
    public String getDriverClassName() { return driverClassName; }
    public int getDefaultPort() { return defaultPort; }
}
