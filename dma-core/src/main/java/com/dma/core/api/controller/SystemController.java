package com.dma.core.api.controller;
import com.dma.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/system")
public class SystemController {
    @GetMapping("/health")
    public ApiResponse<String> health() { return ApiResponse.success("UP"); }

    @GetMapping("/info")
    public ApiResponse<Map<String, Object>> info() {
        return ApiResponse.success(Map.of("version", "1.0.0-SNAPSHOT", "name", "Database Migration Assistant",
            "databaseTypes", java.util.List.of("MYSQL","ORACLE","SQLSERVER","POSTGRESQL","GAUSSDB","GOLDENDB","OCEANBASE","DAMENG")));
    }
}
