package com.dma.common.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据库对象信息。
 * 代表从源数据库中提取的一个对象（存储过程/函数/表/视图）及其兼容性分析结果。
 */
public class DatabaseObjectInfo {

    private String objectName;
    private String objectType;       // TABLE, VIEW, PROCEDURE, FUNCTION, TRIGGER
    private String ddl;              // 对象的 DDL 语句
    private String compatibilityLevel;
    private String severity;
    private String suggestedDdl;     // 建议的转换后 DDL
    private String ruleCode;         // 如果不兼容，关联的规则代码
    private String description;
    private List<String> issues = new ArrayList<>();

    public DatabaseObjectInfo() {}

    public DatabaseObjectInfo(String objectName, String objectType, String ddl) {
        this.objectName = objectName;
        this.objectType = objectType;
        this.ddl = ddl;
    }

    public String getObjectName() { return objectName; }
    public void setObjectName(String s) { this.objectName = s; }
    public String getObjectType() { return objectType; }
    public void setObjectType(String s) { this.objectType = s; }
    public String getDdl() { return ddl; }
    public void setDdl(String s) { this.ddl = s; }
    public String getCompatibilityLevel() { return compatibilityLevel; }
    public void setCompatibilityLevel(String s) { this.compatibilityLevel = s; }
    public String getSeverity() { return severity; }
    public void setSeverity(String s) { this.severity = s; }
    public String getSuggestedDdl() { return suggestedDdl; }
    public void setSuggestedDdl(String s) { this.suggestedDdl = s; }
    public String getRuleCode() { return ruleCode; }
    public void setRuleCode(String s) { this.ruleCode = s; }
    public String getDescription() { return description; }
    public void setDescription(String s) { this.description = s; }
    public List<String> getIssues() { return issues; }
    public void setIssues(List<String> list) { this.issues = list; }
}
