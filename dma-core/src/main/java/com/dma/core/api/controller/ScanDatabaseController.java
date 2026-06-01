package com.dma.core.api.controller;

import com.dma.common.dto.ApiResponse;
import com.dma.common.dto.ScanSummary;
import com.dma.common.enums.DatabaseType;
import com.dma.core.application.service.DatabaseScanService;
import com.dma.core.domain.model.connection.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 数据库兼容性扫描 API。
 * 连接源数据库 → 提取元数据 → 兼容性分析 → 返回汇总结果。
 */
@RestController
@RequestMapping("/api/v1/scan")
public class ScanDatabaseController {

    private static final Logger log = LoggerFactory.getLogger(ScanDatabaseController.class);
    private final DatabaseScanService scanService;

    public ScanDatabaseController(DatabaseScanService scanService) {
        this.scanService = scanService;
    }

    /**
     * 数据库体检——连接源库并执行全面的兼容性扫描。
     *
     * 请求体示例:
     * {
     *   "host": "localhost",
     *   "port": 3306,
     *   "username": "root",
     *   "password": "123456",
     *   "database": "testdb",
     *   "sourceDbType": "MYSQL",
     *   "targetDbType": "POSTGRESQL"
     * }
     */
    @PostMapping("/database")
    public ApiResponse<ScanSummary> scanDatabase(@RequestBody Map<String, Object> body) {
        try {
            DatabaseType sourceDb = DatabaseType.valueOf((String) body.get("sourceDbType"));
            DatabaseType targetDb = DatabaseType.valueOf((String) body.get("targetDbType"));

            DatabaseConnection conn = new DatabaseConnection(
                    "temp-scan",
                    sourceDb,
                    (String) body.get("host"),
                    ((Number) body.get("port")).intValue(),
                    (String) body.get("username"),
                    (String) body.get("password"),   // 实际应用中应解密
                    (String) body.get("database")
            );

            log.info("Starting database scan: {}:{} -> {} -> {}",
                    body.get("host"), body.get("port"), sourceDb, targetDb);

            ScanSummary summary = scanService.scan(conn, targetDb);
            return ApiResponse.success(summary);

        } catch (Exception e) {
            log.error("Database scan failed", e);
            return ApiResponse.error(500, "数据库扫描失败: " + e.getMessage());
        }
    }
}
