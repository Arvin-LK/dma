# Phase 11: 集成测试与验证

## 目标

覆盖 3 条迁移路径的端到端测试，确保 MVP 质量。

## 前置条件

- [ ] Phase 10 完成（桌面端可运行）

## 步骤

### 11.1 测试 Fixtures 准备

```
dma-test/src/test/resources/fixtures/
├── sql/
│   ├── mysql/functions.sql, syntax.sql, datatypes.sql, ddl.sql
│   └── oracle/functions.sql, syntax.sql
├── xml/UserMapper.xml, ProductMapper.xml
└── java/UserService.java, OrderDao.java
```

### 11.2 集成测试用例

| 测试类 | 覆盖内容 |
|--------|---------|
| `MySqlToPgIntegrationTest` | MySQL→PG 端到端：连接→扫描→转换→报告 |
| `OracleToPgIntegrationTest` | Oracle→PG 端到端 |
| `MySqlToDamengIntegrationTest` | MySQL→达梦 端到端 |
| `ReportGenerationTest` | HTML/PDF/Word 三种格式验证 |
| `ProjectScanIntegrationTest` | Java 源码 + Mapper XML 扫描 |
| `RuleEngineIntegrationTest` | 规则加载→匹配→转换全流程 |

### 11.3 运行全量测试

```bash
mvn clean test
# 预期：全部测试通过，覆盖率 ≥ 70%
```

## 验证标准

1. `mvn clean test` 全量通过
2. JaCoCo 覆盖率报告 ≥ 70%
3. 桌面端冒烟测试："连接→扫描→报告"完整流程走通
