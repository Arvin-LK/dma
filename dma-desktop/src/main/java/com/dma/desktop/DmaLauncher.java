package com.dma.desktop;

/**
 * DMA 启动器。
 *
 * 解决 Java 11+ 环境下 "缺少 JavaFX 运行时组件" 的问题。
 * JavaFX 的 Application.launch() 必须由非 Application 子类调用。
 *
 * 运行方式：
 *   1. Maven:  mvn javafx:run -pl dma-desktop
 *   2. IDEA:   直接运行本类的 main 方法
 *              (需确保 javafx-maven-plugin 已正确配置 module-path)
 */
public class DmaLauncher {

    public static void main(String[] args) {
        // 委托给 JavaFX Application 子类启动
        // DmaDesktopApplication.init() 中会启动 Spring Boot 后端
        DmaDesktopApplication.launch(DmaDesktopApplication.class, args);
    }
}
