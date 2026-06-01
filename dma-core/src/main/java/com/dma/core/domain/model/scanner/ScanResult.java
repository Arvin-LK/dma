package com.dma.core.domain.model.scanner;
import java.time.LocalDateTime;

/** 扫描结果实体 */
public class ScanResult {
    private ScanResultId id;
    private String ruleCode;
    private String filePath;
    private int lineNumber;
    private int columnNumber;
    private String sourceSql;
    private String suggestedSql;
    private String compatibilityLevel;
    private String severity;
    private String message;
    private ScanSource source;
    private boolean resolved;
    private String resolutionNote;
    private LocalDateTime createdAt;

    public ScanResult(String ruleCode, String sourceSql, String compatibilityLevel, String severity, String message, ScanSource source) {
        this.ruleCode = ruleCode; this.sourceSql = sourceSql; this.compatibilityLevel = compatibilityLevel;
        this.severity = severity; this.message = message; this.source = source; this.createdAt = LocalDateTime.now();
    }

    public void markResolved() { this.resolved = true; }
    public boolean isResolved() { return resolved; }
    public ScanResultId getId() { return id; }
    public void setId(ScanResultId id) { this.id = id; }
    public String getRuleCode() { return ruleCode; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String p) { this.filePath = p; }
    public int getLineNumber() { return lineNumber; }
    public void setLineNumber(int n) { this.lineNumber = n; }
    public int getColumnNumber() { return columnNumber; }
    public void setColumnNumber(int n) { this.columnNumber = n; }
    public String getSourceSql() { return sourceSql; }
    public String getSuggestedSql() { return suggestedSql; }
    public void setSuggestedSql(String s) { this.suggestedSql = s; }
    public String getCompatibilityLevel() { return compatibilityLevel; }
    public void setCompatibilityLevel(String l) { this.compatibilityLevel = l; }
    public String getSeverity() { return severity; }
    public String getMessage() { return message; }
    public ScanSource getSource() { return source; }
    public String getResolutionNote() { return resolutionNote; }
    public void setResolutionNote(String n) { this.resolutionNote = n; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
