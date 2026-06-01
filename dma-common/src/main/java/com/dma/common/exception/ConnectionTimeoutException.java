package com.dma.common.exception;

/** 连接超时异常 */
public class ConnectionTimeoutException extends ConnectionException {
    public ConnectionTimeoutException(String host, int port) {
        super("CONN_003", "连接超时: " + host + ":" + port);
    }
}
