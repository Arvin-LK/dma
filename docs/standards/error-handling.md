# 异常处理规范

## 异常体系

```
RuntimeException
  └── DmaException (基础异常)
        ├── ConnectionException          # 数据库连接相关
        │     ├── ConnectionTimeoutException
        │     └── AuthenticationException
        ├── ParseException               # SQL 解析相关
        ├── RuleException                # 规则相关
        │     ├── RuleNotFoundException
        │     └── RuleFormatException
        ├── ConversionException          # SQL 转换相关
        ├── ScanException                # 源码扫描相关
        └── ReportGenerationException    # 报告生成相关
```

## 异常设计原则

1. **所有自定义异常继承 `DmaException`**
2. **异常携带错误码和国际化消息键**
3. **只在真正的异常情况下抛出异常，避免用异常控制业务流程**
4. **捕获第三方异常并包装为自定义异常**（除非能真正处理）

---

## 异常类规范

```java
// 基础异常
public class DmaException extends RuntimeException {
    private final String errorCode;       // 业务错误码
    private final String messageKey;      // i18n 消息键
    private final Object[] args;           // 消息参数

    public DmaException(String errorCode, String messageKey, Object... args) {
        super(MessageUtils.getMessage(messageKey, args));
        this.errorCode = errorCode;
        this.messageKey = messageKey;
        this.args = args;
    }
}

// 子异常示例
public class ConnectionException extends DmaException {
    public ConnectionException(String errorCode, String messageKey, Object... args) {
        super(errorCode, messageKey, args);
    }
}

// 使用时
throw new ConnectionException(
    "CONN_001",
    "error.connection.failed",
    host, port
);
```

---

## 错误码规范

| 前缀 | 模块 | 示例 |
|------|------|------|
| `CONN_` | 数据库连接 | `CONN_001` 连接失败 |
| `PARSE_` | SQL 解析 | `PARSE_001` 语法错误 |
| `RULE_` | 规则引擎 | `RULE_001` 规则文件未找到 |
| `SCAN_` | 源码扫描 | `SCAN_001` 项目路径不存在 |
| `CONV_` | SQL 转换 | `CONV_001` 转换策略未找到 |
| `REPORT_` | 报告生成 | `REPORT_001` 模板渲染失败 |
| `TASK_` | 迁移任务 | `TASK_001` 任务不存在 |
| `SYS_` | 系统通用 | `SYS_001` 内部错误 |

---

## Controller 层异常处理

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DmaException.class)
    public ResponseEntity<ApiResponse<Void>> handleDmaException(DmaException ex) {
        log.error("Business exception: code={}, message={}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity
            .status(mapHttpStatus(ex.getErrorCode()))
            .body(ApiResponse.error(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknownException(Exception ex) {
        log.error("Unknown exception", ex);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("SYS_001", "系统内部错误"));
    }
}
```

---

## 日志规范

### 日志框架

使用 **SLF4J + Logback**（Spring Boot 默认）。

### 日志级别使用

| 级别 | 使用场景 |
|------|---------|
| `ERROR` | 系统错误、需要人工介入的问题 |
| `WARN` | 潜在问题、降级处理、可恢复的异常 |
| `INFO` | 关键业务流程节点（任务创建、扫描完成等） |
| `DEBUG` | 调试信息、详细的处理步骤 |
| `TRACE` | 非常详细的诊断信息（不用于生产） |

### 日志格式

```java
// ✅ 使用占位符（SLF4J 风格）
log.info("Migration task started: taskId={}, source={}, target={}", taskId, source, target);

// ✅ 异常日志必须包含堆栈
log.error("SQL parsing failed for: {}", sql, exception);

// ❌ 禁止字符串拼接
log.info("Migration task started: " + taskId);  // ❌

// ❌ 禁止 e.printStackTrace()
e.printStackTrace();  // ❌ 使用 log.error("msg", e) 代替
```

### 日志内容要求

- 包含关键业务标识（taskId, ruleCode 等）
- 不记录敏感信息（密码、完整连接串）
- INFO 级别日志应能还原核心业务流程
