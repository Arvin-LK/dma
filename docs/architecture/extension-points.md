# 扩展点设计

## SPI 接口总览

DMA 设计了 7 个核心 SPI（Service Provider Interface）扩展点，支持在不修改核心代码的情况下扩展功能。

```
                   ┌─────────────────────┐
                   │     DMA Core         │
                   │  (只依赖接口)         │
                   └──────┬──────┬───────┘
                          │      │
          ┌───────────────┤      ├───────────────┐
          ▼               ▼      ▼               ▼
   ┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────────┐
   │RuleLoader│   │SqlConvert│   │SourceCode│   │ReportGen │
   │          │   │Strategy  │   │Scanner   │   │erator    │
   └──────────┘   └──────────┘   └──────────┘   └──────────┘
          ▲               ▲      ▲               ▲
          │               │      │               │
   ┌──────┴──────┐ ┌──────┴──────┐ ┌──────┴──────┐ ┌──────┴──────┐
   │JSON  SQLite │ │Func Limit  │ │Java XML     │ │HTML PDF Word│
   │File  Remote │ │DataType NVL│ │Kotlin ...   │ │CSV  Markdown│
   └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘
```

---

## 1. RuleLoader — 规则加载器

**接口路径**: `com.dma.core.domain.service.RuleLoader`

```java
public interface RuleLoader {
    /**
     * 加载指定源→目标的兼容性规则
     * @return 规则列表，如果没有匹配规则返回空列表
     */
    List<MigrationRule> load(DatabaseType source, DatabaseType target);

    /**
     * 返回此加载器的优先级（数值越小优先级越高）
     */
    default int getPriority() { return 100; }

    /**
     * 是否支持从此外部源动态加载（热更新）
     */
    default boolean supportsHotReload() { return false; }
}
```

**内置实现**：

| 实现类 | 优先级 | 说明 |
|--------|--------|------|
| `JsonFileRuleLoader` | 10 | 从 classpath:rules/*.json 加载内置规则 |
| `ExternalFileRuleLoader` | 20 | 从用户指定目录加载自定义 JSON 规则 |
| `SqliteRuleLoader` | 30 | 从 SQLite 数据库加载规则（后续） |
| `RemoteRuleLoader` | 40 | 从远程规则市场加载（商业化阶段） |

**扩展方式**：实现 `RuleLoader` 接口，注册为 Spring Bean，自动被 `RuleEngine` 发现。

---

## 2. SqlConvertStrategy — SQL 转换策略

**接口路径**: `com.dma.core.domain.service.SqlConvertStrategy`

```java
public interface SqlConvertStrategy {
    /**
     * 判断此策略是否支持处理该兼容性问题
     */
    boolean supports(CompatibilityResult issue);

    /**
     * 执行转换，返回转换后的 SQL
     */
    String convert(String sql, MigrationRule rule);

    /**
     * 策略名称（用于日志和调试）
     */
    default String getName() { return getClass().getSimpleName(); }
}
```

**内置实现**：

| 实现类 | 处理的 PatternType |
|--------|-------------------|
| `FunctionNameConverter` | FUNCTION |
| `LimitClauseConverter` | SYNTAX (LIMIT) |
| `DataTypeConverter` | DATATYPE |
| `IdentifierQuoteConverter` | SYNTAX (标识符) |
| `NvlToCoalesceConverter` | FUNCTION (NVL) |
| `AutoIncrementConverter` | DDL (自增) |
| `StringConcatConverter` | FUNCTION (字符串拼接) |
| `PaginationConverter` | SYNTAX (TOP/FETCH) |

**扩展方式**：实现 `SqlConvertStrategy` 接口，注册为 Spring Bean。`SqlConvertManager` 自动发现并使用。

---

## 3. SourceCodeScanner — 源码扫描器

**接口路径**: `com.dma.core.domain.service.SourceCodeScanner`

```java
public interface SourceCodeScanner {
    /**
     * 扫描项目目录，提取所有 SQL 片段
     */
    List<ScanResult> scanProject(Path projectPath, DatabaseType source, DatabaseType target);

    /**
     * 此扫描器支持的文件扩展名
     */
    Set<String> supportedExtensions();
}
```

**内置实现**：

| 实现类 | 扫描目标 |
|--------|---------|
| `JavaSourceScanner` | .java 文件中的 SQL 字符串 |
| `MyBatisXmlScanner` | *Mapper.xml 中的 SQL |
| (后续) `KotlinSourceScanner` | .kt 文件 |
| (后续) `SqlFileScanner` | .sql 文件直接扫描 |

**扩展方式**：实现 `SourceCodeScanner` 接口，注册为 Spring Bean。

---

## 4. ReportGenerator — 报告生成器

**接口路径**: `com.dma.core.domain.service.ReportGenerator`

```java
public interface ReportGenerator {
    /**
     * 生成报告
     * @return 报告文件的字节数组
     */
    byte[] generate(MigrationTask task, List<ScanResult> results);

    /**
     * 此生成器支持的格式
     */
    ReportFormat supportedFormat();

    /**
     * 生成的文件扩展名
     */
    String fileExtension();
}
```

**内置实现**：

| 实现类 | 格式 |
|--------|------|
| `HtmlReportGenerator` | HTML |
| `PdfReportGenerator` | PDF |
| `WordReportGenerator` | WORD (.docx) |

**扩展方式**：实现 `ReportGenerator` 接口，注册为 Spring Bean。如需新增格式（如 Markdown、CSV），添加新实现即可。

---

## 5. AiAdvisor — AI 迁移顾问

**接口路径**: `com.dma.core.domain.service.AiAdvisor`

```java
public interface AiAdvisor {
    /**
     * 根据兼容性问题生成自然语言建议
     */
    String generateAdvice(CompatibilityResult result);

    /**
     * 根据所有问题生成完整的迁移计划
     */
    MigrationPlan generateMigrationPlan(List<CompatibilityResult> issues);

    /**
     * 批量转换复杂存储过程（Phase 2+）
     */
    String convertStoredProcedure(String procedureBody, DatabaseType source, DatabaseType target);
}
```

**内置实现**：

| 实现类 | 说明 |
|--------|------|
| `NoOpAiAdvisor` | MVP 阶段空实现，返回固定提示 |
| `OpenAiAdvisor` (后续) | 对接 OpenAI API |
| `ClaudeAiAdvisor` (后续) | 对接 Anthropic Claude API |
| `QwenAiAdvisor` (后续) | 对接通义千问 API |

**扩展方式**：实现 `AiAdvisor` 接口，通过配置 `dma.ai.provider` 切换实现。

---

## 6. LicenseManager — 许可证管理（商业化预留）

**接口路径**: `com.dma.core.domain.service.LicenseManager`

```java
public interface LicenseManager {
    boolean isFeatureEnabled(Feature feature);
    LicenseInfo getLicenseInfo();
    boolean validateLicense(String licenseKey);
}
```

**MVP 实现**: `OpenSourceLicenseManager` — 所有功能默认开启。

---

## 7. 注册机制

所有 SPI 接口的实现类，只需添加 Spring 注解即可自动注册：

```java
@Component
public class MyCustomConverter implements SqlConvertStrategy {
    // 自动被 SqlConvertManager 发现和使用
}
```

对于需要优先级排序的接口（如 `RuleLoader`、`SqlConvertStrategy`），通过 `getPriority()` 方法或 Spring `@Order` 注解控制顺序。

---

## 扩展新数据库示例

以新增 **SQLServer → GaussDB** 迁移路径为例：

1. 在 `DatabaseType` 枚举中确认 SQLSERVER 和 GAUSSDB 已存在
2. 创建 `sqlserver-gaussdb.json` 规则文件，放到 `resources/rules/`
3. 规则引擎自动加载新文件，无需改代码
4. 如果有 JSON 规则无法覆盖的复杂转换，实现新的 `SqlConvertStrategy` 并注册为 Bean

**预计工作量**：编写 1 个 JSON 文件（约 30 条规则）+ 可选 2-3 个转换策略类。
