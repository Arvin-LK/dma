package com.dma.core.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * SQLite 数据库初始化器。
 * 应用启动时自动创建数据库文件和表结构。
 */
@Component
public class DatabaseInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

    @Value("${dma.database-path:${user.home}/.dma/dma.db}")
    private String dbPath;

    @Override
    public void run(String... args) throws Exception {
        // 确保目录存在
        File dbFile = new File(dbPath);
        File parentDir = dbFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
            log.info("Created directory: {}", parentDir.getAbsolutePath());
        }

        String jdbcUrl = "jdbc:sqlite:" + dbPath;
        log.info("Initializing SQLite database: {}", jdbcUrl);

        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             Statement stmt = conn.createStatement()) {

            // 启用外键约束
            stmt.execute("PRAGMA foreign_keys = ON");

            // 规则分类表
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS rule_category (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    category_name VARCHAR(100) NOT NULL,
                    parent_category_id INTEGER,
                    display_order INTEGER DEFAULT 0,
                    FOREIGN KEY (parent_category_id) REFERENCES rule_category(id)
                )
                """);

            // 数据库连接配置表
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS database_connection (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name VARCHAR(100) NOT NULL UNIQUE,
                    db_type VARCHAR(20) NOT NULL,
                    host VARCHAR(255) NOT NULL,
                    port INTEGER NOT NULL,
                    username VARCHAR(100) NOT NULL,
                    password_encrypted VARCHAR(500) NOT NULL,
                    database_name VARCHAR(100),
                    created_at TEXT DEFAULT (datetime('now','localtime')),
                    updated_at TEXT DEFAULT (datetime('now','localtime'))
                )
                """);

            // 迁移任务表
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS migration_task (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    task_name VARCHAR(200) NOT NULL,
                    source_conn_id INTEGER NOT NULL,
                    target_conn_id INTEGER NOT NULL,
                    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                    total_issues INTEGER DEFAULT 0,
                    resolved_issues INTEGER DEFAULT 0,
                    risk_score INTEGER DEFAULT 0,
                    error_message TEXT,
                    created_at TEXT DEFAULT (datetime('now','localtime')),
                    completed_at TEXT,
                    FOREIGN KEY (source_conn_id) REFERENCES database_connection(id),
                    FOREIGN KEY (target_conn_id) REFERENCES database_connection(id)
                )
                """);

            // 扫描结果表
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS scan_result (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    task_id INTEGER NOT NULL,
                    rule_code VARCHAR(50) NOT NULL,
                    file_path VARCHAR(500),
                    line_number INTEGER DEFAULT 0,
                    column_number INTEGER DEFAULT 0,
                    source_sql TEXT NOT NULL,
                    suggested_sql TEXT,
                    compatibility_level VARCHAR(30) NOT NULL,
                    severity VARCHAR(20) NOT NULL,
                    is_resolved INTEGER DEFAULT 0,
                    resolution_note TEXT,
                    created_at TEXT DEFAULT (datetime('now','localtime')),
                    FOREIGN KEY (task_id) REFERENCES migration_task(id) ON DELETE CASCADE
                )
                """);

            // 规则表（用于 SQLite 存储自定义规则）
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS migration_rule (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    rule_code VARCHAR(50) NOT NULL UNIQUE,
                    source_db_type VARCHAR(20) NOT NULL,
                    target_db_type VARCHAR(20) NOT NULL,
                    rule_name VARCHAR(200) NOT NULL,
                    category_id INTEGER NOT NULL,
                    severity VARCHAR(20) NOT NULL DEFAULT 'WARNING',
                    pattern_type VARCHAR(20) NOT NULL,
                    match_pattern TEXT NOT NULL,
                    replacement_pattern TEXT,
                    description TEXT,
                    example_sql_source TEXT,
                    example_sql_target TEXT,
                    is_enabled INTEGER DEFAULT 1,
                    created_at TEXT DEFAULT (datetime('now','localtime')),
                    updated_at TEXT DEFAULT (datetime('now','localtime')),
                    FOREIGN KEY (category_id) REFERENCES rule_category(id)
                )
                """);

            // 插入默认分类
            stmt.execute("""
                INSERT OR IGNORE INTO rule_category (id, category_name, display_order) VALUES
                    (1, '内置函数', 1),
                    (2, '语法差异', 2),
                    (3, '数据类型', 3),
                    (4, 'DDL差异', 4),
                    (5, '存储过程', 5),
                    (6, '其他', 99)
                """);

            // 创建索引
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_task_status ON migration_task(status)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_result_task ON scan_result(task_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_rule_db ON migration_rule(source_db_type, target_db_type)");

            log.info("SQLite database initialized successfully");
        }
    }

    public String getDbPath() {
        return dbPath;
    }
}
