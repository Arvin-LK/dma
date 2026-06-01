# Phase 5: SQL 解析与转换引擎

## 目标

基于 JSQLParser 实现 SQL AST 解析 + 策略模式转换器。

## 前置条件

- [ ] Phase 4 完成（规则引擎可用，JSON 规则就绪）

## 步骤

### 5.1 JsqlParserWrapper
- 封装 JSQLParser 解析（`CCJSqlParserUtil.parse()`）
- 封装 AST 遍历（`TablesNamesFinder`, `ExpressionVisitor` 等）
- 封装 SQL 格式化输出
- 异常时降级为 Druid 解析

### 5.2 转换策略实现（7 个）

| 策略 | 处理内容 |
|------|---------|
| `FunctionNameConverter` | IFNULL→COALESCE, NOW→CURRENT_TIMESTAMP 等函数名替换 |
| `LimitClauseConverter` | LIMIT offset,count → LIMIT count OFFSET offset |
| `DataTypeConverter` | DATETIME→TIMESTAMP 等 DDL 类型映射 |
| `IdentifierQuoteConverter` | 反引号→双引号 |
| `NvlToCoalesceConverter` | NVL(a,b)→COALESCE(a,b) |
| `AutoIncrementConverter` | AUTO_INCREMENT→SERIAL |
| `PaginationConverter` | SELECT TOP n→LIMIT 等分页语法 |

### 5.3 SqlConvertManager
- 策略注册与发现（自动扫描 Spring Bean）
- 责任链调度（逐个策略尝试，直到找到支持的）
- 复合转换（一条 SQL 可能匹配多个规则，按顺序应用）

### 5.4 DefaultSqlConverter
- 实现 `SqlConverter` 领域服务接口
- 调用 RuleEngine 匹配规则 + SqlConvertManager 执行转换

## 验证标准

每个转换策略有 ≥3 个单元测试，集成测试覆盖复合 SQL 转换。
