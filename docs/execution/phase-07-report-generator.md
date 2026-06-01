# Phase 7: 报告生成

## 目标

实现 HTML / PDF / Word 三种格式的迁移报告生成。

## 前置条件

- [ ] Phase 6 完成（源码扫描器可用）

## 步骤

### 7.1 HtmlReportGenerator
- Thymeleaf 模板渲染
- 报告内容：封面、概览摘要、统计图表（Chart.js）、详细问题列表、建议汇总
- 支持在报告内按严重程度筛选

### 7.2 PdfReportGenerator
- Flying Saucer 将 HTML 模板渲染为 PDF
- 保持与 HTML 报告相同的布局

### 7.3 WordReportGenerator
- Apache POI 生成 .docx
- 表格展示问题列表
- 支持分页、页眉页脚

### 7.4 ReportGeneratorFactory
- 根据 `ReportFormat` 枚举选择对应生成器

## 验证标准

三种格式报告生成无异常，HTML 可在浏览器中正确显示，PDF/Word 文件可正常打开。
