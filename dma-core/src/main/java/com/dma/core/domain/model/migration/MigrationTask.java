package com.dma.core.domain.model.migration;

import com.dma.common.enums.TaskStatus;
import com.dma.core.domain.model.connection.ConnectionId;
import com.dma.core.domain.model.scanner.ScanResult;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 迁移任务聚合根 */
public class MigrationTask {
    private TaskId id;
    private String taskName;
    private ConnectionId sourceConnectionId;
    private ConnectionId targetConnectionId;
    private TaskStatus status;
    private int totalIssues;
    private int resolvedIssues;
    private RiskScore riskScore;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private List<ScanResult> scanResults;

    public MigrationTask(String taskName, ConnectionId sourceConnectionId, ConnectionId targetConnectionId) {
        this.taskName = taskName; this.sourceConnectionId = sourceConnectionId;
        this.targetConnectionId = targetConnectionId; this.status = TaskStatus.PENDING;
        this.riskScore = RiskScore.ZERO; this.scanResults = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
    }

    public void start() { this.status = TaskStatus.RUNNING; }
    public void complete() { this.status = TaskStatus.COMPLETED; this.completedAt = LocalDateTime.now(); }
    public void fail(String error) { this.status = TaskStatus.FAILED; this.errorMessage = error; }
    public void addScanResult(ScanResult result) { this.scanResults.add(result); this.totalIssues++; }
    public void resolveIssue(int index) { if (!scanResults.get(index).isResolved()) { scanResults.get(index).markResolved(); resolvedIssues++; } }
    public void calculateRiskScore() {
        long errors = scanResults.stream().filter(r -> "ERROR".equals(r.getSeverity())).count();
        long warnings = scanResults.stream().filter(r -> "WARNING".equals(r.getSeverity())).count();
        int score = (int) Math.min(100, (errors * 20 + warnings * 5));
        this.riskScore = new RiskScore(score);
    }

    public TaskId getId() { return id; }
    public void setId(TaskId id) { this.id = id; }
    public String getTaskName() { return taskName; }
    public ConnectionId getSourceConnectionId() { return sourceConnectionId; }
    public ConnectionId getTargetConnectionId() { return targetConnectionId; }
    public TaskStatus getStatus() { return status; }
    public int getTotalIssues() { return totalIssues; }
    public int getResolvedIssues() { return resolvedIssues; }
    public RiskScore getRiskScore() { return riskScore; }
    public String getErrorMessage() { return errorMessage; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public List<ScanResult> getScanResults() { return scanResults; }
}
