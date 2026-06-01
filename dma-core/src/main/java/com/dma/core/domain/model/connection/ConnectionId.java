package com.dma.core.domain.model.connection;

/** 数据库连接唯一标识 */
public record ConnectionId(Long value) {
    public ConnectionId {
        if (value == null || value <= 0) throw new IllegalArgumentException("ConnectionId must be positive");
    }
}
