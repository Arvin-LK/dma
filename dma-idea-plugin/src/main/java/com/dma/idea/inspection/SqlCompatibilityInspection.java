package com.dma.idea.inspection;

import com.dma.common.enums.DatabaseType;
import com.dma.core.domain.model.scanner.ScanResult;
import com.dma.core.domain.service.SqlCompatibilityAnalyzer;
import com.dma.core.infrastructure.engine.DefaultSqlCompatibilityAnalyzer;
import com.dma.core.infrastructure.engine.JsonFileRuleLoader;
import com.dma.core.infrastructure.engine.RuleEngine;
import com.intellij.codeInspection.*;
import com.intellij.psi.*;
import java.util.List;

/**
 * DMA SQL 兼容性实时检测 Inspection。
 * 检测 Java 字符串中的 SQL 兼容性问题。
 */
public class SqlCompatibilityInspection extends LocalInspectionTool {

    @Override
    public ProblemDescriptor[] checkFile(PsiFile file,
                                          InspectionManager manager,
                                          boolean isOnTheFly) {
        return ProblemDescriptor.EMPTY_ARRAY;
    }

    @Override
    public PsiElementVisitor buildVisitor(final ProblemsHolder holder,
                                          boolean isOnTheFly) {
        return new PsiElementVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                // Only check Java string literals
                if (!(element instanceof PsiElement)) return;

                String text = element.getText();
                if (text == null || text.length() < 15) return;
                if (!text.startsWith("\"")) return;

                String sql = text.substring(1, text.length() - 1);
                String upper = sql.toUpperCase();
                if (!upper.contains("SELECT") && !upper.contains("INSERT")
                    && !upper.contains("UPDATE") && !upper.contains("DELETE")
                    && !upper.contains("CREATE") && !upper.contains("ALTER")) return;

                try {
                    List<ScanResult> results = EngineHolder.ANALYZER.analyze(
                        sql, DatabaseType.MYSQL, DatabaseType.POSTGRESQL);

                    for (ScanResult r : results) {
                        String msg = "[" + r.getRuleCode() + "] " + r.getMessage();
                        holder.registerProblem(element, msg,
                            ProblemHighlightType.WARNING,
                            new ConvertQuickFix(r.getRuleCode(), r.getSuggestedSql()));
                    }
                } catch (Exception ignored) { /* parse error */ }
            }
        };
    }

    private static class EngineHolder {
        static final RuleEngine ENGINE = new RuleEngine(
            List.of(new JsonFileRuleLoader()));
        static final SqlCompatibilityAnalyzer ANALYZER =
            new DefaultSqlCompatibilityAnalyzer(ENGINE);
    }
}
