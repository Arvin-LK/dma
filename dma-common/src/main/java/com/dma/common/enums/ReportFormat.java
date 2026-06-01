package com.dma.common.enums;

/**
 * 报告导出格式。
 */
public enum ReportFormat {
    HTML("text/html", ".html"),
    PDF("application/pdf", ".pdf"),
    WORD("application/vnd.openxmlformats-officedocument.wordprocessingml.document", ".docx");

    private final String mimeType;
    private final String extension;

    ReportFormat(String mimeType, String extension) {
        this.mimeType = mimeType;
        this.extension = extension;
    }

    public String getMimeType() { return mimeType; }
    public String getExtension() { return extension; }
}
