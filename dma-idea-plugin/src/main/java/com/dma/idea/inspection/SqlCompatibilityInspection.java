package com.dma.idea.inspection;

import com.dma.common.enums.DatabaseType;
import com.dma.core.domain.model.scanner.ScanResult;
import com.dma.core.domain.service.SqlCompatibilityAnalyzer;
import com.dma.core.infrastructure.engine.DefaultSqlCompatibilityAnalyzer;
import com.dma.core.infrastructure.engine.JsonFileRuleLoader;
import com.dma.core.infrastructure.engine.RuleEngine;
import com.intellij.codeInspection.*;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementVisitor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * DMA SQL 兼容性实时检测 Inspection。
 * 检测 Java 字符串和 SQL 文件中的 SQL 兼容性问题。
 */
public class SqlCompatibilityInspection extends LocalInspectionTool {

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new PsiElementVisitor() {
            @Override
            public void visitFile(@NotNull PsiFile file) {
                String name = file.getName().toLowerCase();
                if (name.endsWith(".sql")) {
                    checkSql(holder, file, file.getText());
                }
            }

            @Override
            public void visitElement(@NotNull PsiElement element) {
                String text = element.getText();
                if (text != null && text.length() > 15 && (text.startsWith("\"") || text.startsWith("'"))
                    && hasSql(text.toUpperCase())) {
                    checkSql(holder, element, text.substring(1, text.length() - 1));
                }
            }

            private boolean hasSql(String upper) {
                return upper.contains("SELECT") || upper.contains("INSERT")
                    || upper.contains("UPDATE") || upper.contains("DELETE")
                    || upper.contains("CREATE") || upper.contains("ALTER");
            }

            private void checkSql(ProblemsHolder holder, PsiElement element, String sql) {
                try {
                    // Lazy-init rule engine (once per JVM)
                    RuleEngine engine = RuleEngineHolder.ENGINE;
                    SqlCompatibilityAnalyzer analyzer = new DefaultSqlCompatibilityAnalyzer(engine);

                    List<ScanResult> results = analyzer.analyze(
                        sql, DatabaseType.MYSQL, DatabaseType.POSTGRESQL);

                    for (ScanResult r : results) {
                        ProblemHighlightType type = "ERROR".equals(r.getSeverity())
                            ? ProblemHighlightType.ERROR
                            : ProblemHighlightType.WARNING;

                        holder.registerProblem(element,
                            "[" + r.getRuleCode() + "] " + r.getMessage(),
                            type,
                            new ConvertQuickFix(r.getRuleCode(), r.getSuggestedSql()));
                    }
                } catch (Exception ignored) { /* skip parse errors */ }
            }
        };
    }

    /** 规则引擎单例持有者 */
    private static class RuleEngineHolder {
        static final RuleEngine ENGINE = new RuleEngine(
            List.of(new JsonFileRuleLoader()));
    }
}
