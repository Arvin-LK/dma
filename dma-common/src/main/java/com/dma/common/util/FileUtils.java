package com.dma.common.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * 文件操作工具类。
 */
public final class FileUtils {

    private FileUtils() {}

    public static String readFile(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    public static void writeFile(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    public static boolean hasExtension(Path path, Set<String> extensions) {
        String name = path.getFileName().toString().toLowerCase();
        return extensions.stream().anyMatch(ext -> name.endsWith(ext.toLowerCase()));
    }

    public static boolean isSqlFile(Path path) {
        return hasExtension(path, Set.of(".sql"));
    }

    public static boolean isJavaFile(Path path) {
        return hasExtension(path, Set.of(".java"));
    }

    public static boolean isXmlFile(Path path) {
        return hasExtension(path, Set.of(".xml"));
    }
}
