# Git 工作流规范

## 分支策略

```
main (稳定分支，只接受 merge)
  │
  ├── develop (开发主分支)
  │     │
  │     ├── feature/phase-01-project-init
  │     ├── feature/phase-02-common-module
  │     ├── feature/phase-03-domain-layer
  │     ├── feature/phase-04-rule-engine
  │     ├── feature/phase-05-sql-parser
  │     ├── feature/phase-06-source-scanner
  │     ├── feature/phase-07-report-generator
  │     ├── feature/phase-08-application-layer
  │     ├── feature/phase-09-api-layer
  │     ├── feature/phase-10-desktop-ui
  │     └── feature/phase-11-integration-test
  │
  └── hotfix/* (紧急修复)
```

### 分支命名规则

| 分支类型 | 命名格式 | 示例 |
|---------|---------|------|
| 功能开发 | `feature/phase-XX-description` | `feature/phase-04-rule-engine` |
| 问题修复 | `fix/issue-description` | `fix/sql-parser-npe` |
| 紧急修复 | `hotfix/issue-description` | `hotfix/report-encoding-error` |
| 文档更新 | `docs/description` | `docs/api-examples` |

---

## Commit Message 格式

```
<type>(<scope>): <subject>

[body]

[footer]
```

### Type 类型

| Type | 说明 |
|------|------|
| `feat` | 新功能 |
| `fix` | Bug 修复 |
| `docs` | 文档更新 |
| `style` | 代码格式调整（不影响功能） |
| `refactor` | 代码重构 |
| `test` | 测试相关 |
| `chore` | 构建/工具/依赖更新 |

### Scope 范围

范围使用模块名或 Phase 编号：

```
feat(core): 实现 JsonFileRuleLoader
feat(desktop): 添加连接管理页面
fix(core): 修复 JSQLParser 解析 LIMIT 子句的空指针异常
docs(standards): 更新代码风格规范
test(core): 添加 MySQL→PG 函数转换测试用例
chore(root): 升级 Spring Boot 至 3.2.5
```

### 推荐的提交格式

```
feat(core): 实现 FunctionNameConverter 转换策略

- 支持 IFNULL → COALESCE 函数名替换
- 基于 JSQLParser AST 遍历实现
- 添加对应的单元测试

Refs: #12
```

---

## 工作流规则

### 开发阶段（当前模式 — 单人开发）

1. 直接从 `develop` 创建 feature 分支
2. 每个 Phase 一个 feature 分支
3. Phase 完成后合并到 `develop`
4. 小步提交，每完成一个任务（如一个类的实现 + 测试）就提交一次

### 合并检查清单

合并前确认：
- [ ] `mvn clean compile` 编译通过
- [ ] `mvn test` 全部测试通过
- [ ] 新增代码有对应的单元测试
- [ ] 没有遗留的 TODO/FIXME（或已创建 issue 跟踪）
- [ ] 没有 `System.out.println` 残留

### .gitignore 确保项

确保以下不被提交：
- `target/` 编译产物
- `.idea/` IDE 配置
- `*.sqlite` 本地数据库文件
- `*.log` 日志文件
- `application-local.yml` 本地配置

---

## 版本标签

- 每个 Phase 完成后打标签: `v0.1-phase01`, `v0.2-phase02`, ...
- MVP 完成时打标签: `v1.0.0-mvp`
