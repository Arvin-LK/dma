package com.dma.core.infrastructure.converter;

import com.dma.common.enums.DatabaseType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 存储过程/函数/触发器/视图 结构性转换引擎。
 *
 * 处理不同数据库之间 CREATE PROCEDURE/FUNCTION/TRIGGER/VIEW 的语法差异。
 * 与 SqlConvertStrategy 互补：本类处理结构语法，规则引擎处理函数名/数据类型。
 */
@Component
public class StoredProcedureConverter {

    private static final Logger log = LoggerFactory.getLogger(StoredProcedureConverter.class);

    /**
     * 转换结果，包含转换后的 DDL 和变更说明。
     */
    public record ConvertResult(
            String originalDdl,
            String convertedDdl,
            String objectType,       // PROCEDURE, FUNCTION, TRIGGER, VIEW
            String objectName,
            List<String> changes     // 每项变更的说明
    ) {}

    /**
     * 转换存储过程/函数/触发器/视图 DDL。
     */
    public ConvertResult convert(String ddl, DatabaseType source, DatabaseType target) {
        String objectType = detectObjectType(ddl);
        String objectName = extractObjectName(ddl, objectType);
        List<String> changes = new ArrayList<>();

        String result = ddl;

        // 1. 移除 DELIMITER（MySQL 特有）
        result = removeDelimiter(result, changes);

        // 2. CREATE → CREATE OR REPLACE（PG/GaussDB 风格）
        result = addCreateOrReplace(result, objectType, target, changes);

        // 3. 移除 DEFINER 子句（MySQL 特有）
        result = removeDefiner(result, changes);

        // 4. 转换函数返回值位置（MySQL: RETURNS 在参数后, PG: RETURNS 在 AS 前）
        if ("FUNCTION".equals(objectType)) {
            result = convertFunctionReturns(result, target, changes);
        }

        // 5. BEGIN...END 结构转换（MySQL → GaussDB AS/BEGIN/END）
        result = convertBeginEnd(result, objectType, target, changes);

        // 6. LANGUAGE 声明（GaussDB 需要）
        result = addLanguageSpec(result, objectType, target, changes);

        // 7. 参数模式转换（IN/OUT/INOUT 位置调整）
        result = convertParameterModes(result, target, changes);

        // 8. 移除 MySQL 特有的 SQL SECURITY 等子句
        result = removeMySqlSpecificClauses(result, changes);

        // 9. TRIGGER 特殊处理
        if ("TRIGGER".equals(objectType)) {
            result = convertTriggerSyntax(result, source, target, changes);
        }

        // 10. 结尾分号规范化
        result = normalizeTerminator(result, changes);

        return new ConvertResult(ddl, result.trim(), objectType, objectName, changes);
    }

    // ==================== 检测 ====================

    private String detectObjectType(String ddl) {
        String upper = ddl.toUpperCase().replaceAll("\\s+", " ");
        if (upper.contains("CREATE") && upper.contains("TRIGGER")) return "TRIGGER";
        if (upper.contains("CREATE") && upper.contains("FUNCTION")) return "FUNCTION";
        if (upper.contains("CREATE") && upper.contains("PROCEDURE")) return "PROCEDURE";
        if (upper.contains("CREATE") && (upper.contains("VIEW") || upper.contains("OR REPLACE VIEW"))) return "VIEW";
        return "UNKNOWN";
    }

    private String extractObjectName(String ddl, String type) {
        try {
            String upper = ddl.toUpperCase().replaceAll("\\s+", " ");
            String keyword = switch (type) {
                case "PROCEDURE" -> "PROCEDURE ";
                case "FUNCTION" -> "FUNCTION ";
                case "TRIGGER" -> "TRIGGER ";
                case "VIEW" -> "VIEW ";
                default -> "PROCEDURE ";
            };
            int idx = upper.indexOf(keyword);
            if (idx >= 0) {
                String rest = ddl.substring(idx + keyword.length()).trim();
                // Handle IF NOT EXISTS
                if (rest.toUpperCase().startsWith("IF NOT EXISTS ")) {
                    rest = rest.substring(14).trim();
                }
                int end = rest.indexOf('(');
                if (end > 0) return rest.substring(0, end).trim().replace("`", "");
                // For TRIGGER, name is before ON/BEFORE/AFTER
                if ("TRIGGER".equals(type)) {
                    String[] words = rest.split("\\s+");
                    return words[0].replace("`", "");
                }
                return rest.split("\\s+")[0].replace("`", "");
            }
        } catch (Exception ignored) {}
        return type.toLowerCase() + "_unnamed";
    }

    // ==================== 各转换步骤 ====================

    private String removeDelimiter(String ddl, List<String> changes) {
        String result = ddl.replaceAll("(?i)DELIMITER\\s+\\$\\$\\s*", "")
                          .replaceAll("(?i)DELIMITER\\s+;\\s*", "")
                          .replaceAll("(?i)DELIMITER\\s+//\\s*", "");
        if (!result.equals(ddl)) {
            changes.add("移除 DELIMITER 声明（目标数据库不需要）");
        }
        return result;
    }

    private String addCreateOrReplace(String ddl, String objectType, DatabaseType target, List<String> changes) {
        if (target == DatabaseType.GAUSSDB || target == DatabaseType.POSTGRESQL || target == DatabaseType.DAMENG) {
            String pattern = "(?i)CREATE\\s+(OR\\s+REPLACE\\s+)?(" + objectType + "\\s+)";
            if (!ddl.toUpperCase().contains("OR REPLACE")) {
                String result = ddl.replaceFirst(pattern, "CREATE OR REPLACE $2");
                if (!result.equals(ddl)) {
                    changes.add("CREATE → CREATE OR REPLACE（支持重复执行）");
                }
                return result;
            }
        }
        return ddl;
    }

    private String removeDefiner(String ddl, List<String> changes) {
        String result = ddl.replaceAll("(?i)DEFINER\\s*=\\s*`[^`]+`@`[^`]+`\\s*", "")
                          .replaceAll("(?i)DEFINER\\s*=\\s*[^\\s]+\\s*", "");
        if (!result.equals(ddl)) {
            changes.add("移除 DEFINER 子句（目标数据库不支持）");
        }
        return result;
    }

    private String convertFunctionReturns(String ddl, DatabaseType target, List<String> changes) {
        if (target == DatabaseType.GAUSSDB || target == DatabaseType.DAMENG) {
            // MySQL: CREATE FUNCTION name(params) RETURNS type BEGIN ...
            // GaussDB: CREATE OR REPLACE FUNCTION name(params) RETURN type AS BEGIN ...
            Pattern p = Pattern.compile("(?i)RETURNS\\s+(\\S+)", Pattern.DOTALL);
            Matcher m = p.matcher(ddl);
            if (m.find()) {
                String returnType = m.group(1);
                String result = m.replaceFirst("RETURN " + returnType);
                if (!result.equals(ddl)) {
                    changes.add("RETURNS → RETURN（目标数据库语法）");
                }
                return result;
            }
        }
        return ddl;
    }

    private String convertBeginEnd(String ddl, String objectType, DatabaseType target, List<String> changes) {
        if (target == DatabaseType.GAUSSDB) {
            // MySQL: CREATE PROCEDURE name(params) BEGIN ... END
            // GaussDB: CREATE OR REPLACE PROCEDURE name(params) AS BEGIN ... END;
            String upper = ddl.toUpperCase().replaceAll("\\s+", " ");

            // 检查是否已有 AS 关键字
            if (!upper.contains(" AS ") && !upper.contains(" AS\n") && !upper.contains("\nAS ")) {
                // 在 BEGIN 前插入 AS
                if (upper.contains(" BEGIN ")) {
                    ddl = ddl.replaceFirst("(?i)\\bBEGIN\\b", "AS BEGIN");
                    changes.add("添加 AS 关键字（GaussDB 语法要求）");
                } else if (upper.contains(" BEGIN\n")) {
                    ddl = ddl.replaceFirst("(?i)\\bBEGIN\\b", "AS BEGIN");
                    changes.add("添加 AS 关键字");
                }
            }

            // 在最后一个 END 后面加分号（如果缺少）
            if (!ddl.trim().endsWith(";") && !ddl.trim().endsWith("$$")) {
                ddl = ddl.trim() + ";";
                changes.add("补充结尾分号");
            }

            // 移除末尾的 $$ 标记
            ddl = ddl.replaceAll("\\$\\$\\s*$", ";");
        }
        return ddl;
    }

    private String addLanguageSpec(String ddl, String objectType, DatabaseType target, List<String> changes) {
        if (target == DatabaseType.GAUSSDB || target == DatabaseType.DAMENG) {
            String upper = ddl.toUpperCase();
            if (!upper.contains("LANGUAGE") && ("FUNCTION".equals(objectType) || "PROCEDURE".equals(objectType))) {
                // 在最后一个 END 之前添加
                String langSpec = target == DatabaseType.GAUSSDB
                        ? "\nLANGUAGE plpgsql"
                        : "\nLANGUAGE SQL";
                if (upper.contains(" END;")) {
                    ddl = ddl.replaceFirst("(?i)\\bEND\\s*;", langSpec + "\nEND;");
                    changes.add("添加 LANGUAGE 声明");
                } else if (upper.contains(" END")) {
                    ddl = ddl.replaceFirst("(?i)\\bEND\\b", langSpec + "\nEND");
                    changes.add("添加 LANGUAGE 声明");
                }
            }
        }
        return ddl;
    }

    private String convertParameterModes(String ddl, DatabaseType target, List<String> changes) {
        if (target == DatabaseType.GAUSSDB || target == DatabaseType.DAMENG) {
            // MySQL: IN param type, OUT param type
            // GaussDB: param IN type, param OUT type (参数名在前)
            // 其实大部分情况下位置相同，只处理明显的差异
            String upper = ddl.toUpperCase();
            // MySQL 的 IN/OUT/INOUT 位置和 GaussDB 一样（参数名后），所以通常不需要转换
            // 不需要动，但记录一下
            if (upper.contains(" IN ") || upper.contains(" OUT ")) {
                // 不做实际转换，因为大多数情况兼容
            }
        }
        return ddl;
    }

    private String removeMySqlSpecificClauses(String ddl, List<String> changes) {
        // SQL SECURITY DEFINER/INVOKER
        if (ddl.toUpperCase().contains("SQL SECURITY")) {
            ddl = ddl.replaceAll("(?i)\\s*SQL\\s+SECURITY\\s+(DEFINER|INVOKER)\\s*", " ");
            changes.add("移除 SQL SECURITY 子句");
        }
        // CHARACTER SET / COLLATE
        if (ddl.toUpperCase().contains("CHARSET") || ddl.toUpperCase().contains("CHARACTER SET")) {
            ddl = ddl.replaceAll("(?i)\\s+(DEFAULT\\s+)?(CHARACTER\\s+SET|CHARSET)\\s*=\\s*\\S+", "");
            changes.add("移除字符集声明");
        }
        if (ddl.toUpperCase().contains("COLLATE")) {
            ddl = ddl.replaceAll("(?i)\\s+COLLATE\\s*=\\s*\\S+", "");
            changes.add("移除 COLLATE 声明");
        }
        // COMMENT 'xxx' in procedure
        if (ddl.toUpperCase().contains("COMMENT ")) {
            ddl = ddl.replaceAll("(?i)\\s+COMMENT\\s+'[^']*'", "");
            changes.add("移除 COMMENT 子句（如有需要可用 PG COMMENT ON 语法）");
        }
        return ddl;
    }

    private String convertTriggerSyntax(String ddl, DatabaseType source, DatabaseType target, List<String> changes) {
        if (target == DatabaseType.GAUSSDB || target == DatabaseType.POSTGRESQL) {
            // MySQL: CREATE TRIGGER name BEFORE/AFTER event ON table FOR EACH ROW BEGIN ... END
            // PG/GaussDB: CREATE TRIGGER name BEFORE/AFTER event ON table FOR EACH ROW EXECUTE FUNCTION func_name()
            // GaussDB 支持兼容 MySQL 的触发器语法，但 PG 不支持
            if (target == DatabaseType.POSTGRESQL) {
                changes.add("⚠ PostgreSQL 触发器需重写为 EXECUTE FUNCTION 形式，请人工审核");
            }
        }
        return ddl;
    }

    private String normalizeTerminator(String ddl, List<String> changes) {
        // 移除末尾多余符号
        ddl = ddl.replaceAll("\\$\\$\\s*$", ";");
        // 确保以分号结尾
        ddl = ddl.trim();
        if (!ddl.endsWith(";") && !ddl.endsWith("$$")) {
            ddl = ddl + ";";
        }
        return ddl;
    }

    /**
     * 对存储过程/函数 DDL 内的 SQL 语句进行兼容性分析（复用规则引擎的结果）。
     * 返回经过函数名/数据类型转换后的 DDL。
     */
    public String applySqlLevelFixes(String ddl, DatabaseType source, DatabaseType target) {
        // 基础函数名替换（与 FunctionNameConverter 保持一致）
        String result = ddl;
        result = result.replaceAll("(?i)\\bIFNULL\\s*\\(", "COALESCE(");
        result = result.replaceAll("(?i)\\bNVL\\s*\\(", "COALESCE(");
        result = result.replaceAll("(?i)\\bNOW\\s*\\(\\)", "CURRENT_TIMESTAMP");
        result = result.replaceAll("(?i)\\bSYSDATE\\b", "CURRENT_TIMESTAMP");
        result = result.replaceAll("(?i)\\bUUID\\s*\\(\\)", "gen_random_uuid()");
        // 数据类型替换
        result = result.replaceAll("(?i)\\bDATETIME\\b", "TIMESTAMP");
        result = result.replaceAll("(?i)\\bTINYINT\\b", "SMALLINT");
        result = result.replaceAll("(?i)\\bMEDIUMTEXT\\b", "TEXT");
        result = result.replaceAll("(?i)\\bBLOB\\b", "BYTEA");
        result = result.replaceAll("(?i)\\bVARCHAR2\\b", "VARCHAR");
        result = result.replaceAll("(?i)\\bNUMBER\\s*\\(", "NUMERIC(");
        result = result.replaceAll("(?i)\\bCLOB\\b", "TEXT");
        return result;
    }
}
