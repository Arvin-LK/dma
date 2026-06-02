package com.dma.core.infrastructure.report;

import com.dma.common.enums.ReportFormat;
import com.dma.core.domain.model.report.MigrationReport;
import com.dma.core.domain.model.scanner.ScanResult;
import com.dma.core.domain.service.ReportGenerator;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Word (.docx) 报告生成器。
 * 使用 Apache POI XWPF 生成格式化的 Word 文档。
 */
@Component
public class WordReportGenerator implements ReportGenerator {

    private static final Logger log = LoggerFactory.getLogger(WordReportGenerator.class);

    @Override
    public byte[] generate(MigrationReport report, List<ScanResult> results) {
        try (XWPFDocument doc = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Title
            XWPFParagraph title = doc.createParagraph();
            title.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = title.createRun();
            titleRun.setText("DMA 数据库迁移报告");
            titleRun.setBold(true);
            titleRun.setFontSize(22);
            titleRun.setColor("1e40af");
            titleRun.setFontFamily("Microsoft YaHei");

            // Blank line
            doc.createParagraph();

            // Meta info
            addParagraph(doc, "任务: " + report.getTaskName(), 12, false);
            addParagraph(doc, "迁移路径: " + report.getSourceDbType() + " → " + report.getTargetDbType(), 12, false);
            addParagraph(doc, "生成时间: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), 12, false);

            // Separator
            XWPFParagraph sep = doc.createParagraph();
            XWPFRun sepRun = sep.createRun();
            sepRun.setText("━".repeat(60));
            sepRun.setColor("94a3b8");

            // Summary table
            int total = results.size();
            long auto = results.stream().filter(r -> "AUTO_CONVERTIBLE".equals(r.getCompatibilityLevel())).count();
            long manual = results.stream().filter(r -> "MANUAL_REVIEW".equals(r.getCompatibilityLevel())).count();
            long incomp = results.stream().filter(r -> "INCOMPATIBLE".equals(r.getCompatibilityLevel())).count();
            double rate = total > 0 ? Math.round((1.0 - (double)(manual + incomp) / total) * 1000) / 10.0 : 100.0;

            XWPFTable summaryTable = doc.createTable(2, 5);
            summaryTable.setWidth("100%");
            setTableStyle(summaryTable);
            String[] headers = {"总问题数", "可自动转换", "需人工审核", "不兼容", "兼容率"};
            String[] values = {String.valueOf(total), String.valueOf(auto),
                    String.valueOf(manual), String.valueOf(incomp), String.format("%.1f%%", rate)};
            for (int i = 0; i < 5; i++) {
                setCell(summaryTable.getRow(0).getCell(i), headers[i], true, "1e40af", "ffffff");
                setCell(summaryTable.getRow(1).getCell(i), values[i], false, null, null);
                if (i == 2) summaryTable.getRow(1).getCell(i).getParagraphs().get(0).getRuns()
                        .forEach(r -> r.setColor("d97706"));
                if (i == 3) summaryTable.getRow(1).getCell(i).getParagraphs().get(0).getRuns()
                        .forEach(r -> r.setColor("dc2626"));
            }

            doc.createParagraph();

            // Detail table
            XWPFTable detailTable = doc.createTable();
            detailTable.setWidth("100%");
            setTableStyle(detailTable);

            // Header row
            XWPFTableRow headerRow = detailTable.getRow(0);
            String[] cols = {"#", "规则代码", "严重度", "级别", "文件路径", "原始SQL", "建议SQL"};
            for (int i = 0; i < cols.length; i++) {
                // Add cells if needed
                if (i >= headerRow.getTableCells().size()) headerRow.addNewTableCell();
                setCell(headerRow.getCell(i), cols[i], true, "1e40af", "ffffff");
            }

            // Data rows
            int maxRows = Math.min(results.size(), 100);
            for (int i = 0; i < maxRows; i++) {
                ScanResult r = results.get(i);
                XWPFTableRow row = detailTable.createRow();
                String[] data = {
                        String.valueOf(i + 1),
                        r.getRuleCode(),
                        r.getSeverity(),
                        r.getCompatibilityLevel(),
                        trunc(r.getFilePath(), 30),
                        trunc(r.getSourceSql(), 60),
                        trunc(r.getSuggestedSql(), 60)
                };
                for (int j = 0; j < data.length; j++) {
                    if (j >= row.getTableCells().size()) row.addNewTableCell();
                    setCell(row.getCell(j), data[j], false, null, null);
                    if ("ERROR".equals(r.getSeverity())) {
                        row.getCell(j).getParagraphs().get(0).getRuns()
                                .forEach(run -> run.setColor("dc2626"));
                    }
                }
            }

            // Footer
            doc.createParagraph();
            XWPFParagraph footer = doc.createParagraph();
            footer.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun footerRun = footer.createRun();
            footerRun.setText("DMA — Database Migration Assistant v1.0.0 | github.com/Arvin-LK/dma");
            footerRun.setFontSize(9);
            footerRun.setColor("94a3b8");

            doc.write(out);
            log.info("Word report generated: {} bytes", out.size());
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Word generation failed", e);
            return ("Word generation error: " + e.getMessage()).getBytes(StandardCharsets.UTF_8);
        }
    }

    @Override
    public ReportFormat supportedFormat() { return ReportFormat.WORD; }

    @Override
    public String fileExtension() { return ".docx"; }

    private void addParagraph(XWPFDocument doc, String text, int fontSize, boolean bold) {
        XWPFParagraph p = doc.createParagraph();
        XWPFRun run = p.createRun();
        run.setText(text);
        run.setFontSize(fontSize);
        run.setBold(bold);
        run.setFontFamily("Microsoft YaHei");
    }

    private void setCell(XWPFTableCell cell, String text, boolean isHeader, String bgColor, String fontColor) {
        cell.removeParagraph(0);
        XWPFParagraph p = cell.addParagraph();
        p.setAlignment(ParagraphAlignment.LEFT);
        XWPFRun run = p.createRun();
        run.setText(text != null ? text : "");
        run.setFontSize(isHeader ? 11 : 10);
        run.setBold(isHeader);
        run.setFontFamily("Microsoft YaHei");
        if (bgColor != null) cell.setColor(bgColor);
        if (fontColor != null) run.setColor(fontColor);
    }

    private void setTableStyle(XWPFTable table) {
        CTTbl ct = table.getCTTbl();
        CTTblPr pr = ct.getTblPr() != null ? ct.getTblPr() : ct.addNewTblPr();
        CTTblBorders borders = pr.addNewTblBorders();
        borders.addNewTop().setVal(STBorder.SINGLE); borders.getTop().setSz(BigInteger.valueOf(4));
        borders.addNewBottom().setVal(STBorder.SINGLE); borders.getBottom().setSz(BigInteger.valueOf(4));
        borders.addNewLeft().setVal(STBorder.SINGLE); borders.getLeft().setSz(BigInteger.valueOf(4));
        borders.addNewRight().setVal(STBorder.SINGLE); borders.getRight().setSz(BigInteger.valueOf(4));
        borders.addNewInsideH().setVal(STBorder.SINGLE); borders.getInsideH().setSz(BigInteger.valueOf(4));
        borders.addNewInsideV().setVal(STBorder.SINGLE); borders.getInsideV().setSz(BigInteger.valueOf(4));
    }

    private String trunc(String s, int max) { return s == null ? "" : s.length() > max ? s.substring(0, max) + "..." : s; }
}
