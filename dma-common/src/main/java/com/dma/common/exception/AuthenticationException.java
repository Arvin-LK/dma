package com.dma.common.exception;

/** 数据库认证失败异常 */
public class AuthenticationException extends ConnectionException {
    public AuthenticationException(String host, String username) {
        super("CONN_002", "认证失败: " + username + "@" + host);
    }
}
