package com.dma.core.domain.service;
import com.dma.common.enums.DatabaseType;
import com.dma.core.domain.model.scanner.ScanResult;
import java.nio.file.Path;
import java.util.List;

/** 领域服务：源码扫描器 */
public interface SourceCodeScanner {
    List<ScanResult> scanProject(Path projectPath, DatabaseType source, DatabaseType target);
}
