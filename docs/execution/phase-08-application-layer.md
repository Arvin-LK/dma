# Phase 8: 应用层编排

## 目标

实现 Application Service，编排完整业务流程。

## 前置条件

- [ ] Phase 7 完成（报告生成器可用）

## 步骤

### 8.1 MigrationApplicationService
- `createTask()` — 创建迁移任务
- `executeScan(taskId)` — 执行扫描（加载规则→扫描→转换→保存结果）
- `generateReport(taskId, format)` — 生成报告
- `resolveIssue(taskId, resultId)` — 标记已处理

### 8.2 ConnectionApplicationService
- `createConnection()` / `updateConnection()` / `deleteConnection()`
- `listConnections()` / `getConnection()`
- `testConnection()` — 使用 Druid 临时连接池测试

### 8.3 ScanApplicationService
- `scanSql()` — 单条 SQL 扫描
- `scanFile()` — SQL 文件扫描
- `scanProject()` — 项目源码扫描（委托给 SourceCodeScanner）

### 8.4 RuleApplicationService
- `listRules()` — 按源/目标查询规则
- `addCustomRule()` / `updateCustomRule()` / `deleteCustomRule()`

## 验证标准

集成测试覆盖完整业务流程（创建任务→扫描→转换→生成报告）。
