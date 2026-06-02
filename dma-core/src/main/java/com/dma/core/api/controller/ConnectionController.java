package com.dma.core.api.controller;
import com.dma.common.dto.ApiResponse;
import com.dma.common.dto.ConnectionConfigDto;
import com.dma.common.dto.PageResult;
import com.dma.core.application.service.ConnectionApplicationService;
import com.dma.core.domain.model.connection.ConnectionId;
import com.dma.core.domain.model.connection.DatabaseConnection;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/connections")
public class ConnectionController {
    private final ConnectionApplicationService service;
    public ConnectionController(ConnectionApplicationService service) { this.service = service; }

    @PostMapping
    public ApiResponse<Long> create(@RequestBody Map<String, Object> body) {
        ConnectionId id = service.create(
            (String) body.get("name"), (String) body.get("dbType"),
            (String) body.get("host"), (int) body.get("port"),
            (String) body.get("username"), (String) body.get("password"),
            (String) body.get("databaseName"));
        return ApiResponse.success(id.value());
    }

    @GetMapping
    public ApiResponse<PageResult<ConnectionConfigDto>> list(@RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="20") int size) {
        List<DatabaseConnection> conns = service.list(page, size);
        List<ConnectionConfigDto> dtos = conns.stream().map(c -> new ConnectionConfigDto(
            c.getId() != null ? c.getId().value() : 0,
            c.getName(), c.getDbType().name(), c.getHost(), c.getPort(), c.getUsername(), c.getDatabaseName())).toList();
        return ApiResponse.success(new PageResult<>(dtos, dtos.size(), page, size));
    }

    @PostMapping("/test")
    public ApiResponse<Boolean> test(@RequestBody Map<String, Object> body) {
        return ApiResponse.success(service.test(
            (String) body.get("dbType"), (String) body.get("host"), (int) body.get("port"),
            (String) body.get("username"), (String) body.get("password"), (String) body.get("databaseName")));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(new ConnectionId(id));
        return ApiResponse.success(null);
    }
}
