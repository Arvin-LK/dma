# Phase 4: 规则引擎 + JSON 规则文件

## 目标

实现规则加载、匹配、缓存引擎 + 3 条路径的 JSON 规则文件（共约 80 条规则）。

## 前置条件

- [ ] Phase 3 完成（领域层接口已定义）

## 步骤

### 4.1 JsonFileRuleLoader
- 从 `classpath:rules/*.json` 加载规则
- 解析 JSON 为 `MigrationRule` 对象列表
- 按 sourceDb + targetDb 建立索引

### 4.2 RuleEngine 核心
- 规则缓存（Map<String, List<MigrationRule>>，key="MYSQL->POSTGRESQL"）
- 按源-目标查询规则
- 规则热重载

### 4.3 DefaultSqlCompatibilityAnalyzer
- 实现 `SqlCompatibilityAnalyzer` 接口
- 逐个规则匹配 SQL，生成 CompatibilityResult 列表

### 4.4 JSON 规则文件编写

| 文件 | 路径 | 规则数量 |
|------|------|---------|
| `mysql-postgresql.json` | `dma-core/src/main/resources/rules/` | ≥30 条 |
| `oracle-postgresql.json` | 同上 | ≥25 条 |
| `mysql-dameng.json` | 同上 | ≥25 条 |

### 4.5 MySQL→PostgreSQL 规则覆盖清单

- 内置函数（12条）：IFNULL, NOW, GROUP_CONCAT, DATE_FORMAT, UNIX_TIMESTAMP, CONCAT_WS, FIND_IN_SET, STR_TO_DATE, SLEEP, UUID, LAST_INSERT_ID, REGEXP
- 语法差异（6条）：LIMIT, 反引号, AUTO_INCREMENT, ENGINE, REPLACE INTO, SHOW TABLES
- 数据类型（5条）：DATETIME, TINYINT, MEDIUMTEXT, BLOB, UNSIGNED
- DDL差异（4条）：TEMPORARY TABLE, DROP IF EXISTS, ALTER MODIFY, RENAME TABLE
- 其他（3条）：USE, LOCK TABLES, 注释语法

## 验证标准

```bash
mvn test -pl dma-core -Dtest="RuleEngineTest,JsonFileRuleLoaderTest"
# 规则加载正确，规则引擎匹配准确
```

## 产出

- `JsonFileRuleLoader` + 单元测试
- `RuleEngine` + 单元测试
- `DefaultSqlCompatibilityAnalyzer` + 单元测试
- 3 个 JSON 规则文件（共约 80 条规则）
