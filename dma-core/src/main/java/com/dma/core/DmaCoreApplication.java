package com.dma.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * DMA Core 启动类。
 * 在桌面端模式下，此类由 dma-desktop 通过 SpringApplicationBuilder 启动。
 * 也可独立启动用于 API 测试。
 */
@SpringBootApplication
public class DmaCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(DmaCoreApplication.class, args);
    }
}
