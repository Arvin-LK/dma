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
 * 支持 HTML / PDF / Word 三种格式。
 */
@RestController
@RequestMapping("/api/v1/report")
public class ReportController {

    private static final Logger log = LoggerFactory.getLogger(ReportController.class);

    /**
     * 导出报告（支持 HTML/PDF/Word）。
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

        if ("PDF".equalsIgnoreCase(format)) {
            reportBytes = generatePdf(title, subtitle, content);
            filename = "DMA_Report_" + timestamp() + ".pdf";
            mimeType = "application/pdf";
        } else if ("WORD".equalsIgnoreCase(format)) {
            // Generate Word document from content
            reportBytes = buildWordReport(title, subtitle, content);
            filename = "DMA_Report_" + timestamp() + ".docx";
            mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        } else {
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
        html.append("<h1 style='font-size:18px;color:#1f2937;margin:0 0 4px 0;padding:0;font-weight:600'>").append(escape(title)).append("</h1>\n");
        if (subtitle != null && !subtitle.isBlank()) {
            html.append("<p style='font-size:13px;color:#6b7280;margin:0 0 16px 0;padding:0'>").append(escape(subtitle)).append("</p>\n");
        }
        html.append("<div class=\"meta\">\n");
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
        html.append("<div class=\"footer\">本报告由 DMA 自动生成</div>\n");
        html.append("</div>\n</body>\n</html>");

        return html.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 生成 PDF。使用 FlyingSaucer + 中文字体支持。
     * 如果 PDF 生成失败，返回错误页面 PDF 而非损坏文件。
     */
    private byte[] generatePdf(String title, String subtitle, String content) {
        try {
            String xhtml = buildXhtmlReport(title, subtitle, content);
            var out = new java.io.ByteArrayOutputStream();
            var renderer = new org.xhtmlrenderer.pdf.ITextRenderer();

            // 添加中文字体（Windows 系统字体）
            try {
                renderer.getFontResolver().addFont(
                    "C:/Windows/Fonts/msyh.ttc", true);      // 微软雅黑
                renderer.getFontResolver().addFont(
                    "C:/Windows/Fonts/simsun.ttc", true);     // 宋体（后备）
                renderer.getFontResolver().addFont(
                    "C:/Windows/Fonts/consola.ttf", true);    // Consolas 等宽
            } catch (Exception fe) {
                log.debug("Font loading skipped: {}", fe.getMessage());
            }

            renderer.setDocumentFromString(xhtml);
            renderer.layout();
            renderer.createPDF(out);
            renderer.finishPDF();
            byte[] pdf = out.toByteArray();
            if (pdf.length < 100) {
                throw new RuntimeException("PDF output too small, likely rendering failed");
            }
            return pdf;
        } catch (Exception e) {
            log.error("PDF generation failed: {}", e.getMessage());
            // 返回显示错误信息的有效 PDF 而非损坏文件
            return buildErrorPdf(e.getMessage());
        }
    }

    /**
     * 生成 XHTML 报告（FlyingSaucer 需要严格 XHTML）。
     */
    private String buildXhtmlReport(String title, String subtitle, String content) {
        StringBuilder html = new StringBuilder();
        html.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        html.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" ");
        html.append("\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n");
        html.append("<html xmlns=\"http://www.w3.org/1999/xhtml\">\n<head>\n");
        html.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>\n");
        html.append("<title>").append(xmlEscape(title)).append("</title>\n");
        html.append("<style type=\"text/css\">\n");
        html.append("  * { margin:0; padding:0; }\n");
        html.append("  body { font-family: 'Microsoft YaHei', 'SimSun', sans-serif; ");
        html.append("         font-size: 12pt; color: #1e293b; line-height: 1.6; padding: 30px 40px; }\n");
        html.append("  h1 { font-size: 20pt; color: #2563eb; margin-bottom: 5px; }\n");
        html.append("  h2 { font-size: 14pt; color: #64748b; margin-bottom: 20px; font-weight: normal; }\n");
        html.append("  .meta { color: #94a3b8; font-size: 9pt; margin-bottom: 20px; ");
        html.append("          border-bottom: 1px solid #e2e8f0; padding-bottom: 12px; }\n");
        html.append("  pre { background: #f8fafc; border: 1px solid #e2e8f0; padding: 16px;");
        html.append("        border-radius: 6px; font-family: 'Consolas', monospace; ");
        html.append("        font-size: 10pt; white-space: pre-wrap; }\n");
        html.append("  .error { color: #dc2626; font-weight: bold; }\n");
        html.append("  .warn { color: #d97706; font-weight: bold; }\n");
        html.append("  .ok { color: #16a34a; font-weight: bold; }\n");
        html.append("  hr { border: none; border-top: 2px solid #e2e8f0; margin: 12px 0; }\n");
        html.append("  .footer { margin-top: 30px; padding-top: 12px; ");
        html.append("            border-top: 1px solid #e2e8f0; font-size: 8pt; color: #cbd5e1; }\n");
        html.append("</style>\n</head>\n<body>\n");

        html.append("<h1>").append(xmlEscape(title)).append("</h1>\n");
        if (subtitle != null && !subtitle.isBlank()) {
            html.append("<h2>").append(xmlEscape(subtitle)).append("</h2>\n");
        }
        html.append("<div class=\"meta\">Database Migration Assistant (DMA) | ")
            .append(java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
            .append("</div>\n");

        // Escape and format content for XHTML
        String escaped = xmlEscape(content);
        // Convert line prefixes to styled spans
        escaped = escaped.replaceAll("(?m)^(✗[^\\n]*)", "<span class=\"error\">$1</span>");
        escaped = escaped.replaceAll("(?m)^(⚠[^\\n]*)", "<span class=\"warn\">$1</span>");
        escaped = escaped.replaceAll("(?m)^(✓[^\\n]*)", "<span class=\"ok\">$1</span>");
        // Wrap in pre, convert newlines to <br/>
        html.append("<pre>");
        html.append(escaped.replace("\n", "<br/>\n"));
        html.append("</pre>\n");

        html.append("<div class=\"footer\">本报告由 DMA 自动生成</div>\n");
        html.append("</body>\n</html>");
        return html.toString();
    }

    /**
     * 生成一个包含错误信息的有效 PDF（而非返回损坏文件）。
     */
    private byte[] buildErrorPdf(String errorMsg) {
        try {
            String xhtml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" "
                + "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">"
                + "<html xmlns=\"http://www.w3.org/1999/xhtml\"><head>"
                + "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>"
                + "<style>body{font-family:'Microsoft YaHei',sans-serif;padding:40px;text-align:center;}"
                + "h1{color:#dc2626;}p{color:#64748b;}</style></head><body>"
                + "<h1>PDF 生成失败</h1>"
                + "<p>报告内容包含 PDF 渲染器不支持的特殊字符或格式。</p>"
                + "<p>请尝试导出为 HTML 或 Word 格式。</p>"
                + "<hr/>"
                + "<p style=\"font-size:10px;color:#94a3b8;\">错误: " + xmlEscape(errorMsg) + "</p>"
                + "</body></html>";
            var out = new java.io.ByteArrayOutputStream();
            var renderer = new org.xhtmlrenderer.pdf.ITextRenderer();
            try {
                renderer.getFontResolver().addFont("C:/Windows/Fonts/msyh.ttc", true);
            } catch (Exception ignored) {}
            renderer.setDocumentFromString(xhtml);
            renderer.layout();
            renderer.createPDF(out);
            renderer.finishPDF();
            return out.toByteArray();
        } catch (Exception ex) {
            return ("PDF Error: " + errorMsg).getBytes(StandardCharsets.UTF_8);
        }
    }

    /** XML 转义（比 HTML 转义更严格） */
    private String xmlEscape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }

    private String timestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    }

    private byte[] buildWordReport(String title, String subtitle, String content) {
        try {
            var doc = new org.apache.poi.xwpf.usermodel.XWPFDocument();
            var out = new java.io.ByteArrayOutputStream();

            // Title
            var titleP = doc.createParagraph();
            titleP.setAlignment(org.apache.poi.xwpf.usermodel.ParagraphAlignment.CENTER);
            var titleRun = titleP.createRun();
            titleRun.setText(title);
            titleRun.setBold(true);
            titleRun.setFontSize(18);
            titleRun.setFontFamily("Microsoft YaHei");

            // Subtitle
            if (subtitle != null && !subtitle.isBlank()) {
                var subP = doc.createParagraph();
                subP.setAlignment(org.apache.poi.xwpf.usermodel.ParagraphAlignment.CENTER);
                var subRun = subP.createRun();
                subRun.setText(subtitle);
                subRun.setFontSize(12);
                subRun.setColor("64748b");
            }

            doc.createParagraph(); // blank line

            // Content as monospaced text
            String[] lines = content.split("\n");
            for (String line : lines) {
                var p = doc.createParagraph();
                var run = p.createRun();
                run.setText(line);
                run.setFontSize(11);
                run.setFontFamily("Consolas");
                if (line.contains("✗") || line.contains("ERROR")) run.setColor("dc2626");
                else if (line.contains("⚠") || line.contains("WARNING")) run.setColor("d97706");
                else if (line.contains("✓") || line.contains("COMPATIBLE")) run.setColor("16a34a");
            }

            // Footer
            doc.createParagraph();
            var footerP = doc.createParagraph();
            footerP.setAlignment(org.apache.poi.xwpf.usermodel.ParagraphAlignment.CENTER);
            var footerRun = footerP.createRun();
            footerRun.setText("DMA — Database Migration Assistant v1.0.0");
            footerRun.setFontSize(9);
            footerRun.setColor("94a3b8");

            doc.write(out);
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Word generation failed", e);
            return ("Word generation error: " + e.getMessage()).getBytes(StandardCharsets.UTF_8);
        }
    }
}
