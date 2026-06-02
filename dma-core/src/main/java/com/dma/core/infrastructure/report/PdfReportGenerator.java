package com.dma.core.infrastructure.report;

import com.dma.common.enums.ReportFormat;
import com.dma.core.domain.model.report.MigrationReport;
import com.dma.core.domain.model.scanner.ScanResult;
import com.dma.core.domain.service.ReportGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * PDF 报告生成器。
 * 使用 Flying Saucer 将 HTML 渲染为 PDF。
 */
@Component
public class PdfReportGenerator implements ReportGenerator {

    private static final Logger log = LoggerFactory.getLogger(PdfReportGenerator.class);

    @Override
    public byte[] generate(MigrationReport report, List<ScanResult> results) {
        try {
            String html = buildPdfHtml(report, results);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(out);
            renderer.finishPDF();
            log.info("PDF report generated: {} bytes", out.size());
            return out.toByteArray();
        } catch (Exception e) {
            log.error("PDF generation failed", e);
            return ("PDF generation error: " + e.getMessage()).getBytes(StandardCharsets.UTF_8);
        }
    }

    @Override
    public ReportFormat supportedFormat() { return ReportFormat.PDF; }

    @Override
    public String fileExtension() { return ".pdf"; }

    private String buildPdfHtml(MigrationReport report, List<ScanResult> results) {
        StringBuilder h = new StringBuilder();
        h.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><style>");
        h.append("@page { size: A4 landscape; margin: 20px; }");
        h.append("body { font-family: 'SimSun', 'Microsoft YaHei', Arial, sans-serif; font-size: 12px; color: #333; }");
        h.append("h1 { color: #1e40af; font-size: 22px; border-bottom: 3px solid #3b82f6; padding-bottom: 8px; }");
        h.append("h2 { color: #1e40af; font-size: 16px; margin-top: 20px; }");
        h.append("table { width: 100%; border-collapse: collapse; margin: 10px 0; font-size: 11px; }");
        h.append("th { background: #1e40af; color: #fff; padding: 8px 10px; text-align: left; font-weight: bold; }");
        h.append("td { padding: 6px 10px; border-bottom: 1px solid #e2e8f0; }");
        h.append("tr:nth-child(even) td { background: #f8fafc; }");
        h.append(".summary { background: #eff6ff; padding: 12px; border-radius: 6px; margin: 10px 0; }");
        h.append(".error { color: #dc2626; font-weight: bold; }");
        h.append(".warning { color: #d97706; }");
        h.append(".info { color: #6b7280; }");
        h.append(".footer { text-align: center; color: #94a3b8; font-size: 10px; margin-top: 20px; border-top: 1px solid #e2e8f0; padding-top: 10px; }");
        h.append("</style></head><body>");

        h.append("<h1>DMA 数据库迁移报告</h1>");
        h.append("<div class=\"summary\">");
        h.append("<strong>任务:</strong> ").append(esc(report.getTaskName())).append(" | ");
        h.append("<strong>迁移路径:</strong> ").append(report.getSourceDbType()).append(" → ").append(report.getTargetDbType()).append(" | ");
        h.append("<strong>生成时间:</strong> ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        h.append("</div>");

        // Summary stats
        h.append("<table><tr>");
        h.append("<th>总问题</th><th>可自动转换</th><th>需人工审核</th><th>不兼容</th><th>兼容率</th>");
        h.append("</tr><tr>");
        int total = results.size();
        long auto = results.stream().filter(r -> "AUTO_CONVERTIBLE".equals(r.getCompatibilityLevel())).count();
        long manual = results.stream().filter(r -> "MANUAL_REVIEW".equals(r.getCompatibilityLevel())).count();
        long incomp = results.stream().filter(r -> "INCOMPATIBLE".equals(r.getCompatibilityLevel())).count();
        double rate = total > 0 ? Math.round((1.0 - (double)(manual + incomp) / total) * 1000) / 10.0 : 100.0;
        h.append("<td>").append(total).append("</td>");
        h.append("<td>").append(auto).append("</td>");
        h.append("<td class=\"warning\">").append(manual).append("</td>");
        h.append("<td class=\"error\">").append(incomp).append("</td>");
        h.append("<td><strong>").append(String.format("%.1f%%", rate)).append("</strong></td>");
        h.append("</tr></table>");

        // Detail table
        h.append("<h2>详细结果</h2>");
        h.append("<table><tr>");
        h.append("<th>#</th><th>规则</th><th>严重度</th><th>级别</th><th>文件</th><th>原始SQL</th><th>建议SQL</th>");
        h.append("</tr>");
        int maxRows = Math.min(results.size(), 200);
        for (int i = 0; i < maxRows; i++) {
            ScanResult r = results.get(i);
            String sevClass = "ERROR".equals(r.getSeverity()) ? "error"
                    : "WARNING".equals(r.getSeverity()) ? "warning" : "info";
            h.append("<tr>");
            h.append("<td>").append(i + 1).append("</td>");
            h.append("<td>").append(esc(r.getRuleCode())).append("</td>");
            h.append("<td class=\"").append(sevClass).append("\">").append(r.getSeverity()).append("</td>");
            h.append("<td>").append(esc(r.getCompatibilityLevel())).append("</td>");
            h.append("<td>").append(esc(trunc(r.getFilePath(), 40))).append("</td>");
            h.append("<td>").append(esc(trunc(r.getSourceSql(), 80))).append("</td>");
            h.append("<td>").append(esc(trunc(r.getSuggestedSql(), 80))).append("</td>");
            h.append("</tr>");
        }
        if (results.size() > maxRows) {
            h.append("<tr><td colspan=\"7\">... 还有 ").append(results.size() - maxRows).append(" 条结果未显示</td></tr>");
        }
        h.append("</table>");

        h.append("<div class=\"footer\">DMA — Database Migration Assistant v1.0.0 | github.com/Arvin-LK/dma</div>");
        h.append("</body></html>");
        return h.toString();
    }

    private String esc(String s) { return s == null ? "" : s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;"); }
    private String trunc(String s, int max) { return s == null ? "" : s.length() > max ? s.substring(0, max) + "..." : s; }
}
