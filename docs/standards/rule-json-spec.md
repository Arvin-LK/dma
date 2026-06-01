# 规则 JSON 格式规范

## 文件命名

```
{sourceDb}-{targetDb}.json

示例:
  mysql-postgresql.json
  oracle-postgresql.json
  mysql-dameng.json
  sqlserver-gaussdb.json
```

数据库名称使用小写，对应 `DatabaseType` 枚举的 `name()` 小写形式。

---

## 完整 JSON Schema

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "title": "DMA Migration Rule Set",
  "type": "object",
  "required": ["meta", "rules"],
  "properties": {
    "meta": {
      "type": "object",
      "required": ["sourceDb", "targetDb", "version"],
      "properties": {
        "sourceDb": {
          "type": "string",
          "enum": ["MYSQL", "ORACLE", "SQLSERVER", "POSTGRESQL",
                   "GAUSSDB", "GOLDENDB", "OCEANBASE", "DAMENG"],
          "description": "源数据库类型"
        },
        "targetDb": {
          "type": "string",
          "enum": ["MYSQL", "ORACLE", "SQLSERVER", "POSTGRESQL",
                   "GAUSSDB", "GOLDENDB", "OCEANBASE", "DAMENG"],
          "description": "目标数据库类型"
        },
        "version": {
          "type": "string",
          "pattern": "^\\d+\\.\\d+\\.\\d+$",
          "description": "规则集版本号（语义版本）"
        },
        "description": {
          "type": "string",
          "description": "规则集描述"
        },
        "author": {
          "type": "string",
          "description": "规则集作者"
        }
      }
    },
    "rules": {
      "type": "array",
      "items": { "$ref": "#/definitions/rule" }
    }
  },
  "definitions": {
    "rule": {
      "type": "object",
      "required": ["code", "name", "category", "severity",
                   "patternType", "matchPattern"],
      "properties": {
        "code": {
          "type": "string",
          "pattern": "^[A-Z]\\d+[A-Z]+-\\w+-\\d{3}$",
          "description": "规则唯一编码，如 M2PG-FN-001"
        },
        "name": {
          "type": "string",
          "maxLength": 200,
          "description": "规则名称（人类可读）"
        },
        "category": {
          "type": "string",
          "enum": ["BUILTIN_FUNCTION", "SYNTAX", "DATATYPE",
                   "DDL", "DML", "STORED_PROCEDURE", "KEYWORD",
                   "IDENTIFIER", "OTHER"],
          "description": "规则分类"
        },
        "severity": {
          "type": "string",
          "enum": ["ERROR", "WARNING", "INFO"]
        },
        "patternType": {
          "type": "string",
          "enum": ["FUNCTION", "SYNTAX", "DATATYPE", "KEYWORD", "BUILTIN"],
          "description": "匹配模式类型"
        },
        "matchPattern": {
          "type": "string",
          "description": "匹配表达式"
        },
        "replacementPattern": {
          "type": "string",
          "description": "替换表达式（可选，不可自动转换时不填）"
        },
        "description": {
          "type": "string",
          "description": "规则详细描述"
        },
        "exampleSqlSource": {
          "type": "string",
          "description": "源数据库示例 SQL"
        },
        "exampleSqlTarget": {
          "type": "string",
          "description": "目标数据库示例 SQL"
        },
        "sourceMinVersion": {
          "type": "string",
          "description": "源数据库最低版本"
        },
        "sourceMaxVersion": {
          "type": "string",
          "description": "源数据库最高版本"
        },
        "targetMinVersion": {
          "type": "string",
          "description": "目标数据库最低版本"
        },
        "targetMaxVersion": {
          "type": "string",
          "description": "目标数据库最高版本"
        }
      }
    }
  }
}
```

---

## 规则编码规范

格式：`{SrcShort}{N}{TgtShort}-{CategoryShort}-{SeqNum}`

**数据库缩写**：

| 数据库 | 缩写 |
|--------|------|
| MySQL | M |
| Oracle | O |
| SQLServer | S |
| PostgreSQL | PG |
| GaussDB | GDB |
| GoldenDB | G |
| OceanBase | OB |
| 达梦 DM | DM |

**分类缩写**：

| 分类 | 缩写 |
|------|------|
| BUILTIN_FUNCTION | FN |
| SYNTAX | SYN |
| DATATYPE | DT |
| DDL | DDL |
| DML | DML |
| STORED_PROCEDURE | SP |
| KEYWORD | KW |
| IDENTIFIER | ID |
| OTHER | OTH |

**示例**：
- `M2PG-FN-001` — MySQL→PostgreSQL，内置函数，第1条
- `O2PG-FN-005` — Oracle→PostgreSQL，内置函数，第5条
- `M2DM-DT-003` — MySQL→达梦，数据类型，第3条

---

## MatchPattern 语法

### 1. 简单字符串匹配

```json
"matchPattern": "DATETIME",
"replacementPattern": "TIMESTAMP"
```
匹配 SQL 中所有出现 `DATETIME` 的地方。

### 2. 函数匹配（带参数占位符）

```json
"matchPattern": "IFNULL(${expr1}, ${expr2})",
"replacementPattern": "COALESCE(${expr1}, ${expr2})"
```
- `${expr1}`, `${expr2}` 是参数占位符
- 每个占位符匹配一个表达式（嵌套括号、逗号等）

### 3. 正则表达式匹配

```json
"matchPattern": "regex:LIMIT\\s+(\\d+)\\s*,\\s*(\\d+)",
"replacementPattern": "LIMIT $2 OFFSET $1"
```
- 以 `regex:` 前缀标识
- 使用 Java 正则语法
- `$1`, `$2` 引用捕获组

### 4. 关键词匹配

```json
"matchPattern": "keyword:AUTO_INCREMENT",
"replacementPattern": "SERIAL"
```
- 匹配 DDL 中的特定关键词

---

## 规则完整示例

```json
{
  "code": "M2PG-FN-001",
  "name": "IFNULL → COALESCE 函数转换",
  "category": "BUILTIN_FUNCTION",
  "severity": "WARNING",
  "patternType": "FUNCTION",
  "matchPattern": "IFNULL(${expr1}, ${expr2})",
  "replacementPattern": "COALESCE(${expr1}, ${expr2})",
  "description": "MySQL 的 IFNULL(expr1, expr2) 函数在 PostgreSQL 中不存在，需替换为 COALESCE(expr1, expr2)。功能等价：当 expr1 为 NULL 时返回 expr2。",
  "exampleSqlSource": "SELECT IFNULL(phone, 'N/A') AS contact FROM customers;",
  "exampleSqlTarget": "SELECT COALESCE(phone, 'N/A') AS contact FROM customers;",
  "sourceMinVersion": "5.0",
  "targetMinVersion": "9.0"
}
```

---

## 规则编写检查清单

每条规则提交前确认：

- [ ] `code` 唯一，格式符合规范
- [ ] `category` 和 `patternType` 取值在允许范围内
- [ ] `matchPattern` 使用了正确的占位符语法
- [ ] `replacementPattern`（如果有）正确引用了占位符
- [ ] `exampleSqlSource` 和 `exampleSqlTarget` 是合法的 SQL
- [ ] `severity` 合理（真正阻止迁移的用 ERROR，可自动转换的用 WARNING）
- [ ] `description` 清晰说明了为什么需要转换
