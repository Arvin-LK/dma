# Phase 2: dma-common 公共模块

## 目标

完成所有枚举类、异常体系、基础 DTO、工具类。

## 前置条件

- [ ] Phase 1 完成（Maven 多模块可编译）

## 步骤

### 2.1 枚举类

| 类名 | 包 | 值 |
|------|-----|-----|
| `DatabaseType` | `enums` | MYSQL, ORACLE, SQLSERVER, POSTGRESQL, GAUSSDB, GOLDENDB, OCEANBASE, DAMENG |
| `CompatibilityLevel` | `enums` | COMPATIBLE, AUTO_CONVERTIBLE, MANUAL_REVIEW, INCOMPATIBLE, PARSE_ERROR |
| `Severity` | `enums` | ERROR, WARNING, INFO |
| `PatternType` | `enums` | FUNCTION, SYNTAX, DATATYPE, KEYWORD, BUILTIN |
| `ReportFormat` | `enums` | HTML, PDF, WORD |
| `TaskStatus` | `enums` | PENDING, RUNNING, COMPLETED, FAILED |

### 2.2 异常体系

| 类名 | 父类 | 说明 |
|------|------|------|
| `DmaException` | RuntimeException | 基础异常，包含 errorCode + messageKey |
| `ConnectionException` | DmaException | 连接相关 |
| `ConnectionTimeoutException` | ConnectionException | 连接超时 |
| `AuthenticationException` | ConnectionException | 认证失败 |
| `ParseException` | DmaException | SQL 解析失败 |
| `RuleException` | DmaException | 规则相关 |
| `RuleNotFoundException` | RuleException | 规则未找到 |
| `RuleFormatException` | RuleException | 规则格式错误 |
| `ConversionException` | DmaException | 转换失败 |
| `ScanException` | DmaException | 扫描失败 |
| `ReportGenerationException` | DmaException | 报告生成失败 |

### 2.3 基础 DTO

- `ApiResponse<T>` — 统一响应包装
- `PageResult<T>` — 分页结果
- `ConnectionConfigDto` — 连接配置

### 2.4 工具类

- `SqlNormalizer` — SQL 空白规范化、注释移除
- `FileUtils` — 文件读写、扩展名检测

## 验证标准

```bash
mvn clean test -pl dma-common
# 所有工具类的单元测试通过
```

## 产出

- 6 个枚举类
- 10 个异常类
- 3 个基础 DTO
- 2 个工具类 + 单元测试
