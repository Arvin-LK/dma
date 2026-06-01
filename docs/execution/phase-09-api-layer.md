# Phase 9: API 层 + SQLite 存储

## 目标

实现 REST API + SQLite 数据库初始化 + Repository 实现。

## 前置条件

- [ ] Phase 8 完成（应用层服务可用）

## 步骤

### 9.1 SQLite 初始化
- 编写 `schema.sql`（完整建表语句 + 初始数据）
- 启动时自动执行 schema.sql
- `~/.dma/dma.db` 作为数据库文件路径

### 9.2 Repository 实现
- `SqliteConnectionRepository`
- `SqliteMigrationTaskRepository`
- `SqliteScanResultRepository`
- `SqliteMigrationRuleRepository`

### 9.3 REST Controller
- `ConnectionController` — 7 个端点
- `ScanController` — 3 个端点
- `ConvertController` — 2 个端点
- `TaskController` — 8 个端点
- `ReportController` — 1 个端点（3 种格式参数）
- `RuleController` — 6 个端点
- `SystemController` — 3 个端点

### 9.4 全局配置
- Swagger/OpenAPI 文档配置
- CORS 配置（允许 JavaFX localhost 调用）
- 全局异常处理器

## 验证标准

所有 API 端点可通过 Postman 或 Swagger UI 测试通过。
