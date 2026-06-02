# Database Migration Assistant (DMA) 数据库迁移助手

[![Java 17](https://img.shields.io/badge/Java-17-orange)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot 3.2](https://img.shields.io/badge/Spring%20Boot-3.2-green)](https://spring.io/projects/spring-boot)
[![JavaFX 21](https://img.shields.io/badge/JavaFX-21-blue)](https://openjfx.io/)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue)](LICENSE)
[![Release](https://img.shields.io/badge/release-v1.0.0-1a73e8)](https://github.com/Arvin-LK/dma/releases)

> 支持 MySQL / Oracle / SQLServer 向 PostgreSQL / GaussDB / GoldenDB / OceanBase / 达梦 的数据库迁移与兼容性分析工具。

---

## 🚀 快速下载（无需 Java 环境）

| 版本 | 下载 | 大小 |
|------|------|------|
| **Windows 64位** | [DMA-v1.0.0-win64.zip](https://github.com/Arvin-LK/dma/releases/latest) | ~118 MB |

**使用方式**：
1. 下载 `DMA-v1.0.0-win64.zip`
2. 解压到任意目录
3. 双击 `DMA.exe`
4. ✅ 无需安装 JDK — 运行时已内置

> 需要 Java 开发环境？见[开发环境运行](#开发环境运行)

---

## 功能总览

| 功能 | 说明 | 状态 |
|------|------|------|
| 🏥 **数据库体检** | 连接源库 → 提取全量对象 → 兼容性分析 → 兼容率统计 | ✅ |
| 🔄 **SQL 自动转换** | 输入 SQL → 规则引擎匹配 → 自动生成目标库语法 | ✅ |
| 📦 **存储过程迁移** | PROCEDURE / FUNCTION / TRIGGER / VIEW DDL 结构性转换 | ✅ |
| 📂 **项目源码扫描** | Java / MyBatis XML / SQL 文件全量扫描 + 风险分级 | ✅ |
| 📄 **报告导出** | HTML / PDF / Word 三种格式报告一键导出 | ✅ |
| 🤖 **AI 迁移顾问** | 支持 Ollama 本地模型 / OpenAI / 通义千问 / DeepSeek + vLLM | ✅ |
| 📋 **连接管理** | 保存/加载/测试数据库连接配置，SQLite 持久化 | ✅ |

---

## 📊 支持的迁移路径（200+ 规则）

| 源数据库 | 目标数据库 | 规则数 |
|---------|-----------|--------|
| **MySQL** | PostgreSQL | 30 |
| **MySQL** | 达梦 DM8 | 28 |
| **MySQL** | GaussDB | 28 |
| **MySQL** | OceanBase | 22 |
| **Oracle** | PostgreSQL | 25 |
| **Oracle** | 达梦 DM8 | 27 |
| **Oracle** | GaussDB | 25 |
| **SQLServer** | PostgreSQL | 27 |
| | **合计** | **200+** |

---

## 🖥️ 界面预览

应用采用 DBeaver 风格专业界面设计：

- **浅色专业主题** — 白色卡片 + 浅灰背景 + 蓝色强调色
- **三区分离布局** — 配置参数区 → 输入编辑区 → 结果输出区，每区独立卡片
- **左侧导航栏** — 蓝色 Logo 区 + 6 个功能入口 + 蓝色激活指示条
- **首页仪表盘** — 统计卡片 + 2×2 快捷功能网格

### 六大功能页面

| 页面 | 功能 |
|------|------|
| 🏠 首页概览 | 项目统计、快捷入口 |
| 🏥 数据库体检 | 连接管理 → Schema 发现 → 全量扫描 → 兼容率 |
| 🔄 SQL 转换 | 粘贴 SQL → 源/目标选择 → 分析转换 → 对比结果 |
| 📦 存储过程 | DDL 粘贴 → 自动转换 → 原DDL/新DDL/变更清单 |
| 📂 项目扫描 | 目录选择 → 全量扫描 → 文件统计 + 风险评分 |
| 🤖 AI 顾问 | 状态检测 → 输入问题 → AI 分析建议 |

---

## 🏥 数据库体检

```
连接源库 → 提取所有对象 → 逐一分析兼容性 → 输出兼容率
```

**流程**：
1. 填写/选择数据库连接（支持保存连接配置）
2. 点击「获取 Schema」自动发现可用数据库
3. 选择源/目标数据库类型
4. 点击「开始扫描」

**输出报告**：
```
  存储过程: 23    函数: 18    表: 142    视图: 8

  ✓ 完全兼容:   156
  ⚡ 可自动转换: 31
  ⚠ 需人工审核:  3
  ✗ 不兼容:      1

  ★ 兼容率: 97.9%
```

**支持的元数据提取**：MySQL (SHOW CREATE) / Oracle (DBMS_METADATA) / SQLServer (OBJECT_DEFINITION)

---

## 🔄 SQL 自动转换

```
输入 SQL → 规则引擎匹配 → 自动转换 → 原SQL/新SQL对比
```

**示例**：
```
输入 MySQL:
  SELECT IFNULL(name, '') FROM users LIMIT 0, 10;

输出 PostgreSQL:
  SELECT COALESCE(name, '') FROM users LIMIT 10 OFFSET 0;

变更:
  ✗ IFNULL → COALESCE（函数名替换）
  ✗ LIMIT m,n → LIMIT n OFFSET m（分页语法转换）
```

**转换类型覆盖**：

| 类型 | 示例 |
|------|------|
| 函数名称 | IFNULL→COALESCE, NVL→COALESCE, NOW()→CURRENT_TIMESTAMP |
| 分页语法 | LIMIT m,n→LIMIT n OFFSET m, ROWNUM→ROW_NUMBER() |
| 数据类型 | DATETIME→TIMESTAMP, TINYINT→SMALLINT, BLOB→BYTEA |
| 标识符 | `` `col` `` → `"col"` |
| 自增列 | AUTO_INCREMENT→SERIAL/IDENTITY |
| DDL | ENGINE=InnoDB→移除, CREATE TEMPORARY→CREATE GLOBAL TEMPORARY |

---

## 📦 存储过程迁移

**支持的对象类型**：PROCEDURE / FUNCTION / TRIGGER / VIEW

**结构性转换项**：

| 转换 | MySQL | → GaussDB/PostgreSQL |
|------|-------|---------------------|
| 分隔符 | `DELIMITER $$` | 移除 |
| 创建语法 | `CREATE PROCEDURE` | `CREATE OR REPLACE PROCEDURE` |
| 定义者 | `DEFINER=root@%` | 移除 |
| 块语法 | `BEGIN...END` | `AS BEGIN...END;` |
| 返回值 | `RETURNS type` | `RETURN type` |
| 语言声明 | 无 | 自动添加 `LANGUAGE plpgsql` |
| SQL 安全 | `SQL SECURITY DEFINER` | 移除 |

---

## 📂 项目源码扫描

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
  Java: 2,103  XML: 421  SQL: 85

  ✗ 高风险: 23     (ERROR 级别)
  ⚠ 中风险: 156    (WARNING 级别)
  ℹ 低风险: 512    (INFO 级别)

  风险评分: 45/100
```

**保留关键字检测**：自动识别 `USER` / `ORDER` / `GROUP` / `COMMENT` / `TYPE` 等 16+ 个常用关键字作为标识符使用时的风险。

---

## 🤖 AI 迁移顾问

支持多种部署模式，满足内网/外网不同场景：

| 模式 | 提供商 | 配置 |
|------|--------|------|
| 🏠 本地 | Ollama | `dma.ai.provider=ollama` |
| ☁️ 云端 | OpenAI | `dma.ai.provider=openai` |
| ☁️ 云端 | 通义千问 | `dma.ai.provider=custom` + DashScope |
| ☁️ 云端 | DeepSeek | `dma.ai.provider=custom` |
| 🖥️ 内网 | vLLM | `dma.ai.provider=custom` |

配置方式：编辑 `application.yml` → `dma.ai.*` 即可切换。详见 [AI 部署指南](docs/ai-setup-guide.md)

---

## 📄 报告导出

扫描完成后支持导出三种格式报告：

| 格式 | 技术 | 特点 |
|------|------|------|
| **HTML** | Thymeleaf 模板 | 浏览器查看，交互式图表 |
| **PDF** | FlyingSaucer | 便携分享，版式固定 |
| **Word** | Apache POI XWPF | 可编辑，适合写迁移方案 |

导出位置：自动保存到桌面，生成后自动打开。

---

## 🔧 开发环境运行

### 环境要求

- **JDK 17**（路径：`D:\DEV_Application\Java\jdk17`）
- **Maven 3.9+**

### 运行方式

```bash
# 方式一：一键启动（推荐）
双击 run.bat

# 方式二：命令行
export JAVA_HOME="D:/DEV_Application/Java/jdk17"
mvn clean install -DskipTests
mvn javafx:run -pl dma-desktop

# 方式三：IDEA
运行 dma-desktop/.../DmaLauncher.java
```

### 测试

```bash
mvn test
# Tests run: 19, Failures: 0, Errors: 0, Skipped: 0
```

### 打包为 .exe

```bash
双击 package.bat
# 输出: dist/DMA/DMA.exe（自包含，无需 JDK）
```

---

## 🧱 项目架构

```
dma/
├── dma-common/       # 公共模块（枚举/DTO/异常/工具类）
├── dma-core/         # 核心模块（DDD四层：api/application/domain/infrastructure）
│   ├── api/          # REST 控制器（7个）
│   ├── application/  # 应用编排服务（4个）
│   ├── domain/       # 领域模型 + SPI 扩展点（8个接口）
│   └── infrastructure/  # 规则引擎/转换策略/扫描器/AI适配器/报告生成
├── dma-desktop/      # JavaFX 桌面端（DBeaver 风格界面）
├── dma-idea-plugin/  # IDEA 插件（骨架）
├── dma-test/         # 集成测试（19个用例）
└── docs/             # 完整项目文档体系
```

---

## 📡 API 端点

| 方法 | URL | 说明 |
|------|-----|------|
| `GET` | `/api/v1/system/health` | 健康检查 |
| `GET` | `/api/v1/system/info` | 系统信息 |
| `POST` | `/api/v1/connections` | 创建连接 |
| `GET` | `/api/v1/connections` | 连接列表 |
| `DELETE` | `/api/v1/connections/{id}` | 删除连接 |
| `POST` | `/api/v1/connections/test` | 测试连接 |
| `POST` | `/api/v1/scan/sql` | 扫描 SQL |
| `POST` | `/api/v1/scan/schemas` | 获取 Schema 列表 |
| `POST` | `/api/v1/scan/database` | 数据库体检 |
| `POST` | `/api/v1/scan/project` | 项目扫描 |
| `POST` | `/api/v1/scan/project-full` | 项目扫描（含风险） |
| `POST` | `/api/v1/convert/procedure` | 转换存储过程 |
| `POST` | `/api/v1/ai/advice` | AI 迁移建议 |
| `GET` | `/api/v1/ai/status` | AI 服务状态 |
| `POST` | `/api/v1/report/export` | 导出报告 |

---

## 🔌 SPI 扩展点

| 接口 | 用途 | 扩展示例 |
|------|------|---------|
| `SqlConvertStrategy` | SQL 转换策略 | 新增 GoldenDB 特化转换 |
| `RuleLoader` | 规则来源 | 数据库/S3/Redis 远程加载 |
| `JdbcMetadataExtractor` | 新数据库支持 | 添加 MySQL 8.4 提取器 |
| `ReportGenerator` | 报告格式 | Markdown/JSON 格式 |
| `AiAdvisor` | AI 模型接入 | 对接新模型厂商 |
| `LicenseManager` | 商业 License | 在线/离线授权 |

详见 [扩展点设计](docs/architecture/extension-points.md)

---

## 🛠 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 语言 | Java | 17 LTS |
| 框架 | Spring Boot | 3.2.5 |
| 桌面 UI | JavaFX | 21.0.2 |
| SQL 解析 | JSQLParser + Druid | 5.0 / 1.2.20 |
| 规则存储 | SQLite + JSON | 3.44.1.0 |
| 报告 | Thymeleaf + POI + FlyingSaucer | — |
| 构建 | Maven | 3.9+ |
| 打包 | jpackage (JDK 17) | — |
| 测试 | JUnit 5 | — |
| JDBC 驱动 | MySQL / PostgreSQL / Oracle / 达梦 / SQLServer | — |

---

## 📈 项目统计

| 指标 | 数值 |
|------|------|
| Java 源文件 | 100+ |
| 测试用例 | 19（全部通过） |
| 兼容性规则 | 200+ 条（8 条路径） |
| Maven 模块 | 6 |
| REST API 端点 | 15 |
| SPI 扩展点 | 8 |
| 文档文件 | 34+ |
| 代码行数 | ~12,000 |

---

## 📋 后续规划

| 优先级 | 功能 | 描述 |
|--------|------|------|
| 🔴 高 | **缺失规则补充** | MySQL→GoldenDB, Oracle→OceanBase/GoldenDB, SQLServer→GaussDB/达梦 |
| 🔴 高 | **SQLite 规则存储** | 替换 JSON 文件为数据库查询 |
| 🟡 中 | **IDEA 插件开发** | IntelliJ 内 SQL 实时检测 + Alt+Enter 修复 |
| 🟡 中 | **批量迁移向导** | 向导式多步骤迁移流程 |
| 🟡 中 | **.exe 安装程序** | WiX Toolset → .msi 安装包 |
| 🟢 低 | **远程规则市场** | 社区共享规则包 |
| 🟢 低 | **国际化** | i18n 多语言支持 |

---

**License**: Apache 2.0  
**Author**: Arvin-LK  
**GitHub**: [https://github.com/Arvin-LK/dma](https://github.com/Arvin-LK/dma)
