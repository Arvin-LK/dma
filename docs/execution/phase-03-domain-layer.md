# Phase 3: dma-core 领域层

## 目标

完成所有聚合根、实体、值对象、仓储接口、领域服务接口、AI 预留接口。

## 前置条件

- [ ] Phase 2 完成

## 步骤

### 3.1 连接聚合
- `ConnectionId` 值对象（record）
- `DatabaseConnection` 聚合根

### 3.2 迁移任务聚合
- `TaskId` 值对象
- `RiskScore` 值对象
- `MigrationTask` 聚合根（含行为方法：start, addScanResult, resolveIssue, calculateRiskScore）

### 3.3 规则聚合
- `RuleId`, `RuleCode`, `MatchPattern`, `ReplacementPattern`, `RuleName` 值对象
- `MigrationRule` 聚合根（含 evaluate, convertSql 方法）

### 3.4 扫描结果实体
- `ScanResultId` 值对象
- `ScanSource` 枚举（JAVA_FILE, XML_FILE, SQL_FILE, DATABASE）
- `ScanResult` 实体

### 3.5 报告实体
- `MigrationReport` 实体
- `ReportSection` 值对象

### 3.6 仓储接口（领域层定义，不含实现）
- `MigrationRuleRepository`
- `MigrationTaskRepository`
- `ScanResultRepository`
- `ConnectionRepository`

### 3.7 领域服务接口
- `SqlCompatibilityAnalyzer`
- `SqlConverter`
- `SourceCodeScanner`
- `ReportGenerator`
- `RuleLoader`

### 3.8 扩展点接口
- `SqlConvertStrategy`
- `AiAdvisor`
- `LicenseManager`

## 验证标准

所有类编译通过，接口定义清晰，值对象使用 record 或 @Value。

## 产出

~15 个实体/值对象类 + 10 个接口
