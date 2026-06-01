# MVP 范围定义

## 版本信息

- **版本号**: v1.0.0-MVP
- **目标**: 验证核心价值（SQL 兼容性扫描 + 自动转换），跑通端到端流程

---

## ✅ MVP 要做的事

### 1. 数据库连接管理
- 支持连接 MySQL 5.7+、Oracle 11g+、PostgreSQL 12+、达梦 DM8
- 保存连接配置到本地（密码加密存储）
- 测试连接可用性
- 操作：新建、编辑、删除、查看连接列表

### 2. SQL 兼容性扫描
- 支持扫描单条 SQL 语句
- 支持扫描 .sql 文件
- 支持扫描数据库中的存储过程/函数/视图（DDL 提取）
- 输出每条 SQL 的兼容性级别：COMPATIBLE / AUTO_CONVERTIBLE / MANUAL_REVIEW / INCOMPATIBLE
- 输出具体的规则匹配详情

### 3. SQL 自动转换
- 对 AUTO_CONVERTIBLE 级别的 SQL 自动生成转换后的 SQL
- 支持的转换类型：
  - 内置函数名称转换（IFNULL→COALESCE, NVL→COALESCE, NOW()→CURRENT_TIMESTAMP 等）
  - 分页语法转换（LIMIT m,n → LIMIT n OFFSET m）
  - 数据类型映射提示（DATETIME→TIMESTAMP 等）
  - 标识符引用符转换（反引号→双引号）
  - DDL 语法差异处理（AUTO_INCREMENT→SERIAL 等）

### 4. MyBatis Mapper XML 扫描
- 解析 Mapper XML 文件
- 提取 `<select>`, `<insert>`, `<update>`, `<delete>` 中的 SQL
- 对提取的 SQL 执行兼容性分析

### 5. Java 源码扫描
- 扫描 .java 文件中的 SQL 字符串字面量
- 识别常见的 SQL 构建模式（字符串拼接、StringBuilder 等）
- 对识别到的 SQL 执行兼容性分析

### 6. 报告生成
- HTML 格式报告（含图表和统计）
- PDF 格式报告
- Word (.docx) 格式报告
- 报告内容：迁移概览、问题列表、统计图表、建议汇总

### 7. 3 条迁移路径
- MySQL → PostgreSQL（约 30 条规则）
- Oracle → PostgreSQL（约 25 条规则）
- MySQL → 达梦 DM（约 25 条规则）

---

## ❌ MVP 不做的事

### 数据库相关
- ❌ 不支持 SQLServer 作为源库（Phase 2+）
- ❌ 不支持 GaussDB / GoldenDB / OceanBase 作为目标库（Phase 2+）
- ❌ 不执行实际的数据库迁移（仅分析和转换）
- ❌ 不支持数据迁移（仅 DDL/DML 分析）

### 存储过程
- ❌ 不支持存储过程/函数体的深度分析（仅提取 DDL）
- ❌ 不转换存储过程语言（PL/SQL→PL/pgSQL 等，Phase 2+）
- ❌ 不支持触发器分析（Phase 2+）

### IDEA 插件
- ❌ 不实现 IDEA 插件的实际功能（仅建骨架占位，Phase 3+）
- ❌ 不实现 SQL 实时检测
- ❌ 不实现 Alt+Enter 自动修复

### 其他
- ❌ 不实现 AI 迁移顾问的实际对接（仅预留接口）
- ❌ 不实现许可证校验（仅预留接口）
- ❌ 不实现远程规则市场
- ❌ 不实现团队协作/多租户
- ❌ 不实现自动化迁移流水线（CI/CD 集成）
- ❌ 不做国际化（MVP 界面为中文）

---

## MVP 验收标准

1. 桌面端应用可在 Windows 10/11 上启动运行
2. 能连接 MySQL、Oracle、PG、达梦数据库
3. 对 3 条路径的测试 SQL 集，兼容性扫描准确率 ≥ 90%
4. 对 AUTO_CONVERTIBLE 规则，转换准确率 ≥ 95%
5. Mapper XML 和 Java 源码扫描能正确提取 SQL
6. HTML/PDF/Word 三种格式报告能正确生成
7. `mvn test` 全部通过，覆盖率 ≥ 70%
