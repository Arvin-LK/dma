package com.dma.core.infrastructure.report;
import com.dma.common.enums.ReportFormat;
import com.dma.core.domain.model.report.MigrationReport;
import com.dma.core.domain.model.scanner.ScanResult;
import com.dma.core.domain.service.ReportGenerator;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class HtmlReportGenerator implements ReportGenerator {
    @Override
    public byte[] generate(MigrationReport report, List<ScanResult> results) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html lang=\"zh-CN\"><head><meta charset=\"UTF-8\">");
        html.append("<title>DMA 迁移报告</title>");
        html.append("<style>body{font-family:Arial,sans-serif;margin:40px;color:#333}");
        html.append("h1{color:#2563eb}h2{color:#1e40af;border-bottom:2px solid #93c5fd;padding-bottom:8px}");
        html.append("table{border-collapse:collapse;width:100%;margin:12px 0}");
        html.append("th,td{border:1px solid #d1d5db;padding:8px 12px;text-align:left}");
        html.append("th{background:#f3f4f6}.ERROR{color:#dc2626;font-weight:bold}");
        html.append(".WARNING{color:#d97706}.INFO{color:#6b7280}.summary{background:#f0f9ff;padding:16px;border-radius:8px;margin:16px 0}");
        html.append("</style></head><body>");
        html.append("<h1>DMA 数据库迁移报告</h1>");
        html.append("<div class=\"summary\">");
        html.append("<p><strong>任务:</strong> ").append(escape(report.getTaskName())).append("</p>");
        html.append("<p><strong>迁移路径:</strong> ").append(report.getSourceDbType()).append(" → ").append(report.getTargetDbType()).append("</p>");
        html.append("<p><strong>生成时间:</strong> ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("</p>");
        html.append("<p><strong>总问题数:</strong> ").append(results.size()).append("</p>");
        html.append("</div>");
        html.append("<h2>详细结果</h2><table><tr><th>#</th><th>规则</th><th>严重度</th><th>级别</th><th>文件</th><th>行</th><th>原始SQL</th><th>建议SQL</th></tr>");
        for (int i = 0; i < results.size(); i++) {
            ScanResult r = results.get(i);
            html.append("<tr>");
            html.append("<td>").append(i+1).append("</td>");
            html.append("<td>").append(escape(r.getRuleCode())).append("</td>");
            html.append("<td class=\"").append(r.getSeverity()).append("\">").append(r.getSeverity()).append("</td>");
            html.append("<td>").append(r.getCompatibilityLevel()).append("</td>");
            html.append("<td>").append(escape(r.getFilePath() != null ? r.getFilePath() : "-")).append("</td>");
            html.append("<td>").append(r.getLineNumber() > 0 ? String.valueOf(r.getLineNumber()) : "-").append("</td>");
            html.append("<td><code>").append(escape(truncate(r.getSourceSql(), 100))).append("</code></td>");
            html.append("<td><code>").append(escape(truncate(r.getSuggestedSql(), 100))).append("</code></td>");
            html.append("</tr>");
        }
        html.append("</table></body></html>");
        return html.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override public ReportFormat supportedFormat() { return ReportFormat.HTML; }
    @Override public String fileExtension() { return ".html"; }

    private String escape(String s) { return s == null ? "" : s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;"); }
    private String truncate(String s, int max) { return s == null ? "" : s.length() > max ? s.substring(0, max) + "..." : s; }
}
