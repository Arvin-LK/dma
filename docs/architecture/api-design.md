# REST API 设计规范

## 基础约定

| 项目 | 约定 |
|------|------|
| **Base URL** | `http://localhost:8080/api/v1` |
| **Content-Type** | `application/json; charset=UTF-8` |
| **字符编码** | UTF-8 |
| **日期格式** | ISO 8601 (`yyyy-MM-dd'T'HH:mm:ss`) |
| **认证方式** | MVP 阶段无认证（本地应用），后续使用 JWT |

---

## 统一响应格式

### 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": { ... },
  "timestamp": "2026-06-01T10:30:00"
}
```

### 分页响应

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "items": [ ... ],
    "total": 100,
    "page": 1,
    "size": 20,
    "totalPages": 5
  },
  "timestamp": "2026-06-01T10:30:00"
}
```

### 错误响应

```json
{
  "code": 400,
  "message": "参数校验失败",
  "errors": [
    {
      "field": "host",
      "message": "主机地址不能为空"
    }
  ],
  "timestamp": "2026-06-01T10:30:00"
}
```

---

## 错误码规范

| 范围 | 含义 | 示例 |
|------|------|------|
| 200 | 成功 | — |
| 400 | 客户端错误（参数校验） | 必填字段为空、格式错误 |
| 404 | 资源不存在 | 连接 ID 不存在 |
| 409 | 资源冲突 | 连接名重复 |
| 500 | 服务器内部错误 | 数据库连接失败、文件读取异常 |

### 业务错误码（内嵌在 data 中）

| 错误码 | 含义 |
|--------|------|
| `CONN_001` | 数据库连接失败 |
| `CONN_002` | 连接认证失败 |
| `CONN_003` | 连接超时 |
| `PARSE_001` | SQL 语法解析失败 |
| `RULE_001` | 规则文件未找到 |
| `RULE_002` | 规则格式错误 |
| `SCAN_001` | 项目路径不存在 |
| `SCAN_002` | 文件过大（超过限制） |
| `REPORT_001` | 报告生成失败 |
| `TASK_001` | 任务不存在 |

---

## API 端点完整列表

### 1. 数据库连接管理

| 方法 | URL | 说明 |
|------|-----|------|
| `POST` | `/api/v1/connections` | 创建连接 |
| `GET` | `/api/v1/connections` | 连接列表（分页） |
| `GET` | `/api/v1/connections/{id}` | 连接详情 |
| `PUT` | `/api/v1/connections/{id}` | 更新连接 |
| `DELETE` | `/api/v1/connections/{id}` | 删除连接 |
| `POST` | `/api/v1/connections/test` | 测试连接（不需要先保存） |
| `POST` | `/api/v1/connections/{id}/test` | 测试已保存的连接 |

### 2. SQL 扫描与转换

| 方法 | URL | 说明 |
|------|-----|------|
| `POST` | `/api/v1/scan/sql` | 扫描单条 SQL |
| `POST` | `/api/v1/scan/file` | 扫描 SQL 文件 |
| `POST` | `/api/v1/scan/project` | 扫描项目源码 |

### 3. SQL 转换

| 方法 | URL | 说明 |
|------|-----|------|
| `POST` | `/api/v1/convert/sql` | 转换单条 SQL |
| `POST` | `/api/v1/convert/batch` | 批量转换 SQL 列表 |

### 4. 迁移任务

| 方法 | URL | 说明 |
|------|-----|------|
| `POST` | `/api/v1/tasks` | 创建迁移任务 |
| `GET` | `/api/v1/tasks` | 任务列表 |
| `GET` | `/api/v1/tasks/{id}` | 任务详情 |
| `PUT` | `/api/v1/tasks/{id}` | 更新任务 |
| `DELETE` | `/api/v1/tasks/{id}` | 删除任务 |
| `POST` | `/api/v1/tasks/{id}/execute` | 执行迁移扫描 |
| `GET` | `/api/v1/tasks/{id}/results` | 获取扫描结果 |
| `PUT` | `/api/v1/tasks/{taskId}/results/{resultId}/resolve` | 标记结果已处理 |

### 5. 报告

| 方法 | URL | 说明 |
|------|-----|------|
| `GET` | `/api/v1/tasks/{id}/report?format=HTML` | 导出 HTML 报告 |
| `GET` | `/api/v1/tasks/{id}/report?format=PDF` | 导出 PDF 报告 |
| `GET` | `/api/v1/tasks/{id}/report?format=WORD` | 导出 Word 报告 |

### 6. 规则管理（Phase 2+ 功能性完善）

| 方法 | URL | 说明 |
|------|-----|------|
| `GET` | `/api/v1/rules` | 规则列表（按源/目标筛选） |
| `POST` | `/api/v1/rules` | 添加自定义规则 |
| `PUT` | `/api/v1/rules/{id}` | 更新规则 |
| `DELETE` | `/api/v1/rules/{id}` | 删除自定义规则 |
| `POST` | `/api/v1/rules/import` | 导入规则文件 |
| `GET` | `/api/v1/rules/export` | 导出规则文件 |

### 7. 系统信息

| 方法 | URL | 说明 |
|------|-----|------|
| `GET` | `/api/v1/system/health` | 健康检查 |
| `GET` | `/api/v1/system/info` | 版本信息、规则统计 |
| `GET` | `/api/v1/system/database-types` | 支持的数据库类型列表 |

---

## 请求/响应示例

### POST /api/v1/scan/sql

**请求**:
```json
{
  "sql": "SELECT IFNULL(name, 'N/A') FROM users LIMIT 10, 20;",
  "sourceDbType": "MYSQL",
  "targetDbType": "POSTGRESQL"
}
```

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "originalSql": "SELECT IFNULL(name, 'N/A') FROM users LIMIT 10, 20;",
    "results": [
      {
        "ruleCode": "M2PG-FN-001",
        "ruleName": "IFNULL → COALESCE",
        "severity": "WARNING",
        "compatibilityLevel": "AUTO_CONVERTIBLE",
        "sourceFragment": "IFNULL(name, 'N/A')",
        "suggestedFragment": "COALESCE(name, 'N/A')",
        "message": "MySQL IFNULL 函数需替换为 PG COALESCE"
      },
      {
        "ruleCode": "M2PG-SYN-001",
        "ruleName": "LIMIT 分页转换",
        "severity": "ERROR",
        "compatibilityLevel": "AUTO_CONVERTIBLE",
        "sourceFragment": "LIMIT 10, 20",
        "suggestedFragment": "LIMIT 20 OFFSET 10",
        "message": "MySQL LIMIT m,n 需转换为 PG LIMIT n OFFSET m"
      }
    ],
    "summary": {
      "total": 2,
      "compatible": 0,
      "autoConvertible": 2,
      "manualReview": 0,
      "incompatible": 0
    }
  },
  "timestamp": "2026-06-01T10:30:00"
}
```

---

## 命名规范

- **URL**: 全小写，单词间用连字符 `-`，资源名用复数
- **Query 参数**: camelCase（如 `?sourceDbType=MYSQL`）
- **JSON 字段**: camelCase（如 `sourceDbType`）
- **Header**: kebab-case（如 `X-Request-Id`）
