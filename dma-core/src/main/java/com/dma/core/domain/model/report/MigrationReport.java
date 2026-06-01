package com.dma.core.domain.model.report;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 迁移报告实体 */
public class MigrationReport {
    private String taskName;
    private String sourceDbType;
    private String targetDbType;
    private int totalIssues;
    private int compatibleCount;
    private int autoConvertibleCount;
    private int manualReviewCount;
    private int incompatibleCount;
    private int riskScore;
    private LocalDateTime generatedAt;
    private List<ReportSection> sections;

    public MigrationReport(String taskName, String sourceDbType, String targetDbType) {
        this.taskName = taskName; this.sourceDbType = sourceDbType; this.targetDbType = targetDbType;
        this.sections = new ArrayList<>(); this.generatedAt = LocalDateTime.now();
    }

    public void addSection(ReportSection s) { sections.add(s); }
    public String getTaskName() { return taskName; }
    public String getSourceDbType() { return sourceDbType; }
    public String getTargetDbType() { return targetDbType; }
    public int getTotalIssues() { return totalIssues; }
    public void setTotalIssues(int n) { this.totalIssues = n; }
    public int getCompatibleCount() { return compatibleCount; }
    public void setCompatibleCount(int n) { this.compatibleCount = n; }
    public int getAutoConvertibleCount() { return autoConvertibleCount; }
    public void setAutoConvertibleCount(int n) { this.autoConvertibleCount = n; }
    public int getManualReviewCount() { return manualReviewCount; }
    public void setManualReviewCount(int n) { this.manualReviewCount = n; }
    public int getIncompatibleCount() { return incompatibleCount; }
    public void setIncompatibleCount(int n) { this.incompatibleCount = n; }
    public int getRiskScore() { return riskScore; }
    public void setRiskScore(int s) { this.riskScore = s; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public List<ReportSection> getSections() { return sections; }
}
