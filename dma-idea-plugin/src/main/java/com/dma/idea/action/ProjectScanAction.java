package com.dma.idea.action;

import com.dma.common.enums.DatabaseType;
import com.dma.core.domain.model.scanner.ScanResult;
import com.dma.idea.service.DmaProjectService;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 全项目扫描 Action。
 * 遍历整个项目的 Java/XML/SQL 文件，执行兼容性分析并报告结果。
 */
public class ProjectScanAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        DmaProjectService service = DmaProjectService.getInstance(project);
        DatabaseType source = service.getSourceDb();
        DatabaseType target = service.getTargetDb();

        ProgressManager.getInstance().run(new Task.Backgroundable(project,
                "DMA 项目扫描: " + source + " → " + target, true) {

            private int totalFiles = 0;
            private int javaFiles = 0;
            private int xmlFiles = 0;
            private int sqlFiles = 0;
            private int totalIssues = 0;
            private int highRisk = 0;

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(false);

                VirtualFile baseDir = project.getBaseDir();
                List<ScanResult> allResults = new ArrayList<>();

                // Walk all project files
                ApplicationManager.getApplication().runReadAction(() -> {
                    com.intellij.openapi.vfs.VfsUtil.visitChildrenRecursively(baseDir,
                        new VirtualFileVisitor<Void>() {
                            @Override
                            public boolean visitFile(@NotNull VirtualFile file) {
                                if (indicator.isCanceled()) return false;

                                String name = file.getName().toLowerCase();
                                indicator.setText("扫描: " + file.getPath());

                                if (name.endsWith(".java")) {
                                    javaFiles++;
                                    checkFile(file, indicator);
                                } else if (name.endsWith(".xml") && isMapperXml(file)) {
                                    xmlFiles++;
                                    checkXmlFile(file, service, source, target, indicator);
                                } else if (name.endsWith(".sql")) {
                                    sqlFiles++;
                                    checkSqlFile(file, service, source, target, indicator);
                                }
                                totalFiles++;
                                indicator.setFraction((double) totalFiles / 1000);
                                return true;
                            }
                        });
                });

                // Show notification
                ApplicationManager.getApplication().invokeLater(() -> {
                    NotificationGroupManager.getInstance()
                        .getNotificationGroup("DMA Scan Results")
                        .createNotification(
                            String.format("DMA 扫描完成\n" +
                                "文件: %d (Java:%d XML:%d SQL:%d)\n" +
                                "问题: %d (高风险:%d)\n" +
                                "迁移方向: %s → %s",
                                totalFiles, javaFiles, xmlFiles, sqlFiles,
                                totalIssues, highRisk, source, target),
                            NotificationType.INFORMATION)
                        .notify(project);
                });
            }

            private void checkFile(VirtualFile file, ProgressIndicator indicator) {
                try {
                    String content = new String(file.contentsToByteArray());
                    // Extract SQL strings from Java code
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                        "\"(?i)(SELECT|INSERT|UPDATE|DELETE|CREATE|ALTER)\\b[^\"]*\""
                    ).matcher(content);

                    while (m.find()) {
                        String sql = m.group().replaceAll("^\"|\"$", "");
                        try {
                            List<ScanResult> results = service.analyze(sql, source, target);
                            totalIssues += results.size();
                            highRisk += results.stream()
                                .filter(r -> "ERROR".equals(r.getSeverity())).count();
                        } catch (Exception ignored) {}
                    }
                } catch (IOException ignored) {}
            }
        });
    }

    private boolean isMapperXml(VirtualFile file) {
        try {
            String content = new String(file.contentsToByteArray());
            return content.contains("<mapper") || content.contains("<select")
                || content.contains("<insert") || content.contains("<update");
        } catch (IOException e) { return false; }
    }

    private void checkXmlFile(VirtualFile file, DmaProjectService service,
                               DatabaseType source, DatabaseType target,
                               ProgressIndicator indicator) {
        try {
            String content = new String(file.contentsToByteArray());
            // Extract SQL from MyBatis XML tags
            java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                "<(?:select|insert|update|delete)[^>]*>(.*?)</(?:select|insert|update|delete)>",
                java.util.regex.Pattern.DOTALL | java.util.regex.Pattern.CASE_INSENSITIVE
            ).matcher(content);

            while (m.find()) {
                String sql = m.group(1).replaceAll("<[^>]+>", "").trim();
                if (sql.length() > 5) {
                    totalIssues += service.analyze(sql, source, target).size();
                }
            }
        } catch (Exception ignored) {}
    }

    private void checkSqlFile(VirtualFile file, DmaProjectService service,
                               DatabaseType source, DatabaseType target,
                               ProgressIndicator indicator) {
        try {
            String content = new String(file.contentsToByteArray());
            String[] statements = content.split(";");
            for (String stmt : statements) {
                String sql = stmt.trim();
                if (sql.length() > 5) {
                    List<ScanResult> r = service.analyze(sql, source, target);
                    totalIssues += r.size();
                    highRisk += r.stream()
                        .filter(x -> "ERROR".equals(x.getSeverity())).count();
                }
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(e.getProject() != null);
    }
}
