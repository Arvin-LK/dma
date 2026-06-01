# 数据库设计

## 概述

MVP 阶段使用 **SQLite** 作为本地存储引擎，存储数据库连接配置、迁移任务、扫描结果和扩展规则。

**数据库文件位置**: `~/.dma/dma.db`（用户主目录下的 .dma 文件夹）

---

## ER 图

```
┌──────────────────────┐       ┌──────────────────────────┐
│  database_connection  │       │     migration_rule        │
├──────────────────────┤       ├──────────────────────────┤
│ id (PK) INTEGER      │       │ id (PK) INTEGER          │
│ name VARCHAR(100)    │       │ rule_code VARCHAR(50) UK  │
│ db_type VARCHAR(20)  │       │ source_db_type VARCHAR(20)│
│ host VARCHAR(255)    │       │ target_db_type VARCHAR(20)│
│ port INTEGER         │       │ rule_name VARCHAR(200)    │
│ username VARCHAR(100)│       │ category_id (FK) INTEGER  │
│ password_encrypted   │       │ severity VARCHAR(20)      │
│   VARCHAR(500)       │       │ pattern_type VARCHAR(20)  │
│ database_name        │       │ match_pattern TEXT        │
│   VARCHAR(100)       │       │ replacement_pattern TEXT  │
│ created_at DATETIME  │       │ description TEXT          │
│ updated_at DATETIME  │       │ example_sql_source TEXT   │
└──────────┬───────────┘       │ example_sql_target TEXT   │
           │                   │ is_enabled INTEGER        │
           │                   │ source_min_version        │
┌──────────┴───────────┐       │ source_max_version        │
│   migration_task      │       │ target_min_version        │
├──────────────────────┤       │ target_max_version        │
│ id (PK) INTEGER      │       │ created_at DATETIME       │
│ task_name VARCHAR(200)│       │ updated_at DATETIME       │
│ source_conn_id (FK)  │───────└──────────────────────────┘
│ target_conn_id (FK)  │
│ status VARCHAR(20)   │       ┌──────────────────────────┐
│ total_issues INTEGER │       │     rule_category         │
│ resolved_issues INT  │       ├──────────────────────────┤
│ risk_score INTEGER   │       │ id (PK) INTEGER          │
│ error_message TEXT   │       │ category_name VARCHAR(100)│
│ created_at DATETIME  │       │ parent_category_id (FK)  │
│ completed_at DATETIME│       │ display_order INTEGER     │
└──────────┬───────────┘       └──────────────────────────┘
           │
┌──────────┴───────────┐
│    scan_result        │
├──────────────────────┤
│ id (PK) INTEGER      │
│ task_id (FK) INTEGER │
│ rule_code VARCHAR(50)│
│ file_path VARCHAR(500)│
│ line_number INTEGER  │
│ column_number INTEGER│
│ source_sql TEXT      │
│ suggested_sql TEXT   │
│ compatibility_level  │
│   VARCHAR(30)        │
│ severity VARCHAR(20) │
│ is_resolved INTEGER  │
│ resolution_note TEXT │
│ created_at DATETIME  │
└──────────────────────┘
```

---

## 完整建表 SQL

```sql
-- ============================================
-- DMA 数据库初始化脚本
-- 目标: SQLite
-- ============================================

-- 1. 规则分类表
CREATE TABLE IF NOT EXISTS rule_category (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    category_name VARCHAR(100) NOT NULL,
    parent_category_id INTEGER,
    display_order INTEGER DEFAULT 0,
    FOREIGN KEY (parent_category_id) REFERENCES rule_category(id)
);

-- 初始分类数据
INSERT OR IGNORE INTO rule_category (id, category_name, display_order) VALUES
    (1, '内置函数', 1),
    (2, '语法差异', 2),
    (3, '数据类型', 3),
    (4, 'DDL差异', 4),
    (5, '存储过程', 5),
    (6, '其他', 99);

-- 2. 数据库连接表
CREATE TABLE IF NOT EXISTS database_connection (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(100) NOT NULL,
    db_type VARCHAR(20) NOT NULL,
    host VARCHAR(255) NOT NULL,
    port INTEGER NOT NULL,
    username VARCHAR(100) NOT NULL,
    password_encrypted VARCHAR(500) NOT NULL,
    database_name VARCHAR(100),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_connection_name ON database_connection(name);

-- 3. 迁移任务表
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
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME,
    FOREIGN KEY (source_conn_id) REFERENCES database_connection(id),
    FOREIGN KEY (target_conn_id) REFERENCES database_connection(id)
);

CREATE INDEX IF NOT EXISTS idx_task_status ON migration_task(status);
CREATE INDEX IF NOT EXISTS idx_task_created ON migration_task(created_at DESC);

-- 4. 扫描结果表
CREATE TABLE IF NOT EXISTS scan_result (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id INTEGER NOT NULL,
    rule_code VARCHAR(50) NOT NULL,
    file_path VARCHAR(500),
    line_number INTEGER,
    column_number INTEGER,
    source_sql TEXT NOT NULL,
    suggested_sql TEXT,
    compatibility_level VARCHAR(30) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    is_resolved INTEGER DEFAULT 0,
    resolution_note TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (task_id) REFERENCES migration_task(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_result_task ON scan_result(task_id);
CREATE INDEX IF NOT EXISTS idx_result_level ON scan_result(compatibility_level);
CREATE INDEX IF NOT EXISTS idx_result_severity ON scan_result(severity);

-- 5. 兼容性规则表 (用于 SQLite 存储规则，MVP 主要用 JSON)
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
    source_min_version VARCHAR(20),
    source_max_version VARCHAR(20),
    target_min_version VARCHAR(20),
    target_max_version VARCHAR(20),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES rule_category(id)
);

CREATE INDEX IF NOT EXISTS idx_rule_src_tgt ON migration_rule(source_db_type, target_db_type);
CREATE INDEX IF NOT EXISTS idx_rule_code ON migration_rule(rule_code);
CREATE INDEX IF NOT EXISTS idx_rule_category ON migration_rule(category_id);
CREATE INDEX IF NOT EXISTS idx_rule_enabled ON migration_rule(is_enabled);
```

---

## 设计决策

| 决策 | 理由 |
|------|------|
| **SQLite 而非 H2** | SQLite 是本地文件数据库，数据文件可拷贝迁移，更适合同步和备份 |
| **密码加密存储** | 使用 AES-256 加密，密钥由用户主密码派生 |
| **ON DELETE CASCADE** | scan_result 是任务的子项，任务删除时结果一并删除 |
| **TEXT 而非 VARCHAR(长度)** | SQLite 不强制 VARCHAR 长度，但保留长度标注以提高可读性 |
| **INTEGER DEFAULT 0 for boolean** | SQLite 无 BOOLEAN 类型，使用 INTEGER 1/0 代替 |
