# Database Migration Assistant (DMA) 数据库迁移助手

[![Java 17](https://img.shields.io/badge/Java-17-orange)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot 3.2](https://img.shields.io/badge/Spring%20Boot-3.2-green)](https://spring.io/projects/spring-boot)
[![JavaFX 21](https://img.shields.io/badge/JavaFX-21-blue)](https://openjfx.io/)
[![Release](https://img.shields.io/badge/release-v1.1.0-1a73e8)](https://github.com/Arvin-LK/dma/releases)

> 支持 MySQL / Oracle / SQLServer 向 PostgreSQL / GaussDB / GoldenDB / OceanBase / 达梦 的数据库迁移与兼容性分析工具。

---

## 🚀 快速下载（无需 Java 环境）

| 版本 | 下载 | 大小 |
|------|------|------|
| **Windows 64位 v1.1.0** | [DMA-v1.1.0-win64.zip](https://github.com/Arvin-LK/dma/releases/latest) | ~119 MB |

**使用方式**：
1. 下载 `DMA-v1.1.0-win64.zip`
2. 解压到任意目录
3. 双击 `DMA.exe`
4. ✅ 无需安装 JDK — 运行时已内置

---

## ✨ 功能总览

| 功能 | 说明 | 状态 |
|------|------|------|
| 🏥 **数据库体检** | 连接源库 → 全量扫描 → 逐对象分析 → 兼容率 + 修改建议 | ✅ |
| 🔄 **SQL 自动转换** | 输入 SQL → 规则引擎匹配 → 自动生成目标库语法 | ✅ |
| 📦 **存储过程迁移** | PROCEDURE / FUNCTION / TRIGGER / VIEW DDL 自动转换 | ✅ |
| 📂 **项目源码扫描** | Java / MyBatis XML / SQL 文件全量扫描 + 保留关键字 + 风险分级 | ✅ |
| 📄 **报告导出** | HTML / PDF / Word 三种格式一键导出 | ✅ |
| 🤖 **AI 迁移顾问** | 7 种大模型支持，页面内自行配置 API Key | ✅ |
| ⚙️ **AI 模型配置** | Provider / URL / Key / Model 页面内配置，SQLite 持久化 | ✅ |
| 🔐 **密码加密** | AES-256-GCM 加密存储数据库连接密码 | ✅ |
| 📋 **连接管理** | 保存/加载/测试数据库连接配置 | ✅ |

---

## 🤖 AI 模型支持

页面内自行配置，无需修改配置文件：

| Provider | 模型 | 说明 |
|----------|------|------|
| **Ollama** | qwen2.5:7b / llama3 / deepseek-r1 | 本地部署，无需 API Key |
| **OpenAI** | gpt-4o-mini / gpt-4o | ChatGPT 云端 |
| **DeepSeek** | deepseek-chat / deepseek-reasoner | 高性价比 |
| **通义千问** | qwen-plus / qwen-max | 阿里云 DashScope |
| **智谱 GLM** | glm-4-flash / glm-4-plus | 智谱清言 |
| **月之暗面** | moonshot-v1-8k | Kimi |
| **自定义** | 任意 OpenAI 兼容接口 | vLLM / LocalAI 等 |

---

## 📊 迁移路径与规则（15 条路径 / 322 条规则）

| 源数据库 | PostgreSQL | 达梦 | GaussDB | OceanBase | GoldenDB |
|---------|-----------|------|---------|-----------|----------|
| **MySQL** | 30 | 27 | 29 | 22 | 17 |
| **Oracle** | 25 | 30 | 25 | 13 | 17 |
| **SQLServer** | 27 | 17 | 18 | 12 | 13 |

---

## 🖥️ 界面

DBeaver 风格专业界面设计：

- **浅色专业主题** — 白色卡片 + 浅灰背景 + 蓝色强调色
- **三区分离布局** — 配置参数区 → 输入编辑区 → 结果输出区
- **输入/输出 3:7 比例** — 编辑区精简，结果区占主导
- **DMA 字母图标** — 蓝底白字，多分辨率适配

---

## 🏥 数据库体检

```
连接源库 → 提取对象 → 规则匹配 → 逐对象详细分析 → 兼容率 + 修改建议
```

**流程**：
1. 填写数据库连接信息
2. 点击「获取 Schema」自动发现可用数据库
3. 选择源/目标数据库类型
4. 点击「开始扫描」

**输出报告包含**：
- 对象统计（存储过程 / 函数 / 表 / 视图）
- 兼容性概要（完全兼容 / 可自动转换 / 需审核 / 不兼容）
- 逐对象详细分析：原始 DDL → 具体问题 → 匹配规则 → 建议方案
- 每个问题给出操作建议（可自动转换 / 需人工 / 建议 AI）

---

## 🔄 SQL 自动转换

```
输入 SQL → 规则引擎匹配 → 自动生成目标库语法 → 原SQL/新SQL对比
```

**转换类型覆盖**：函数名称 / 分页语法 / 数据类型 / 标识符 / 自增列 / DDL 差异

---

## 📦 存储过程迁移

**支持对象**：PROCEDURE / FUNCTION / TRIGGER / VIEW

**转换项**：DELIMITER / DEFINER / BEGIN-END / RETURNS / LANGUAGE / SQL SECURITY / 字符集等

---

## 📂 项目源码扫描

| 文件类型 | 检测内容 |
|---------|---------|
| `.java` | SQL 字符串字面量 → 兼容性分析 |
| `*Mapper.xml` | MyBatis SQL 标签 → 兼容性分析 |
| `.sql` | 逐条 SQL + 保留关键字检测 |

输出：文件统计 + 风险分级（高/中/低）+ 风险评分

---

## 🔧 开发环境

### 环境要求
- **JDK 17**
- **Maven**（或使用 `./mvnw` wrapper）

### 运行

```bash
# 一键启动
双击 run.bat

# 命令行
./mvnw install -DskipTests
./mvnw javafx:run -pl dma-desktop

# IDEA
运行 DmaLauncher.java
```

### 测试

```bash
./mvnw test
# Tests run: 26, Failures: 0, Errors: 0, Skipped: 0
```

### 打包 .exe

```bash
双击 package.bat
# 输出: dist/DMA/DMA.exe（自包含 JRE）
```

---

## 🧱 项目架构

```
dma/
├── dma-common/       # 公共模块（枚举/DTO/异常/工具类）
├── dma-core/         # 核心模块（DDD四层）
│   ├── api/          # REST 控制器（9个）
│   ├── application/  # 应用编排服务（4个）
│   ├── domain/       # 领域模型 + SPI 扩展点（8个）
│   └── infrastructure/  # 规则引擎/转换策略/扫描器/AI/报告/加密
├── dma-desktop/      # JavaFX 桌面端
├── dma-idea-plugin/  # IDEA 插件（骨架）
├── dma-test/         # 集成测试（26个）
└── docs/             # 完整文档体系
```

---

## 📡 API 端点（15个）

| 方法 | URL | 说明 |
|------|-----|------|
| `GET` | `/api/v1/system/health` | 健康检查 |
| `GET` | `/api/v1/system/info` | 系统信息 |
| `POST` | `/api/v1/connections` | 创建连接 |
| `GET` | `/api/v1/connections` | 连接列表 |
| `DELETE` | `/api/v1/connections/{id}` | 删除连接 |
| `POST` | `/api/v1/scan/sql` | SQL 兼容性扫描 |
| `POST` | `/api/v1/scan/schemas` | 获取 Schema 列表 |
| `POST` | `/api/v1/scan/database` | 数据库全量体检 |
| `POST` | `/api/v1/scan/project` | 项目源码扫描 |
| `POST` | `/api/v1/scan/project-full` | 项目扫描（含风险） |
| `POST` | `/api/v1/convert/procedure` | 存储过程转换 |
| `GET` | `/api/v1/ai/status` | AI 服务状态 |
| `POST` | `/api/v1/ai/advice` | AI 迁移建议 |
| `GET/POST` | `/api/v1/ai/settings` | AI 配置读写 |
| `POST` | `/api/v1/report/export` | 导出报告 |

---

## 🛠 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 语言 | Java | 17 LTS |
| 框架 | Spring Boot | 3.2.5 |
| 桌面 UI | JavaFX | 21.0.2 |
| SQL 解析 | JSQLParser + Druid | 5.0 / 1.2.20 |
| 规则存储 | JSON 文件（322条） | — |
| 配置存储 | SQLite | 3.44.1.0 |
| 密码加密 | AES-256-GCM | — |
| 报告 | Thymeleaf + POI + FlyingSaucer | — |
| AI 适配 | OpenAI 兼容接口 | — |
| 构建 | Maven Wrapper | 3.9+ |
| 打包 | jpackage (JDK 17) | — |
| 测试 | JUnit 5 | 26 tests |
| JDBC | MySQL / PG / Oracle / SQLServer / SQLite | — |

---

## 📈 项目统计（v1.1.0）

| 指标 | 数值 |
|------|------|
| 兼容性规则 | 322 条 |
| 迁移路径 | 15 条（全覆盖） |
| 测试用例 | 26（全部通过） |
| Maven 模块 | 6 |
| REST API 端点 | 15 |
| SPI 扩展点 | 8 |
| AI 模型支持 | 7 种 Provider |
| 异常处理 | GlobalExceptionHandler（8种） |

---

**License**: Apache 2.0  
**Author**: Arvin-LK  
**GitHub**: [https://github.com/Arvin-LK/dma](https://github.com/Arvin-LK/dma)
