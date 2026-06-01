# 代码风格规范

## 命名约定

### 包命名

- 全小写，使用单数形式
- 基础包：`com.dma.<module>`
- 子包按功能或层次划分，不使用下划线

```
✅ com.dma.core.domain.model.connection
✅ com.dma.core.infrastructure.parser
❌ com.dma.core.domain.model.dataBaseConnection
❌ com.dma.core.infrastructure.sql_parser
```

### 类命名

| 类型 | 约定 | 示例 |
|------|------|------|
| 实体/聚合根 | 名词，无后缀 | `MigrationTask`, `DatabaseConnection` |
| 值对象 | 名词，描述性 | `RuleCode`, `RiskScore`, `ConnectionId` |
| 领域服务接口 | 名词 + 动词或 `*Service` | `SqlCompatibilityAnalyzer`, `RuleLoader` |
| 领域服务实现 | `Default*` 或具体描述 | `DefaultSqlConverter` |
| 应用服务 | `*ApplicationService` | `MigrationApplicationService` |
| Controller | `*Controller` | `ConnectionController` |
| Repository 接口 | `*Repository` | `MigrationRuleRepository` |
| Repository 实现 | `Sqlite*Repository` | `SqliteMigrationRuleRepository` |
| 策略/转换器 | `*Converter`, `*Strategy` | `FunctionNameConverter` |
| 枚举 | 单数名词 | `DatabaseType`, `CompatibilityLevel` |
| 异常 | `*Exception` | `ConnectionException`, `ParseException` |
| 工具类 | `*Utils`, `*Helper` | `SqlNormalizer` |
| 工厂类 | `*Factory` | `ReportGeneratorFactory` |

### 方法命名

| 类型 | 约定 | 示例 |
|------|------|------|
| 查询单个 | `find*` | `findById()`, `findByCode()` |
| 查询列表 | `find*` (返回 List) | `findBySourceAndTarget()` |
| 保存 | `save*` | `save()`, `saveAll()` |
| 删除 | `delete*` | `deleteById()` |
| 创建/生成 | `create*`, `generate*` | `createTask()`, `generateReport()` |
| 执行 | `execute*`, `run*` | `executeScan()` |
| 校验/判断 | `is*`, `can*`, `validate*` | `isEnabled()`, `canConvert()` |
| 转换 | `convert*` | `convertSql()`, `convertDataType()` |

### 变量命名

- 局部变量和字段使用 camelCase
- 常量使用 UPPER_SNAKE_CASE
- Boolean 类型变量以 `is`/`has`/`can`/`should` 开头

---

## 注释规范

### 类注释（必须）

```java
/**
 * 兼容性规则聚合根。
 * 
 * 表示一条从源数据库到目标数据库的 SQL 兼容性转换规则。
 * 每条规则包含匹配模式和替换模式，由规则引擎加载和匹配。
 *
 * @author DMA Team
 * @since 1.0.0
 */
public class MigrationRule { ... }
```

### 公共方法注释（必须）

```java
/**
 * 根据源数据库和目标数据库类型加载规则。
 *
 * @param source 源数据库类型
 * @param target 目标数据库类型
 * @return 匹配的规则列表，如果没有则返回空列表
 * @throws RuleLoadException 如果规则文件格式错误
 */
public List<MigrationRule> load(DatabaseType source, DatabaseType target) { ... }
```

### 不需要注释的情况

- 自解释的 getter/setter（Lombok 生成）
- 标准 override 方法（如 `toString()`）
- 参数名已经充分表达意图的简单方法

---

## 代码格式

- **缩进**: 4 个空格（不使用 Tab）
- **行宽**: 120 字符
- **大括号**: K&R 风格（左括号不换行）
- **import**: 不使用通配符 `*`
- **空行**: 逻辑段落间使用一个空行分隔
- **文件末尾**: 一个空行

---

## 最佳实践

### 使用 Lombok（减少样板代码）

```java
// ✅ 推荐
@Getter
@AllArgsConstructor
public class RuleCode {
    private final String value;
}

// ❌ 避免手写 getter/setter/constructor
```

### 使用 final

```java
// ✅ 方法参数和局部变量默认使用 final
public void process(final String sql) {
    final var parser = new CCJSqlParserUtil();
}

// ✅ 值对象字段使用 final
@Getter
public class RiskScore {
    private final int value;
}
```

### 使用 record（Java 17+）

```java
// ✅ 简单的值对象和 DTO 优先使用 record
public record ConnectionConfigDto(
    String name,
    String dbType,
    String host,
    int port
) {}
```

### 避免的做法

```java
// ❌ 不要返回 null 集合，返回空集合
public List<Rule> findAll() {
    return Collections.emptyList(); // 而非 null
}

// ❌ 不要使用通配符 import
import java.util.*;  // ❌

// ❌ 不要在 lambda 中修改外部变量
List<String> list = new ArrayList<>();
items.forEach(i -> list.add(i)); // ❌ 用 collect(Collectors.toList())
```

---

## 包导入顺序

1. `java.*` 和 `javax.*`
2. 第三方库（按字母序）
3. 项目内部包（`com.dma.*`）
4. `static` 导入（最末尾）

各组之间用空行分隔。
