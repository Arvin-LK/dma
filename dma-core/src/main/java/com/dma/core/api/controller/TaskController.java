package com.dma.core.api.controller;
import com.dma.common.dto.ApiResponse;
import com.dma.common.enums.DatabaseType;
import com.dma.common.enums.ReportFormat;
import com.dma.core.application.service.MigrationApplicationService;
import com.dma.core.domain.model.connection.ConnectionId;
import com.dma.core.domain.model.migration.MigrationTask;
import com.dma.core.domain.model.migration.TaskId;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {
    private final MigrationApplicationService service;
    public TaskController(MigrationApplicationService service) { this.service = service; }

    @PostMapping
    public ApiResponse<Long> create(@RequestBody Map<String, Object> body) {
        MigrationTask task = service.createTask(
            (String) body.get("taskName"),
            new ConnectionId(((Number) body.get("sourceConnId")).longValue()),
            new ConnectionId(((Number) body.get("targetConnId")).longValue()),
            DatabaseType.valueOf((String) body.get("sourceDbType")),
            DatabaseType.valueOf((String) body.get("targetDbType")));
        return ApiResponse.success(task.getId().value());
    }

    @PostMapping("/{id}/execute")
    public ApiResponse<Long> execute(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        MigrationTask task = service.executeScan(new TaskId(id),
            DatabaseType.valueOf((String) body.get("sourceDbType")),
            DatabaseType.valueOf((String) body.get("targetDbType")));
        return ApiResponse.success(task.getId().value());
    }

    @GetMapping("/{id}/report")
    public ResponseEntity<byte[]> report(@PathVariable Long id, @RequestParam(defaultValue="HTML") String format) {
        byte[] report = service.generateReport(new TaskId(id), ReportFormat.valueOf(format));
        String ext = ReportFormat.valueOf(format).getExtension();
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report" + ext)
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(report);
    }
}
