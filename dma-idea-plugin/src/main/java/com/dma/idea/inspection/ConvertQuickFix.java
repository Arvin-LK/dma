package com.dma.idea.inspection;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

/**
 * Alt+Enter 自动修复：将源 SQL 替换为建议的目标 SQL。
 */
public class ConvertQuickFix implements LocalQuickFix, IntentionAction {

    private final String ruleCode;
    private final String suggestedSql;

    public ConvertQuickFix(String ruleCode, String suggestedSql) {
        this.ruleCode = ruleCode;
        this.suggestedSql = suggestedSql;
    }

    @NotNull
    @Override
    public String getName() {
        return "DMA: 自动转换 SQL (" + ruleCode + ")";
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return "DMA 数据库迁移修复";
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
        if (suggestedSql == null || suggestedSql.isBlank()) return;

        PsiElement element = descriptor.getPsiElement();
        WriteCommandAction.runWriteCommandAction(project, () -> {
            try {
                // Replace the SQL string literal with the suggested SQL
                String currentText = element.getText();
                String newText;
                if (currentText.startsWith("\"") && currentText.endsWith("\"")) {
                    newText = "\"" + suggestedSql.replace("\"", "\\\"") + "\"";
                } else {
                    newText = suggestedSql;
                }
                // Create new element from text
                PsiElement newElement = element instanceof com.intellij.psi.PsiLiteralExpression
                    ? ((com.intellij.psi.PsiLiteralExpression) element).replace(
                        element.getManager().getElementFactory().createExpressionFromText(newText, element))
                    : null;
            } catch (Exception e) {
                throw new IncorrectOperationException("SQL 转换失败: " + e.getMessage());
            }
        });
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        return true;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) {
        // IntentionAction interface
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }
}
