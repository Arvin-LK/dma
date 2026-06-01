package com.dma.core.api.controller;

import com.dma.common.dto.ApiResponse;
import com.dma.common.enums.DatabaseType;
import com.dma.core.application.service.MigrationApplicationService;
import com.dma.core.application.service.ProjectScanService;
import com.dma.core.application.service.ProjectScanService.ProjectScanSummary;
import com.dma.core.domain.model.scanner.ScanResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/scan")
public class ScanController {

    private final MigrationApplicationService service;
    private final ProjectScanService projectScanService;

    public ScanController(MigrationApplicationService service, ProjectScanService projectScanService) {
        this.service = service;
        this.projectScanService = projectScanService;
    }

    @PostMapping("/sql")
    public ApiResponse<List<ScanResult>> scanSql(@RequestBody Map<String, Object> body) {
        List<ScanResult> results = service.scanSql(
            (String) body.get("sql"),
            DatabaseType.valueOf((String) body.get("sourceDbType")),
            DatabaseType.valueOf((String) body.get("targetDbType")));
        return ApiResponse.success(results);
    }

    /** 旧版项目扫描接口，保留兼容 */
    @PostMapping("/project")
    public ApiResponse<List<ScanResult>> scanProject(@RequestBody Map<String, Object> body) {
        List<ScanResult> results = service.scanProject(
            (String) body.get("projectPath"),
            DatabaseType.valueOf((String) body.get("sourceDbType")),
            DatabaseType.valueOf((String) body.get("targetDbType")));
        return ApiResponse.success(results);
    }

    /** 新版项目源码扫描——带文件统计和风险分级汇总 */
    @PostMapping("/project-full")
    public ApiResponse<ProjectScanSummary> scanProjectFull(@RequestBody Map<String, Object> body) {
        ProjectScanSummary summary = projectScanService.scan(
            (String) body.get("projectPath"),
            DatabaseType.valueOf((String) body.get("sourceDbType")),
            DatabaseType.valueOf((String) body.get("targetDbType")));
        return ApiResponse.success(summary);
    }
}
