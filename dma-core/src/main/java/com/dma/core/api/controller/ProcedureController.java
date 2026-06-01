package com.dma.core.api.controller;

import com.dma.common.dto.ApiResponse;
import com.dma.common.enums.DatabaseType;
import com.dma.core.infrastructure.converter.StoredProcedureConverter;
import com.dma.core.infrastructure.converter.StoredProcedureConverter.ConvertResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 存储过程/函数/触发器/视图 迁移 API。
 */
@RestController
@RequestMapping("/api/v1/convert")
public class ProcedureController {

    private static final Logger log = LoggerFactory.getLogger(ProcedureController.class);
    private final StoredProcedureConverter converter;

    public ProcedureController(StoredProcedureConverter converter) {
        this.converter = converter;
    }

    /**
     * 转换存储过程/函数/触发器/视图。
     *
     * 请求体: { "ddl": "CREATE PROCEDURE...", "sourceDbType": "MYSQL", "targetDbType": "GAUSSDB" }
     */
    @PostMapping("/procedure")
    public ApiResponse<Map<String, Object>> convertProcedure(@RequestBody Map<String, Object> body) {
        String ddl = (String) body.get("ddl");
        DatabaseType source = DatabaseType.valueOf((String) body.get("sourceDbType"));
        DatabaseType target = DatabaseType.valueOf((String) body.get("targetDbType"));

        // 1. 结构性转换
        ConvertResult result = converter.convert(ddl, source, target);

        // 2. SQL 级修复（函数名+数据类型）
        String finalDdl = converter.applySqlLevelFixes(result.convertedDdl(), source, target);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("objectType", result.objectType());
        response.put("objectName", result.objectName());
        response.put("originalDdl", ddl);
        response.put("convertedDdl", finalDdl);
        response.put("sourceDbType", source.name());
        response.put("targetDbType", target.name());
        response.put("changes", result.changes());
        response.put("changeCount", result.changes().size());

        log.info("Procedure converted: {} '{}' ({} → {})",
                result.objectType(), result.objectName(), source, target);

        return ApiResponse.success(response);
    }
}
