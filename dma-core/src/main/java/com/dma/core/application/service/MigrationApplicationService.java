package com.dma.core.application.service;
import com.dma.common.enums.DatabaseType;
import com.dma.common.enums.ReportFormat;
import com.dma.common.enums.TaskStatus;
import com.dma.core.domain.model.connection.ConnectionId;
import com.dma.core.domain.model.migration.MigrationTask;
import com.dma.core.domain.model.migration.TaskId;
import com.dma.core.domain.model.report.MigrationReport;
import com.dma.core.domain.model.scanner.ScanResult;
import com.dma.core.domain.repository.MigrationTaskRepository;
import com.dma.core.domain.repository.ScanResultRepository;
import com.dma.core.domain.service.ReportGenerator;
import com.dma.core.domain.service.SqlCompatibilityAnalyzer;
import com.dma.core.domain.service.SqlConverter;
import com.dma.core.domain.service.SourceCodeScanner;
import com.dma.core.infrastructure.report.ReportGeneratorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.nio.file.Path;
import java.util.List;

@Service
public class MigrationApplicationService {
    private static final Logger log = LoggerFactory.getLogger(MigrationApplicationService.class);
    private final MigrationTaskRepository taskRepository;
    private final ScanResultRepository scanResultRepository;
    private final SqlCompatibilityAnalyzer analyzer;
    private final SqlConverter converter;
    private final SourceCodeScanner sourceCodeScanner;
    private final ReportGeneratorFactory reportFactory;

    public MigrationApplicationService(MigrationTaskRepository taskRepository, ScanResultRepository scanResultRepository,
                                        SqlCompatibilityAnalyzer analyzer, SqlConverter converter,
                                        SourceCodeScanner sourceCodeScanner, ReportGeneratorFactory reportFactory) {
        this.taskRepository = taskRepository; this.scanResultRepository = scanResultRepository;
        this.analyzer = analyzer; this.converter = converter;
        this.sourceCodeScanner = sourceCodeScanner; this.reportFactory = reportFactory;
    }

    public MigrationTask createTask(String name, ConnectionId sourceId, ConnectionId targetId, DatabaseType sourceType, DatabaseType targetType) {
        MigrationTask task = new MigrationTask(name, sourceId, targetId);
        TaskId taskId = taskRepository.save(task);
        task.setId(taskId);
        log.info("Created migration task: id={}, name={}", taskId.value(), name);
        return task;
    }

public MigrationTask executeScan(TaskId taskId, DatabaseType source, DatabaseType target) {
        MigrationTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found: " + taskId.value()));
        task.start();
        taskRepository.update(task);
        try {
            List<ScanResult> results = analyzer.analyze("SELECT 1", source, target);
            for (ScanResult r : results) {
                if ("AUTO_CONVERTIBLE".equals(r.getCompatibilityLevel())) {
                    converter.convertSingle(r);
                }
                task.addScanResult(r);
                scanResultRepository.save(taskId, r);
            }
            task.calculateRiskScore();
            task.complete();
        } catch (Exception e) {
            log.error("Scan failed for task {}", taskId.value(), e);
            task.fail(e.getMessage());
        }
        taskRepository.update(task);
        return task;
    }

public List<ScanResult> scanSql(String sql, DatabaseType source, DatabaseType target) {
        List<ScanResult> results = analyzer.analyze(sql, source, target);
        for (ScanResult r : results) {
            if ("AUTO_CONVERTIBLE".equals(r.getCompatibilityLevel())) {
                converter.convertSingle(r);
            }
        }
        return results;
    }

public List<ScanResult> scanProject(String projectPath, DatabaseType source, DatabaseType target) {
        return sourceCodeScanner.scanProject(Path.of(projectPath), source, target);
    }

    public byte[] generateReport(TaskId taskId, ReportFormat format) {
        MigrationTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found: " + taskId.value()));
        List<ScanResult> results = scanResultRepository.findByTaskId(taskId);
        MigrationReport report = new MigrationReport(task.getTaskName(),
                sourceTypeFromId(task.getSourceConnectionId()),
                targetTypeFromId(task.getTargetConnectionId()));
        report.setTotalIssues(results.size());
        ReportGenerator generator = reportFactory.getGenerator(format);
        return generator.generate(report, results);
    }

    private String sourceTypeFromId(ConnectionId id) { return "MYSQL"; }
    private String targetTypeFromId(ConnectionId id) { return "POSTGRESQL"; }
}
