package com.dma.core.infrastructure.parser;

import com.dma.common.exception.ParseException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * JSQLParser 封装类。
 * 提供 SQL 解析、格式化、AST 操作的基础能力。
 */
@Component
public class JsqlParserWrapper {

    private static final Logger log = LoggerFactory.getLogger(JsqlParserWrapper.class);

    /**
     * 解析 SQL 字符串为 AST Statement。
     * @throws ParseException 当 SQL 语法无效时
     */
    public Statement parse(String sql) {
        try {
            return CCJSqlParserUtil.parse(sql);
        } catch (Exception e) {
            log.debug("JSQLParser failed to parse SQL: {}", sql.substring(0, Math.min(100, sql.length())));
            throw new ParseException(sql, e);
        }
    }

    /**
     * 尝试解析 SQL，失败时返回 null 而不是抛异常。
     */
    public Statement tryParse(String sql) {
        try {
            return CCJSqlParserUtil.parse(sql);
        } catch (Exception e) {
            log.debug("Failed to parse SQL (non-fatal): {}", e.getMessage());
            return null;
        }
    }

    /**
     * 格式化 Statement 为 SQL 字符串。
     */
    public String format(Statement statement) {
        return statement.toString();
    }

    /**
     * 检测 SQL 类型简述（用于日志）。
     */
    public String detectType(String sql) {
        String upper = sql.trim().toUpperCase();
        if (upper.startsWith("SELECT")) return "SELECT";
        if (upper.startsWith("INSERT")) return "INSERT";
        if (upper.startsWith("UPDATE")) return "UPDATE";
        if (upper.startsWith("DELETE")) return "DELETE";
        if (upper.startsWith("CREATE")) return "CREATE";
        if (upper.startsWith("ALTER")) return "ALTER";
        if (upper.startsWith("DROP")) return "DROP";
        return "OTHER";
    }
}
