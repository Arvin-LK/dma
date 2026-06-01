package com.dma.core.domain.model.report;
import java.util.ArrayList;
import java.util.List;

/** 报告章节值对象 */
public class ReportSection {
    private final String title;
    private final List<String> items;

    public ReportSection(String title) { this.title = title; this.items = new ArrayList<>(); }
    public void addItem(String item) { items.add(item); }
    public String getTitle() { return title; }
    public List<String> getItems() { return items; }
}
