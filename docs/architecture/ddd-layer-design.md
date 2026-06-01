# DDD 四层设计规范

## 分层架构总览

```
┌─────────────────────────────────────────────────┐
│                   API 层 (接口层)                  │
│  Controller / DTO / 请求校验                      │
│  职责: 接收请求、参数校验、调用应用层、返回响应      │
├─────────────────────────────────────────────────┤
│                 Application 层 (应用层)           │
│  Application Service / Pipeline / Event          │
│  职责: 编排业务流程、事务管理、调用领域服务         │
├─────────────────────────────────────────────────┤
│                  Domain 层 (领域层)                │
│  Entity / Value Object / Aggregate Root          │
│  Repository Interface / Domain Service Interface │
│  职责: 核心业务逻辑、领域规则、不依赖任何框架       │
├─────────────────────────────────────────────────┤
│              Infrastructure 层 (基础设施层)       │
│  Repository Impl / Parser / Converter / Config   │
│  职责: 技术实现、外部依赖适配、框架集成            │
└─────────────────────────────────────────────────┘
```

## 各层详细规范

### 1. API 层（接口层）

**包路径**: `com.dma.core.api`

| 元素 | 约定 | 示例 |
|------|------|------|
| Controller | `*Controller.java`，使用 `@RestController` | `ConnectionController` |
| DTO | `*Request.java` / `*Response.java` | `ScanRequest`, `ScanResponse` |
| 校验 | 使用 `jakarta.validation` 注解 | `@NotBlank`, `@NotNull` |
| 异常处理 | 全局 `@ControllerAdvice` | `GlobalExceptionHandler` |

**规则**：
- Controller 不包含任何业务逻辑，只做参数校验和调用应用服务
- 每个 Controller 方法不超过 15 行
- 统一使用 `ApiResponse<T>` 包装返回值

### 2. Application 层（应用层）

**包路径**: `com.dma.core.application`

| 元素 | 约定 | 示例 |
|------|------|------|
| Service | `*ApplicationService.java`，使用 `@Service` | `MigrationApplicationService` |
| Pipeline | `*Pipeline.java` | `MigrationPipeline` |
| DTO | `*Command.java` / `*Query.java` | `CreateTaskCommand` |

**规则**：
- 应用服务负责事务管理和流程编排
- 不包含领域逻辑（领域逻辑在 Domain 层）
- 一个应用服务方法通常对应一个用例/用户故事
- 跨聚合的操作在应用层协调

### 3. Domain 层（领域层）

**包路径**: `com.dma.core.domain`

| 元素 | 约定 | 示例 |
|------|------|------|
| 聚合根 | 使用 `@Entity` 概念（非 JPA），自管理一致性 | `MigrationTask` |
| 实体 | 有唯一标识，可变 | `ScanResult` |
| 值对象 | 无标识，不可变，通过值相等比较 | `RuleCode`, `RiskScore` |
| 仓储接口 | `*Repository.java`，只定义接口 | `MigrationRuleRepository` |
| 领域服务 | `*Service.java`（无状态），或 `*Analyzer`/`*Converter` | `SqlCompatibilityAnalyzer` |
| 领域事件 | `*Event.java` | `TaskCompletedEvent` |

**规则**：
- 领域层不依赖任何框架（Spring、Hibernate 等）
- 领域层不依赖 Infrastructure 层
- 仓储接口在领域层定义，实现在基础设施层
- 值对象必须不可变（所有字段 final，无 setter）
- 聚合根负责维护内部一致性

### 4. Infrastructure 层（基础设施层）

**包路径**: `com.dma.core.infrastructure`

| 元素 | 约定 | 示例 |
|------|------|------|
| 仓储实现 | `Sqlite*Repository.java` | `SqliteMigrationRuleRepository` |
| 解析器 | `*Parser.java`, `*Wrapper.java` | `JsqlParserWrapper` |
| 转换器 | `*Converter.java`, `*Strategy.java` | `FunctionNameConverter` |
| 扫描器 | `*Scanner.java` | `JavaSourceScanner` |
| 规则引擎 | `RuleEngine.java`, `*RuleLoader.java` | `JsonFileRuleLoader` |
| 报告 | `*ReportGenerator.java` | `HtmlReportGenerator` |
| 配置 | `*Config.java`, `*Properties.java` | `DmaConfig` |

**规则**：
- 基础设施层实现领域层定义的接口
- 可以使用任何第三方库和框架
- 通过 Spring `@Service` / `@Component` / `@Repository` 注册为 Bean

---

## 依赖方向（核心原则）

```
API 层 ──→ Application 层 ──→ Domain 层 ←── Infrastructure 层
                                       ↑          │
                                       └──────────┘
                                     Infrastructure 实现
                                     Domain 层定义的接口
```

- **Domain 层**：不依赖任何层（最内层，最稳定）
- **Application 层**：依赖 Domain 层
- **API 层**：依赖 Application 层（也允许直接使用 Domain DTO）
- **Infrastructure 层**：依赖 Domain 层（实现其接口）

---

## 包结构约定

```
com.dma.core
├── api
│   ├── controller          # REST Controller
│   └── dto                  # API 专用 DTO
├── application
│   ├── service             # Application Service
│   └── pipeline            # 业务流程编排
├── domain
│   ├── model               # 实体、值对象、聚合根
│   │   ├── connection
│   │   ├── migration
│   │   ├── rule
│   │   ├── scanner
│   │   └── report
│   ├── service             # 领域服务接口
│   └── repository          # 仓储接口
└── infrastructure
    ├── repository          # 仓储实现
    ├── parser              # SQL 解析器
    ├── converter           # SQL 转换器
    ├── scanner             # 源码扫描器
    ├── engine              # 规则引擎
    ├── report              # 报告生成器
    ├── ai                  # AI 适配器
    ├── license             # 许可证（预留）
    └── config              # Spring 配置
```

---

## 反模式（禁止做法）

- ❌ Controller 中包含业务逻辑
- ❌ Domain 层 import Spring 注解
- ❌ Application Service 直接操作数据库（必须通过 Repository 接口）
- ❌ 跨聚合的直接引用（通过 ID 引用其他聚合）
- ❌ 值对象有 setter 方法
