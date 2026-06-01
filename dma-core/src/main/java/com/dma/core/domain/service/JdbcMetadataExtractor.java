package com.dma.core.domain.service;

import com.dma.common.enums.DatabaseType;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * JDBC 数据库元数据提取器。
 * 策略模式——每种数据库类型提供不同的实现。
 * 负责从源库中提取所有可分析对象的 DDL。
 */
public interface JdbcMetadataExtractor {

    /** 是否支持此数据库类型 */
    boolean supports(DatabaseType dbType);

    /** 提取所有存储过程定义（DDL 文本列表） */
    List<String> extractStoredProcedures(Connection conn, String schema) throws SQLException;

    /** 提取所有函数定义（DDL 文本列表） */
    List<String> extractFunctions(Connection conn, String schema) throws SQLException;

    /** 提取所有表的 DDL（CREATE TABLE 语句） */
    List<String> extractTableDDLs(Connection conn, String schema) throws SQLException;

    /** 提取所有视图的 DDL（CREATE VIEW 语句） */
    List<String> extractViewDDLs(Connection conn, String schema) throws SQLException;

    /** 获取某个 schema 下的对象总数 */
    int getObjectCount(Connection conn, String schema) throws SQLException;
}
