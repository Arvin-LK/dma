# Database Migration Assistant (DMA)

数据库迁移与兼容性分析工具。支持 MySQL/Oracle/SQLServer 向 PostgreSQL/GaussDB/GoldenDB/OceanBase/达梦的 SQL 兼容性扫描与自动转换。

## 环境要求

- **JDK**: 必须使用 JDK 17，路径 `D:/DEV_Application/Java/jdk17`
- **编译命令**: `export JAVA_HOME="D:/DEV_Application/Java/jdk17" && mvn compile`
- **安装命令**: `export JAVA_HOME="D:/DEV_Application/Java/jdk17" && mvn install -DskipTests`
- **运行方式一（推荐）**: 双击 `run.bat`，自动编译安装并启动
- **运行方式二（IDEA）**: 先执行 `mvn install -DskipTests`，再右键 `DmaLauncher` → Run
- **运行方式三（Maven）**: `mvn javafx:run -pl dma-desktop`
- **API 端口**: Spring Boot 默认 `8080`，Swagger 未配置

## 项目结构速查

```
dma/
├── docs/            # 所有项目文档（需求/架构/规范/执行步骤/决策）
├── dev-logs/        # 每日开发日志
├── dma-common/      # 公共模块（枚举/异常/DTO/工具类）
├── dma-core/        # 核心模块（DDD四层：api/application/domain/infrastructure）
├── dma-desktop/     # JavaFX 桌面端
├── dma-idea-plugin/ # IDEA 插件（后续）
├── dma-test/        # 集成测试
├── CLAUDE.md        # 本文件 — AI 助手指引
└── pom.xml          # 父 POM
```

## 文档导航

### 需求文档（docs/requirements/）
- [产品简介](docs/requirements/product-brief.md) — 产品定位、目标用户、核心价值
- [MVP 范围定义](docs/requirements/mvp-scope.md) — 明确的"做"与"不做"清单
- [用户故事](docs/requirements/user-stories.md) — 按角色划分的用户故事
- [功能规格说明](docs/requirements/functional-spec.md) — 每个功能的输入/输出/边界条件

### 架构设计（docs/architecture/）
- [系统架构总览](docs/architecture/system-architecture.md) — 模块关系图、技术选型及理由
- [DDD 分层设计](docs/architecture/ddd-layer-design.md) — 四层职责边界、包结构、依赖规则
- [数据库设计](docs/architecture/database-design.md) — ER 图、建表 SQL、索引设计
- [API 设计规范](docs/architecture/api-design.md) — URL 命名、请求响应格式、错误码
- [扩展点设计](docs/architecture/extension-points.md) — SPI 接口列表、注册机制、扩展示例

### 开发规范（docs/standards/）
- [代码风格规范](docs/standards/code-style.md) — 命名约定、注释规范、包结构约定
- [Git 工作流](docs/standards/git-workflow.md) — 分支策略、commit message 格式
- [测试规范](docs/standards/testing-standard.md) — 单测/集成测试的划分与覆盖率要求
- [异常处理规范](docs/standards/error-handling.md) — 异常体系、错误码、日志规范
- [规则 JSON 格式规范](docs/standards/rule-json-spec.md) — 字段定义、示例、校验规则

### 执行步骤（docs/execution/）
- [开发路线图](docs/execution/development-roadmap.md) — 所有 Phase 总览、里程碑、依赖关系
- [Phase 1 项目初始化](docs/execution/phase-01-project-init.md)
- [Phase 2 公共模块](docs/execution/phase-02-common-module.md)
- [Phase 3 领域层](docs/execution/phase-03-domain-layer.md)
- [Phase 4 规则引擎](docs/execution/phase-04-rule-engine.md)
- [Phase 5 SQL解析转换](docs/execution/phase-05-sql-parser.md)
- [Phase 6 源码扫描](docs/execution/phase-06-source-scanner.md)
- [Phase 7 报告生成](docs/execution/phase-07-report-generator.md)
- [Phase 8 应用层](docs/execution/phase-08-application-layer.md)
- [Phase 9 API层](docs/execution/phase-09-api-layer.md)
- [Phase 10 桌面端](docs/execution/phase-10-desktop-ui.md)
- [Phase 11 集成测试](docs/execution/phase-11-integration-test.md)

### 技术决策（docs/decisions/）
- [ADR 模板](docs/decisions/adr-template.md) — 架构决策记录模板

### 开发日志（dev-logs/）
- [日志规范](dev-logs/README.md) — 日志格式和更新规则
- [每日日志模板](dev-logs/template.md) — 每日日志模板

---

## 工作流指引

### 开始新 Phase 前
1. 阅读 [开发路线图](docs/execution/development-roadmap.md) 了解当前阶段在整个项目中的位置
2. 阅读对应的 `docs/execution/phase-XX-*.md` 了解本 Phase 的详细步骤
3. 阅读相关的 `docs/architecture/` 和 `docs/standards/` 文档确保理解规范

### 编码过程中
- 严格遵循 [代码风格规范](docs/standards/code-style.md)
- 每实现一个类，立即编写对应的单元测试（遵循 [测试规范](docs/standards/testing-standard.md)）
- 异常处理遵循 [异常处理规范](docs/standards/error-handling.md)
- 新增规则 JSON 遵循 [规则 JSON 格式规范](docs/standards/rule-json-spec.md)
- 需要做技术选型决策时，先在 `docs/decisions/` 中创建 ADR

### 每个 Phase 结束时
1. 确保 `mvn clean compile` 全量通过
2. 确保本 Phase 相关的所有测试通过
3. 更新 `dev-logs/YYYY-MM-DD.md` 记录完成情况
4. 更新 [开发路线图](docs/execution/development-roadmap.md) 中的进度

### 每日结束前
- 按模板格式更新 `dev-logs/YYYY-MM-DD.md`（今日完成、遇到的问题、明日计划、当前风险、项目状态）

---

## 禁止事项

- ❌ 不允许跨 Phase 开发（如 Phase 2 未完成就做 Phase 4 的内容）
- ❌ 不允许跳过测试（每个类的单测必须与类本身同时交付）
- ❌ 不允许直接修改父 POM 中未涉及的模块依赖
- ❌ 不允许引入计划外的第三方依赖（需先在 `docs/decisions/` 中记录决策）
- ❌ 不允许使用 `System.out.println` 替代日志框架（使用 SLF4J）
- ❌ 不允许硬编码中文错误消息（使用 i18n properties 资源文件）
- ❌ 不允许在没有对应 `docs/execution/phase-XX-*.md` 的情况下开始新 Phase

---

## 关键决策记录

| 决策 | 结论 | 记录位置 |
|------|------|---------|
| MVP 数据库范围 | MySQL→PG, Oracle→PG, MySQL→达梦 | [MVP 范围定义](docs/requirements/mvp-scope.md) |
| 产品形态优先级 | 先 JavaFX 桌面端，IDEA 插件后续 | [MVP 范围定义](docs/requirements/mvp-scope.md) |
| AI 接入方式 | 云端 API 优先，MVP 预留接口 | [系统架构总览](docs/architecture/system-architecture.md) |
| 商业化路径 | IDEA 插件免费获客，桌面端/高级功能付费 | [系统架构总览](docs/architecture/system-architecture.md) |
| 规则存储 | MVP 用 JSON 文件，后续迁移至 SQLite | [数据库设计](docs/architecture/database-design.md) |
