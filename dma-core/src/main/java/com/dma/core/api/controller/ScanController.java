package com.dma.core.api.controller;
import com.dma.common.dto.ApiResponse;
import com.dma.common.enums.DatabaseType;
import com.dma.core.application.service.MigrationApplicationService;
import com.dma.core.domain.model.scanner.ScanResult;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/scan")
public class ScanController {
    private final MigrationApplicationService service;
    public ScanController(MigrationApplicationService service) { this.service = service; }

    @PostMapping("/sql")
    public ApiResponse<List<ScanResult>> scanSql(@RequestBody Map<String, Object> body) {
        List<ScanResult> results = service.scanSql(
            (String) body.get("sql"),
            DatabaseType.valueOf((String) body.get("sourceDbType")),
            DatabaseType.valueOf((String) body.get("targetDbType")));
        return ApiResponse.success(results);
    }

    @PostMapping("/project")
    public ApiResponse<List<ScanResult>> scanProject(@RequestBody Map<String, Object> body) {
        List<ScanResult> results = service.scanProject(
            (String) body.get("projectPath"),
            DatabaseType.valueOf((String) body.get("sourceDbType")),
            DatabaseType.valueOf((String) body.get("targetDbType")));
        return ApiResponse.success(results);
    }
}
