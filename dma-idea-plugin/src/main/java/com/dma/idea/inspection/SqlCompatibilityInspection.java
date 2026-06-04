package com.dma.idea.inspection;

import com.dma.common.enums.DatabaseType;
import com.dma.core.domain.model.scanner.ScanResult;
import com.dma.core.domain.service.SqlCompatibilityAnalyzer;
import com.intellij.codeInspection.*;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * IDEA SQL 兼容性实时检测。
 * 检测 Java/XML/SQL 文件中的 SQL 语句，实时提示兼容性问题。
 */
public class SqlCompatibilityInspection extends LocalInspectionTool {

    private static final Pattern SQL_PATTERN = Pattern.compile(
        "\"(?i)(SELECT|INSERT|UPDATE|DELETE|CREATE|ALTER|DROP|MERGE|REPLACE|TRUNCATE)\\b[^\"]*\"");

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly,
                                          @NotNull LocalInspectionToolSession session) {
        return new SqlElementVisitor(holder);
    }

    private static class SqlElementVisitor extends PsiElementVisitor {
        private final ProblemsHolder holder;

        SqlElementVisitor(ProblemsHolder holder) { this.holder = holder; }

        @Override
        public void visitFile(@NotNull PsiFile file) {
            String fileName = file.getName().toLowerCase();

            if (fileName.endsWith(".sql")) {
                checkSqlContent(holder, file, file.getText());
            } else if (fileName.endsWith(".xml") && file instanceof XmlFile) {
                checkXmlFile(holder, (XmlFile) file);
            }
        }

        @Override
        public void visitElement(@NotNull PsiElement element) {
            // Check Java string literals that contain SQL
            if (element instanceof PsiLiteralExpression lit) {
                String text = lit.getText();
                if (text != null && text.length() > 10 && isSql(text)) {
                    checkSqlContent(holder, element, text.substring(1, text.length() - 1));
                }
            }
        }

        private boolean isSql(String text) {
            String upper = text.toUpperCase();
            return upper.contains("SELECT") || upper.contains("INSERT") || upper.contains("UPDATE")
                || upper.contains("DELETE") || upper.contains("CREATE") || upper.contains("ALTER")
                || upper.contains("DROP");
        }

        private void checkXmlFile(ProblemsHolder holder, XmlFile xmlFile) {
            xmlFile.accept(new XmlRecursiveElementVisitor() {
                @Override
                public void visitXmlTag(@NotNull XmlTag tag) {
                    String tagName = tag.getName().toLowerCase();
                    if (tagName.equals("select") || tagName.equals("insert")
                        || tagName.equals("update") || tagName.equals("delete")) {
                        String sql = tag.getValue().getText();
                        if (sql != null && !sql.isBlank()) {
                            checkSqlContent(holder, tag, sql);
                        }
                    }
                    super.visitXmlTag(tag);
                }
            });
        }

        private void checkSqlContent(ProblemsHolder holder, PsiElement element, String sql) {
            Project project = element.getProject();
            SqlCompatibilityAnalyzer analyzer = project.getService(SqlCompatibilityAnalyzer.class);

            if (analyzer == null) return;

            try {
                // Default: MySQL → PostgreSQL
                List<ScanResult> results = analyzer.analyze(sql, DatabaseType.MYSQL, DatabaseType.POSTGRESQL);
                for (ScanResult r : results) {
                    String severity = r.getSeverity();
                    ProblemHighlightType highlightType = switch (severity) {
                        case "ERROR" -> ProblemHighlightType.ERROR;
                        case "WARNING" -> ProblemHighlightType.WARNING;
                        default -> ProblemHighlightType.INFORMATION;
                    };

                    String msg = "[" + r.getRuleCode() + "] " + r.getMessage();
                    if (r.getSuggestedSql() != null && !r.getSuggestedSql().isBlank()) {
                        msg += "  → 建议: " + r.getSuggestedSql();
                    }

                    holder.registerProblem(element, msg, highlightType,
                        new ConvertQuickFix(r.getRuleCode(), r.getSuggestedSql()));
                }
            } catch (Exception ignored) {
                // Silently skip analysis errors
            }
        }
    }
}
