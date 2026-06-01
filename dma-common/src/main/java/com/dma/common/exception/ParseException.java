package com.dma.common.exception;

/** SQL 解析异常 */
public class ParseException extends DmaException {
    public ParseException(String sql, Throwable cause) {
        super("PARSE_001", "SQL 解析失败: " + truncate(sql), cause);
    }
    private static String truncate(String s) { return s.length() > 100 ? s.substring(0, 100) + "..." : s; }
}
