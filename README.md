# Database Migration Assistant (DMA) 数据库迁移助手

[![Java 17](https://img.shields.io/badge/Java-17-orange)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot 3.2](https://img.shields.io/badge/Spring%20Boot-3.2-green)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue)](LICENSE)

> 支持 MySQL / Oracle / SQLServer 向 PostgreSQL / GaussDB / GoldenDB / OceanBase / 达梦 的数据库迁移与兼容性分析工具。

---

## 目录

- [功能总览](#功能总览)
- [项目架构](#项目架构)
- [核心模块](#核心模块)
- [技术栈](#技术栈)
- [快速开始](#快速开始)
- [四大核心功能](#四大核心功能)
- [API 文档](#api-文档)
- [规则体系](#规则体系)
- [扩展开发](#扩展开发)
- [后续规划](#后续规划)

---

## 功能总览

| 功能模块 | 说明 | 状态 |
|---------|------|------|
| 🏥 **数据库体检** | 连接源库 → 提取对象 → 兼容性分析 → 兼容率统计 | ✅ |
| 🔄 **SQL 自动转换** | 输入 SQL → 规则匹配 → 自动转换 → 原SQL/新SQL对比 | ✅ |
| 📦 **存储过程迁移** | PROCEDURE/FUNCTION/TRIGGER/VIEW DDL 结构性转换 | ✅ |
| 📂 **项目源码扫描** | Java/XML/SQL 文件全量扫描 + 保留关键字检测 + 风险分级 | ✅ |
| 📄 **报告导出** | HTML/PDF/Word 迁移报告生成 | ✅ |
| 🤖 **AI 迁移顾问** | 大模型辅助迁移建议（接口已预留） | 🔜 |
| 🔌 **IDEA 插件** | IntelliJ IDEA 内实时检测 + Alt+Enter 修复 | 🔜 |

---

## 项目架构

```
dma/
├── README.md                        # 本文件
├── CLAUDE.md                        # AI 助手指引
├── run.bat                          # Windows 一键启动脚本
├── pom.xml                          # 父 POM（Spring Boot 3.2 + Java 17）
│
├── docs/                            # 📚 项目文档体系
│   ├── requirements/                # 需求文档（产品简介/MVP范围/用户故事/功能规格）
│   ├── architecture/                # 架构设计（系统架构/DDD分层/数据库设计/API设计/扩展点）
│   ├── standards/                   # 开发规范（代码风格/Git工作流/测试规范/异常处理/规则JSON）
│   ├── execution/                   # 执行步骤（开发路线图 + Phase 1~11 详细步骤）
│   └── decisions/                   # 技术决策记录（ADR 模板）
│
├── dev-logs/                        # 📝 每日开发日志
│
├── dma-common/                      # 🔧 公共模块（不依赖任何内部模块）
│   └── src/main/java/com/dma/common/
│       ├── enums/                   # DatabaseType, CompatibilityLevel, Severity, PatternType...
│       ├── dto/                     # ApiResponse, PageResult, ScanSummary, DatabaseObjectInfo...
│       ├── exception/               # DmaException → Connection/Parse/Rule/Conversion/Scan...
│       └── util/                    # SqlNormalizer, FileUtils
│
├── dma-core/                        # 🧠 核心模块（DDD 四层架构）
│   └── src/main/java/com/dma/core/
│       ├── api/controller/          # ── 接口层 ──
│       │   ├── ConnectionController   POST/GET/DELETE /api/v1/connections
│       │   ├── ScanController         POST /api/v1/scan/sql | /project | /project-full
│       │   ├── ScanDatabaseController POST /api/v1/scan/database | /schemas
│       │   ├── ProcedureController    POST /api/v1/convert/procedure
│       │   ├── TaskController         POST/GET /api/v1/tasks
│       │   └── SystemController       GET /api/v1/system/health | /info
│       │
│       ├── application/service/     # ── 应用层（业务编排）──
│       │   ├── ConnectionApplicationService
│       │   ├── DatabaseScanService      # 连库→提取→分析→统计
│       │   ├── MigrationApplicationService
│       │   └── ProjectScanService       # 多扫描器调度 + 风险分级
│       │
│       ├── domain/                  # ── 领域层（核心逻辑，不依赖框架）──
│       │   ├── model/               # 聚合根/实体/值对象
│       │   │   ├── connection/      # DatabaseConnection, ConnectionId
│       │   │   ├── migration/       # MigrationTask, TaskId, RiskScore
│       │   │   ├── rule/            # MigrationRule, RuleCode, MatchPattern...
│       │   │   ├── scanner/         # ScanResult, ScanSource
│       │   │   └── report/          # MigrationReport, ReportSection
│       │   ├── repository/          # 仓储接口（Connection/Task/ScanResult/MigrationRule）
│       │   └── service/             # 领域服务 + SPI 扩展点
│       │       ├── SqlCompatibilityAnalyzer    # SQL 兼容性分析
│       │       ├── SqlConvertStrategy         # SQL 转换策略（SPI）
│       │       ├── SqlConverter               # SQL 转换器
│       │       ├── JdbcMetadataExtractor      # 元数据提取器（SPI）
│       │       ├── SourceCodeScanner          # 源码扫描器
│       │       ├── ReportGenerator            # 报告生成器
│       │       ├── RuleLoader                 # 规则加载器（SPI）
│       │       ├── AiAdvisor                  # AI 顾问（SPI 预留）
│       │       └── LicenseManager             # 许可证管理（SPI 预留）
│       │
│       ├── infrastructure/          # ── 基础设施层 ──
│       │   ├── engine/              # RuleEngine, JsonFileRuleLoader, DefaultSqlCompatibilityAnalyzer
│       │   ├── converter/           # 7种SQL转换策略 + StoredProcedureConverter + SqlConvertManager
│       │   ├── scanner/             # Java/Xml/SqlFile扫描器 + ReservedKeywordDetector + 元数据提取器
│       │   ├── parser/              # JsqlParserWrapper（JSQLParser + Druid）
│       │   ├── report/              # HtmlReportGenerator, ReportGeneratorFactory
│       │   ├── repository/          # InMemory 仓储实现（后续迁移至 SQLite）
│       │   ├── ai/                  # NoOpAiAdvisor（MVP 占位）
│       │   └── license/             # OpenSourceLicenseManager（MVP 宽松）
│       │
│       └── resources/
│           ├── application.yml
│           ├── rules/               # 📋 83条兼容性规则（3条迁移路径）
│           │   ├── mysql-postgresql.json    # MySQL→PG: 30条规则
│           │   ├── oracle-postgresql.json   # Oracle→PG: 25条规则
│           │   └── mysql-dameng.json        # MySQL→达梦: 28条规则
│           └── templates/report/    # Thymeleaf 报告模板
│
├── dma-desktop/                     # 🖥️ JavaFX 桌面端（4 Tab 界面）
│   └── src/main/java/com/dma/desktop/
│       ├── DmaLauncher.java        # 启动器（解决 JavaFX module-path）
│       └── DmaDesktopApplication.java  # 主界面：4个Tab页
│
├── dma-idea-plugin/                 # 🔌 IDEA 插件骨架（后续）
├── dma-test/                        # 🧪 集成测试（19个测试用例）
└── docs/                            # 📚 详细设计文档
```

---

## 技术栈

| 层级 | 技术 | 版本 | 用途 |
|------|------|------|------|
| **语言** | Java | 17 LTS | — |
| **框架** | Spring Boot | 3.2.5 | DI、REST、配置 |
| **桌面 UI** | JavaFX | 21.0.2 | Windows 桌面应用 |
| **SQL 解析** | JSQLParser | 5.0 | SQL AST 解析与转换 |
| **连接池** | Druid | 1.2.20 | 数据库连接管理 |
| **构建** | Maven | 3.9+ | 多模块构建 |
| **规则存储** | SQLite + JSON | — | 本地规则库（MVP用JSON，生产用SQLite） |
| **报告** | Thymeleaf + POI + FlyingSaucer | — | HTML/PDF/Word |
| **JDBC 驱动** | MySQL / PostgreSQL / Oracle | — | 数据库连接 |
| **测试** | JUnit 5 | — | 19个测试，全部通过 |

---

## 快速开始

### 环境要求
- **JDK 17**（路径：`D:\DEV_Application\Java\jdk17`）
- **Maven 3.9+**

### 运行

```bash
# 方式一：一键启动
双击 run.bat

# 方式二：命令行
export JAVA_HOME="D:/DEV_Application/Java/jdk17"
mvn clean install -DskipTests
mvn javafx:run -pl dma-desktop

# 方式三：IDEA
运行 dma-desktop/src/main/java/com/dma/desktop/DmaLauncher.java
```

### 测试

```bash
mvn test
# Tests run: 19, Failures: 0, Errors: 0, Skipped: 0
```

---

## 四大核心功能

### 🏥 一、数据库体检

```
连接源库 → 提取所有对象 → 逐一分析兼容性 → 输出兼容率
```

**流程**：
1. 填写数据库连接信息（主机/端口/用户名/密码）
2. 点击「获取Schema」自动发现可用数据库
3. 下拉选择目标 Schema
4. 选择目标数据库类型
5. 点击「开始扫描」

**输出**：
```
  存储过程: 23    函数: 18
  表:       142   视图:  8
  总对象数: 191

  ✓ 完全兼容:   156
  ⚡ 可自动转换: 31
  ⚠ 需人工审核:  3
  ✗ 不兼容:      1

  ★ 兼容率: 97.9%
```

### 🔄 二、SQL 自动转换

```
输入 SQL → 规则引擎匹配 → 自动转换 → 原SQL/转换后SQL 对比
```

**示例**：
```
输入 MySQL:
  SELECT IFNULL(name, '') FROM users LIMIT 0, 10;

输出 PostgreSQL:
  SELECT COALESCE(name, '') FROM users LIMIT 10 OFFSET 0;

说明:
  - IFNULL → COALESCE（函数名替换）
  - LIMIT m,n → LIMIT n OFFSET m（分页语法转换）
```

**支持的转换类型**：
| 类型 | 示例 |
|------|------|
| 函数名称 | IFNULL→COALESCE, NVL→COALESCE, NOW→CURRENT_TIMESTAMP |
| 分页语法 | LIMIT m,n→LIMIT n OFFSET m, ROWNUM→ROW_NUMBER() |
| 数据类型 | DATETIME→TIMESTAMP, TINYINT→SMALLINT, BLOB→BYTEA |
| 标识符 | `` `col` `` → `"col"` |
| 自增列 | AUTO_INCREMENT→SERIAL/IDENTITY |
| NULL 处理 | NVL(a,b)→COALESCE(a,b) |

### 📦 三、存储过程迁移

```
粘贴 DDL → 结构性转换 → 显示: 原DDL ↓ 转换后DDL ↓ 变更说明
```

**支持的对象类型**：
- ✅ `PROCEDURE` — 存储过程
- ✅ `FUNCTION` — 函数
- ✅ `TRIGGER` — 触发器
- ✅ `VIEW` — 视图

**转换示例**：
```
MySQL 源:                          GaussDB 目标:
DELIMITER $$                       CREATE OR REPLACE PROCEDURE test()
CREATE PROCEDURE test()            AS
BEGIN                              BEGIN
  SELECT NOW();                      SELECT CURRENT_TIMESTAMP;
END                                END;
$$                                 LANGUAGE plpgsql;
```

**结构性转换项**：
| 转换 | MySQL | GaussDB/PostgreSQL |
|------|-------|-------------------|
| DELIMITER | `DELIMITER $$` | 移除 |
| CREATE | `CREATE PROCEDURE` | `CREATE OR REPLACE PROCEDURE` |
| DEFINER | `DEFINER=root@%` | 移除 |
| BEGIN/END | `BEGIN...END` | `AS BEGIN...END;` |
| RETURNS | `RETURNS type` | `RETURN type` |
| LANGUAGE | 无 | 自动添加 `LANGUAGE plpgsql` |
| SQL 安全 | `SQL SECURITY DEFINER` | 移除 |

### 📂 四、项目源码扫描

```
选择项目目录 → 全量扫描 → 文件统计 + 风险分级 + 关键字检测
```

**扫描范围**：
| 文件类型 | 检测内容 |
|---------|---------|
| `.java` | SQL 字符串字面量 → 兼容性分析 |
| `*Mapper.xml` | MyBatis SQL 标签 → 兼容性分析 |
| `.sql` | 按分号分割 → 逐条兼容性分析 + 保留关键字检测 |

**输出示例**：
```
  共扫描: 5,321 个文件

  发现问题:
  ✗ 高风险: 23     (ERROR 级别 — 阻止迁移)
  ⚠ 中风险: 156    (WARNING 级别 — 可自动转换)
  ℹ 低风险: 512    (INFO 级别 — 建议关注)

  迁移风险评分: 45/100
  [████████████████░░░░░░░░░░░░░░░░░░░░░░░░░░] 45%
```

**保留关键字检测**：自动识别 `USER` / `ORDER` / `GROUP` / `COMMENT` / `TYPE` 等 16+ 个常用关键字作为标识符使用时的风险。

---

## API 文档

### REST API 端点一览

| 方法 | URL | 说明 |
|------|-----|------|
| `GET` | `/api/v1/system/health` | 健康检查 |
| `GET` | `/api/v1/system/info` | 系统信息 |
| `POST` | `/api/v1/connections` | 创建数据库连接 |
| `GET` | `/api/v1/connections` | 连接列表 |
| `DELETE` | `/api/v1/connections/{id}` | 删除连接 |
| `POST` | `/api/v1/connections/test` | 测试连接 |
| `POST` | `/api/v1/scan/sql` | 扫描单条 SQL |
| `POST` | `/api/v1/scan/project` | 扫描项目源码 |
| `POST` | `/api/v1/scan/project-full` | 项目扫描（含风险分级） |
| `POST` | `/api/v1/scan/database` | 数据库体检 |
| `POST` | `/api/v1/scan/schemas` | 获取 Schema 列表 |
| `POST` | `/api/v1/convert/procedure` | 转换存储过程 DDL |
| `POST` | `/api/v1/tasks` | 创建迁移任务 |
| `POST` | `/api/v1/tasks/{id}/execute` | 执行任务 |
| `GET` | `/api/v1/tasks/{id}/report` | 导出迁移报告 |

---

## 规则体系

### 已实现规则

| 迁移路径 | 规则数 | 覆盖分类 |
|---------|--------|---------|
| MySQL → PostgreSQL | 30 | 内置函数(12) + 语法(6) + 数据类型(5) + DDL(4) + 其他(3) |
| Oracle → PostgreSQL | 25 | 内置函数(12) + 语法(4) + 数据类型(4) + DDL(2) + 其他(3) |
| MySQL → 达梦 DM | 28 | 内置函数(12) + 语法(6) + 数据类型(4) + DDL(3) + 其他(3) |

### 规则扩展

在 `dma-core/src/main/resources/rules/` 下创建新的 JSON 文件即可添加新路径或新规则：

```json
{
  "code": "M2PG-FN-001",
  "name": "IFNULL → COALESCE",
  "sourceDbType": "MYSQL",
  "targetDbType": "POSTGRESQL",
  "category": "BUILTIN_FUNCTION",
  "severity": "WARNING",
  "patternType": "FUNCTION",
  "matchPattern": "IFNULL(${expr1}, ${expr2})",
  "replacementPattern": "COALESCE(${expr1}, ${expr2})",
  "description": "MySQL IFNULL 函数需替换为 PG COALESCE",
  "exampleSqlSource": "SELECT IFNULL(name, 'N/A') FROM users;",
  "exampleSqlTarget": "SELECT COALESCE(name, 'N/A') FROM users;"
}
```

详见 `docs/standards/rule-json-spec.md`

---

## 扩展开发

### SPI 扩展点

| 接口 | 用途 | 扩展方式 |
|------|------|---------|
| `SqlConvertStrategy` | 新增 SQL 转换策略 | 实现接口 + `@Component` |
| `RuleLoader` | 新增规则来源 | 实现接口 + `@Component` |
| `JdbcMetadataExtractor` | 支持新数据库元数据提取 | 实现接口 + `@Component` |
| `ReportGenerator` | 新增报告格式 | 实现接口 + `@Component` |
| `AiAdvisor` | 接入 AI 大模型 | 实现接口 + `@Component` |
| `LicenseManager` | 商业化 License | 实现接口 + `@Component` |

详见 `docs/architecture/extension-points.md`

---

## 后续规划

| 优先级 | 功能 | 描述 |
|--------|------|------|
| 🔴 高 | **报告导出集成** | 扫描结果一键导出 HTML/PDF/Word |
| 🔴 高 | **缺失规则路径补充** | SQLServer → PG/GaussDB, MySQL → GaussDB/GoldenDB/OceanBase |
| 🔴 高 | **SQLite 持久化存储** | 替换 InMemory 仓储，本地持久化 |
| 🟡 中 | **AI 迁移顾问对接** | 对接 OpenAI/Claude/通义千问 API |
| 🟡 中 | **连接配置持久化** | 保存/加载常用连接配置 |
| 🟡 中 | **IDEA 插件开发** | IntelliJ 内 SQL 实时检测 + Alt+Enter 修复 |
| 🟢 低 | **批量迁移向导** | 向导式多步骤迁移流程 |
| 🟢 低 | **远程规则市场** | 社区共享规则包 |

---

## 项目统计

| 指标 | 数值 |
|------|------|
| Java 源文件 | 95 |
| 测试用例 | 19（全部通过） |
| 兼容性规则 | 83 条（3 条路径） |
| Maven 模块 | 6 |
| REST API 端点 | 15 |
| 文档文件 | 34+ |
| 代码行数 | ~12,000 |

---

**License**: Apache 2.0  
**Author**: Arvin-LK  
**GitHub**: [https://github.com/Arvin-LK/dma](https://github.com/Arvin-LK/dma)
