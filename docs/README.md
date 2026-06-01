# DMA 项目文档索引

## 文档结构总览

```
docs/
├── README.md                           # 本文件 — 文档导航入口
│
├── requirements/                       # 需求文档
│   ├── product-brief.md                # 产品简介与定位
│   ├── mvp-scope.md                    # MVP 范围定义
│   ├── user-stories.md                 # 用户故事
│   └── functional-spec.md              # 功能规格说明
│
├── architecture/                       # 架构设计文档
│   ├── system-architecture.md          # 系统架构总览
│   ├── ddd-layer-design.md             # DDD 四层设计规范
│   ├── database-design.md              # 数据库设计
│   ├── api-design.md                   # REST API 设计规范
│   └── extension-points.md             # 扩展点设计
│
├── standards/                          # 开发规范文档
│   ├── code-style.md                   # 代码风格规范
│   ├── git-workflow.md                 # Git 工作流规范
│   ├── testing-standard.md             # 测试规范
│   ├── error-handling.md               # 异常处理规范
│   └── rule-json-spec.md               # 规则 JSON 格式规范
│
├── execution/                          # 执行步骤文档
│   ├── development-roadmap.md          # 开发路线图
│   ├── phase-01-project-init.md        # Phase 1 详细步骤
│   ├── phase-02-common-module.md       # Phase 2 详细步骤
│   ├── phase-03-domain-layer.md        # Phase 3 详细步骤
│   ├── phase-04-rule-engine.md         # Phase 4 详细步骤
│   ├── phase-05-sql-parser.md          # Phase 5 详细步骤
│   ├── phase-06-source-scanner.md      # Phase 6 详细步骤
│   ├── phase-07-report-generator.md    # Phase 7 详细步骤
│   ├── phase-08-application-layer.md   # Phase 8 详细步骤
│   ├── phase-09-api-layer.md           # Phase 9 详细步骤
│   ├── phase-10-desktop-ui.md          # Phase 10 详细步骤
│   └── phase-11-integration-test.md    # Phase 11 详细步骤
│
└── decisions/                          # 技术决策记录 (ADR)
    └── adr-template.md                 # ADR 模板
```

## 阅读顺序建议

### 如果你想了解项目全貌：
1. [产品简介](requirements/product-brief.md) — 这是什么产品
2. [MVP 范围定义](requirements/mvp-scope.md) — 当前版本做什么、不做什么
3. [系统架构总览](architecture/system-architecture.md) — 技术栈和模块关系
4. [开发路线图](execution/development-roadmap.md) — 整体开发计划

### 如果你是新加入的开发者：
1. [代码风格规范](standards/code-style.md) — 先看这个，了解编码约定
2. [Git 工作流](standards/git-workflow.md) — 分支和提交规范
3. [DDD 分层设计](architecture/ddd-layer-design.md) — 理解代码组织方式
4. [测试规范](standards/testing-standard.md) — 测试要求
5. 然后找当前正在执行的 Phase 文档，开始工作

### 如果你要扩展规则：
1. [规则 JSON 格式规范](standards/rule-json-spec.md)
2. [扩展点设计](architecture/extension-points.md)

### 如果你要做技术决策：
1. [ADR 模板](decisions/adr-template.md)
2. 在 `decisions/` 下创建新的 ADR 文件
