package com.dma.common.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据库兼容性扫描汇总结果。
 * 包含按对象类型分类的统计以及整体兼容率。
 */
public class ScanSummary {

    private String sourceDbType;
    private String targetDbType;
    private String databaseName;

    // 对象统计
    private int storedProcedureCount;
    private int functionCount;
    private int tableCount;
    private int viewCount;
    private int totalObjects;

    // 兼容性统计
    private int compatibleCount;
    private int autoConvertibleCount;
    private int manualReviewCount;
    private int incompatibleCount;
    private int totalIssues;

    // 兼容率（百分比，如 92.5 表示 92.5%）
    private double compatibilityRate;

    // 详细结果列表
    private List<DatabaseObjectInfo> objects = new ArrayList<>();

    public ScanSummary() {}

    public ScanSummary(String sourceDbType, String targetDbType, String databaseName) {
        this.sourceDbType = sourceDbType;
        this.targetDbType = targetDbType;
        this.databaseName = databaseName;
    }

    /** 根据兼容性统计计算兼容率 */
    public void calculateRate() {
        this.totalObjects = storedProcedureCount + functionCount + tableCount + viewCount;
        if (totalObjects == 0) {
            this.compatibilityRate = 100.0;
        } else {
            int problematic = manualReviewCount + incompatibleCount;
            this.compatibilityRate = Math.round((1.0 - (double) problematic / totalObjects) * 10000.0) / 100.0;
        }
    }

    // === Getters / Setters ===

    public String getSourceDbType() { return sourceDbType; }
    public void setSourceDbType(String s) { this.sourceDbType = s; }
    public String getTargetDbType() { return targetDbType; }
    public void setTargetDbType(String s) { this.targetDbType = s; }
    public String getDatabaseName() { return databaseName; }
    public void setDatabaseName(String s) { this.databaseName = s; }

    public int getStoredProcedureCount() { return storedProcedureCount; }
    public void setStoredProcedureCount(int n) { this.storedProcedureCount = n; }
    public int getFunctionCount() { return functionCount; }
    public void setFunctionCount(int n) { this.functionCount = n; }
    public int getTableCount() { return tableCount; }
    public void setTableCount(int n) { this.tableCount = n; }
    public int getViewCount() { return viewCount; }
    public void setViewCount(int n) { this.viewCount = n; }
    public int getTotalObjects() { return totalObjects; }
    public void setTotalObjects(int n) { this.totalObjects = n; }

    public int getCompatibleCount() { return compatibleCount; }
    public void setCompatibleCount(int n) { this.compatibleCount = n; }
    public int getAutoConvertibleCount() { return autoConvertibleCount; }
    public void setAutoConvertibleCount(int n) { this.autoConvertibleCount = n; }
    public int getManualReviewCount() { return manualReviewCount; }
    public void setManualReviewCount(int n) { this.manualReviewCount = n; }
    public int getIncompatibleCount() { return incompatibleCount; }
    public void setIncompatibleCount(int n) { this.incompatibleCount = n; }
    public int getTotalIssues() { return totalIssues; }
    public void setTotalIssues(int n) { this.totalIssues = n; }

    public double getCompatibilityRate() { return compatibilityRate; }
    public void setCompatibilityRate(double d) { this.compatibilityRate = d; }

    public List<DatabaseObjectInfo> getObjects() { return objects; }
    public void setObjects(List<DatabaseObjectInfo> list) { this.objects = list; }
    public void addObject(DatabaseObjectInfo obj) { this.objects.add(obj); }
}
