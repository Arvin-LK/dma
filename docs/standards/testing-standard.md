# 测试规范

## 测试金字塔

```
         ╱─────╲
        ╱  E2E  ╲         少量：端到端流程验证
       ╱─────────╲
      ╱ 集成测试   ╲        适量：模块间交互、数据库操作
     ╱─────────────╲
    ╱   单元测试     ╲      大量：每个类、每个方法
   ╱─────────────────╲
```

---

## 单元测试规范

### 基本要求

| 要求 | 说明 |
|------|------|
| **框架** | JUnit 5 + Mockito |
| **命名** | `{ClassName}Test.java`，放在 `src/test/java` 对称路径下 |
| **覆盖率目标** | 整体 ≥ 70%，领域层 ≥ 85% |
| **每个 public 方法** | 至少 1 个正常路径测试 + 边界条件测试 |
| **每个转换策略** | 至少 3 个测试：正例、边界值、不支持的情况 |

### 测试方法命名

采用 `should_预期行为_when_条件` 格式：

```java
@Test
void should_returnAutoConvertible_when_ifnullFunctionDetected() { ... }

@Test
void should_throwParseException_when_sqlSyntaxInvalid() { ... }

@Test
void should_returnEmptyList_when_sqlIsCompatible() { ... }
```

### 测试结构（AAA 模式）

```java
@Test
void should_convertIfnullToCoalesce() {
    // Arrange — 准备数据
    var converter = new FunctionNameConverter();
    var rule = MigrationRule.builder()
        .code(RuleCode.of("M2PG-FN-001"))
        .matchPattern(MatchPattern.of("IFNULL(${expr1}, ${expr2})"))
        .replacementPattern(ReplacementPattern.of("COALESCE(${expr1}, ${expr2})"))
        .build();
    var sql = "SELECT IFNULL(name, 'N/A') FROM users;";

    // Act — 执行操作
    var result = converter.convert(sql, rule);

    // Assert — 验证结果
    assertThat(result).isEqualTo("SELECT COALESCE(name, 'N/A') FROM users;");
}
```

### Mock 使用规范

- 只 Mock 外部依赖（数据库、文件系统、网络）
- 不 Mock 领域对象（值对象、实体）
- 不 Mock 同一模块内的其他类

---

## 集成测试规范

### 基本要求

| 要求 | 说明 |
|------|------|
| **命名** | `*IntegrationTest.java` 或 `*IT.java` |
| **位置** | `dma-test/src/test/java/` |
| **数据库** | 使用临时 SQLite 文件（测试结束自动删除） |
| **Spring 上下文** | 使用 `@SpringBootTest` |

### 必须覆盖的集成场景

1. **规则加载** — JSON 文件 → `JsonFileRuleLoader` → 正确的 `MigrationRule` 列表
2. **SQL 扫描** — 输入测试 SQL → 规则引擎 → 正确的兼容性结果
3. **SQL 转换** — 输入不兼容 SQL → 转换策略 → 正确的转换结果
4. **源码扫描** — 测试项目目录 → 扫描器 → 正确提取 SQL 片段
5. **报告生成** — 测试数据 → 三种格式报告 → 文件内容正确
6. **端到端流程** — 创建任务 → 执行扫描 → 生成报告

### 测试数据（Fixtures）

测试用的 SQL / XML / Java 文件统一放在：
```
dma-test/src/test/resources/fixtures/
├── sql/
│   ├── mysql/
│   │   ├── functions.sql      # 函数相关 SQL
│   │   ├── syntax.sql         # 语法相关 SQL
│   │   ├── datatypes.sql      # 数据类型相关 SQL
│   │   └── ddl.sql            # DDL 相关 SQL
│   ├── oracle/
│   │   └── functions.sql
│   └── compatible/
│       └── standard.sql       # 应该无问题通过的 SQL
├── xml/
│   └── UserMapper.xml         # 测试用 Mapper XML
└── java/
    └── UserService.java       # 测试用 Java 源码（含 SQL 拼接）
```

---

## 测试禁止事项

- ❌ 不允许依赖测试执行顺序（每个测试独立）
- ❌ 不允许测试间共享可变状态
- ❌ 不允许在测试中硬编码本地路径
- ❌ 不允许忽略测试失败（`@Disabled` 必须有明确理由和 issue 链接）
- ❌ 不允许测试方法抛出 Exception 而不做断言
- ❌ 不允许在单元测试中使用 `Thread.sleep()`
