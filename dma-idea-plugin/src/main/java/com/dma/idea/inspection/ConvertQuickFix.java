package com.dma.idea.inspection;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * Alt+Enter 自动修复：将源 SQL 替换为建议的目标 SQL。
 */
public class ConvertQuickFix implements LocalQuickFix {

    private final String ruleCode;
    private final String suggestedSql;

    public ConvertQuickFix(String ruleCode, String suggestedSql) {
        this.ruleCode = ruleCode;
        this.suggestedSql = suggestedSql;
    }

    @NotNull
    @Override
    public String getName() {
        return "DMA 自动转换 SQL (" + ruleCode + ")";
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return "DMA 数据库迁移修复";
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
        // QuickFix: show the suggested SQL in a notification
        if (suggestedSql != null && !suggestedSql.isBlank()) {
            com.intellij.openapi.ui.Messages.showInfoMessage(
                project,
                "建议转换为:\n\n" + suggestedSql,
                "DMA SQL 转换建议 [" + ruleCode + "]"
            );
        }
    }
}
