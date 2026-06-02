package com.dma.core.api.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 报告导出 API。
 * 将扫描/转换结果导出为 HTML/PDF/Word 格式。
 */
@RestController
@RequestMapping("/api/v1/report")
public class ReportController {

    private static final Logger log = LoggerFactory.getLogger(ReportController.class);

    /**
     * 导出 HTML 报告。
     */
    @PostMapping("/export")
    public ResponseEntity<byte[]> exportReport(@RequestBody Map<String, Object> body) {
        String title = (String) body.getOrDefault("title", "DMA 报告");
        String subtitle = (String) body.getOrDefault("subtitle", "");
        String content = (String) body.getOrDefault("content", "");
        String format = (String) body.getOrDefault("format", "HTML");

        log.info("Exporting report: title='{}', format={}", title, format);

        byte[] reportBytes;
        String filename;
        String mimeType;

        if ("HTML".equalsIgnoreCase(format)) {
            reportBytes = buildHtmlReport(title, subtitle, content);
            filename = "DMA_Report_" + timestamp() + ".html";
            mimeType = "text/html; charset=UTF-8";
        } else {
            // PDF and Word are not yet implemented, fallback to HTML
            reportBytes = buildHtmlReport(title, subtitle, content);
            filename = "DMA_Report_" + timestamp() + ".html";
            mimeType = "text/html; charset=UTF-8";
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(mimeType))
                .body(reportBytes);
    }

    private byte[] buildHtmlReport(String title, String subtitle, String content) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html lang=\"zh-CN\">\n<head>\n");
        html.append("<meta charset=\"UTF-8\">\n");
        html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("<title>").append(escape(title)).append("</title>\n");
        html.append("<style>\n");
        html.append("  * { margin:0; padding:0; box-sizing:border-box; }\n");
        html.append("  body { font-family: 'Segoe UI', 'Microsoft YaHei', Arial, sans-serif; ");
        html.append("         background: #f8fafc; color: #1e293b; line-height:1.6; }\n");
        html.append("  .report { max-width:960px; margin:40px auto; background:#fff; ");
        html.append("            border-radius:12px; box-shadow:0 4px 24px rgba(0,0,0,.08); overflow:hidden; }\n");
        html.append("  .header { background: linear-gradient(135deg, #2563eb, #7c3aed); ");
        html.append("            color:#fff; padding:32px 40px; }\n");
        html.append("  .header h1 { font-size:24px; font-weight:700; margin-bottom:8px; }\n");
        html.append("  .header p { opacity:.85; font-size:14px; }\n");
        html.append("  .meta { padding:16px 40px; background:#f1f5f9; font-size:13px; color:#64748b; ");
        html.append("          display:flex; justify-content:space-between; }\n");
        html.append("  .body { padding:32px 40px; }\n");
        html.append("  .body pre { background:#1e293b; color:#e2e8f0; padding:20px; border-radius:8px; ");
        html.append("              overflow-x:auto; font-family:'Consolas','Courier New',monospace; ");
        html.append("              font-size:13px; line-height:1.5; white-space:pre-wrap; }\n");
        html.append("  .body table { width:100%; border-collapse:collapse; margin:16px 0; }\n");
        html.append("  .body th { background:#f1f5f9; padding:10px 14px; text-align:left; ");
        html.append("             font-weight:600; border-bottom:2px solid #e2e8f0; }\n");
        html.append("  .body td { padding:10px 14px; border-bottom:1px solid #f1f5f9; }\n");
        html.append("  .badge { display:inline-block; padding:2px 10px; border-radius:12px; ");
        html.append("           font-size:12px; font-weight:600; }\n");
        html.append("  .badge-error { background:#fef2f2; color:#dc2626; }\n");
        html.append("  .badge-warn { background:#fffbeb; color:#d97706; }\n");
        html.append("  .badge-info { background:#eff6ff; color:#2563eb; }\n");
        html.append("  .footer { padding:20px 40px; background:#f8fafc; border-top:1px solid #e2e8f0; ");
        html.append("            font-size:12px; color:#94a3b8; text-align:center; }\n");
        html.append("</style>\n</head>\n<body>\n");
        html.append("<div class=\"report\">\n");
        html.append("<div class=\"header\">\n");
        html.append("  <h1>").append(escape(title)).append("</h1>\n");
        if (subtitle != null && !subtitle.isBlank()) {
            html.append("  <p>").append(escape(subtitle)).append("</p>\n");
        }
        html.append("</div>\n");
        html.append("<div class=\"meta\">\n");
        html.append("  <span>Database Migration Assistant (DMA)</span>\n");
        html.append("  <span>生成时间: ").append(LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("</span>\n");
        html.append("</div>\n");
        html.append("<div class=\"body\">\n");

        // 转换内容：检测是否包含表格或代码块
        String formatted = content
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");

        // 将 ─ 开头的行识别为分隔线
        formatted = formatted.replaceAll("(?m)^([─═━]+)$", "<hr style='border:none;border-top:2px solid #e2e8f0;margin:16px 0'>");

        // 将 ● ⚠ ✗ ✓ 开头的行包裹为带图标的段落
        formatted = formatted.replaceAll("(?m)^(✗.*)$", "<p style='color:#dc2626;font-weight:600'>$1</p>");
        formatted = formatted.replaceAll("(?m)^(⚠.*)$", "<p style='color:#d97706;font-weight:600'>$1</p>");
        formatted = formatted.replaceAll("(?m)^(✓.*)$", "<p style='color:#16a34a;font-weight:600'>$1</p>");
        formatted = formatted.replaceAll("(?m)^(ℹ.*)$", "<p style='color:#2563eb;'>$1</p>");

        // 将多空格对齐的行包裹为 pre
        if (formatted.contains("  ") && formatted.length() > 200) {
            formatted = "<pre>" + formatted + "</pre>";
        } else {
            formatted = formatted.replace("\n", "<br>\n");
        }

        html.append(formatted);
        html.append("</div>\n");
        html.append("<div class=\"footer\">\n");
        html.append("  <p>DMA — Database Migration Assistant v1.0.0</p>\n");
        html.append("  <p>本报告由 DMA 自动生成 | github.com/Arvin-LK/dma</p>\n");
        html.append("</div>\n</div>\n</body>\n</html>");

        return html.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }

    private String timestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    }
}
