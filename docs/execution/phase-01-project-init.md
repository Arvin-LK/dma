# Phase 1: Maven 多模块骨架

## 目标

创建 5 个子模块，确保父 POM 聚合编译通过。

## 前置条件

- [x] Phase 0 完成（文档体系、父POM、CLAUDE.md 就绪）

## 步骤

### 1.1 创建 dma-common 模块

- 创建 `dma-common/pom.xml`，声明父 POM，无额外依赖
- 创建包结构 `src/main/java/com/dma/common/`
- 创建占位包：`enums/`, `dto/`, `exception/`, `util/`

### 1.2 创建 dma-core 模块

- 创建 `dma-core/pom.xml`，依赖 `dma-common`, Spring Boot Starter, JSQLParser, Druid
- 创建 DDD 四层包结构：
  - `api/controller/`, `api/dto/`
  - `application/service/`, `application/pipeline/`
  - `domain/model/connection/`, `domain/model/migration/`, `domain/model/rule/`, `domain/model/scanner/`, `domain/model/report/`
  - `domain/service/`, `domain/repository/`
  - `infrastructure/repository/`, `infrastructure/parser/`, `infrastructure/converter/`, `infrastructure/scanner/`, `infrastructure/engine/`, `infrastructure/report/`, `infrastructure/ai/`, `infrastructure/license/`, `infrastructure/config/`
- 创建 `resources/application.yml`（基础配置）
- 创建 `resources/rules/` 空目录

### 1.3 创建 dma-desktop 模块

- 创建 `dma-desktop/pom.xml`，依赖 `dma-core`，JavaFX
- 创建包结构：`DmaApplication.java`, `ui/controller/`, `ui/view/`, `ui/component/`, `config/`

### 1.4 创建 dma-idea-plugin 模块

- 创建 `dma-idea-plugin/pom.xml`（仅占位，后续用 Gradle 重建）
- 创建基本目录结构

### 1.5 创建 dma-test 模块

- 创建 `dma-test/pom.xml`，依赖所有模块 + Spring Boot Test
- 创建测试包结构

## 验证标准

```bash
# 从根目录编译
mvn clean compile

# 预期：全部模块编译成功，无错误
```

## 产出

- 5 个子模块的 pom.xml
- 完整的 Java 包目录结构
- 根目录 `mvn compile` 通过
