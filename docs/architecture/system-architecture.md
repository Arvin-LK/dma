# 系统架构总览

## 架构全景图

```
┌────────────────────────────────────────────────────────────┐
│                     dma-desktop (JavaFX)                     │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐      │
│  │ 连接管理  │ │ SQL转换  │ │ 项目扫描  │ │ 报告查看  │      │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘      │
│                        │ HTTP REST                          │
├────────────────────────┼───────────────────────────────────┤
│                     dma-core (核心)                          │
│  ┌─────────────────────┴──────────────────────────────┐    │
│  │                  API 层 (REST Controller)            │    │
│  ├────────────────────────────────────────────────────┤    │
│  │               应用层 (Application Service)          │    │
│  │   MigrationService | ScanService | ReportService     │    │
│  ├────────────────────────────────────────────────────┤    │
│  │                   领域层 (Domain)                    │    │
│  │   Entities | Value Objects | Repository Interfaces │    │
│  │   Domain Services | SPI Interfaces                  │    │
│  ├────────────────────────────────────────────────────┤    │
│  │                基础设施层 (Infrastructure)           │    │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐           │    │
│  │  │ 规则引擎  │ │SQL解析器 │ │ 扫描器   │           │    │
│  │  ├──────────┤ ├──────────┤ ├──────────┤           │    │
│  │  │ 报告生成  │ │ 仓储实现  │ │ AI适配器 │           │    │
│  │  └──────────┘ └──────────┘ └──────────┘           │    │
│  └────────────────────────────────────────────────────┘    │
│                        │                                    │
│  ┌─────────────────────┴──────────────────────────────┐    │
│  │                   dma-common                         │    │
│  │   Enums | DTOs | Exceptions | Utils                  │    │
│  └────────────────────────────────────────────────────┘    │
└────────────────────────────────────────────────────────────┘
```

## 技术选型

| 层级 | 技术 | 版本 | 选型理由 |
|------|------|------|---------|
| **构建工具** | Maven | 3.9+ | Java 生态标准，依赖管理成熟 |
| **语言** | Java | 17 | LTS 版本，生态稳定 |
| **框架** | Spring Boot | 3.2.x | 依赖注入、自动配置、REST 支持 |
| **桌面 UI** | JavaFX | 21 | 现代桌面 UI，支持 FXML 布局 |
| **SQL 解析** | JSQLParser | 5.0 | 支持多种 SQL 方言的 AST 解析 |
| **连接池** | Druid | 1.2.20 | 阿里巴巴出品，SQL 监控能力强 |
| **规则存储** | SQLite | 3.44+ | 轻量、无需服务端、适合本地应用 |
| **HTML 报告** | Thymeleaf | (SB 内置) | Spring Boot 官方推荐模板引擎 |
| **PDF 报告** | Flying Saucer | 9.x | HTML→PDF，复用 Thymeleaf 模板 |
| **Word 报告** | Apache POI | 5.2.x | Java 操作 Office 文档的事实标准 |
| **JSON** | Jackson | (SB 内置) | Spring Boot 默认 JSON 库 |
| **测试** | JUnit 5 | (SB 内置) | Java 测试标准 |

## 模块依赖关系

```
dma (父 POM)
 ├── dma-common          ← 无内部依赖，最底层
 ├── dma-core            ← 依赖 dma-common
 ├── dma-desktop         ← 依赖 dma-core + dma-common
 ├── dma-idea-plugin     ← 依赖 dma-core + dma-common（后续）
 └── dma-test            ← 依赖所有模块（测试）
```

**依赖规则**：
- 上层可依赖下层，下层不可依赖上层
- dma-common 不依赖任何内部模块
- dma-core 不依赖 dma-desktop / dma-idea-plugin
- dma-desktop 和 dma-idea-plugin 通过 REST API 与 dma-core 通信

## 部署架构（MVP）

```
Windows 桌面应用
┌──────────────────────────────┐
│  dma-desktop.exe (jpackage)  │
│  ├── 内嵌 JRE 17             │
│  ├── Spring Boot (嵌入式)    │
│  │   └── Tomcat (嵌入式)     │
│  ├── JavaFX 21               │
│  ├── SQLite 规则库 (本地)    │
│  └── 用户数据 (本地 SQLite)  │
└──────────────────────────────┘
```

- 桌面端内嵌 Spring Boot + Tomcat，JavaFX 通过 HTTP localhost 调用 Core API
- 所有数据存储在本地 SQLite 文件中
- 规则库打包在 JAR 内（resources/rules/*.json）
- 用户可添加自定义规则到用户目录
