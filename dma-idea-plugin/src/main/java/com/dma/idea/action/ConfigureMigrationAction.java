package com.dma.idea.action;

import com.dma.idea.service.DmaProjectService;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBLabel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * 迁移配置对话框 — 设置源/目标数据库类型。
 */
public class ConfigureMigrationAction extends AnAction {

    private static final String[] DB_TYPES = {
        "MYSQL", "ORACLE", "SQLSERVER",
        "POSTGRESQL", "DAMENG", "GAUSSDB", "OCEANBASE", "GOLDENDB"
    };

    private static final String[] DB_LABELS = {
        "MySQL", "Oracle", "SQL Server",
        "PostgreSQL", "达梦 DM", "GaussDB", "OceanBase", "GoldenDB"
    };

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        DmaProjectService service = DmaProjectService.getInstance(project);

        MigrationConfigDialog dialog = new MigrationConfigDialog(
            project, service.getSourceDb().name(), service.getTargetDb().name());
        if (dialog.showAndGet()) {
            service.setMigrationPath(dialog.getSourceDb(), dialog.getTargetDb());
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(e.getProject() != null);
    }

    private static class MigrationConfigDialog extends DialogWrapper {
        private final ComboBox<String> sourceCombo;
        private final ComboBox<String> targetCombo;

        MigrationConfigDialog(Project project, String currentSource, String currentTarget) {
            super(project);
            setTitle("DMA 迁移配置");

            sourceCombo = new ComboBox<>(DB_LABELS);
            sourceCombo.setSelectedIndex(indexOf(currentSource));
            targetCombo = new ComboBox<>(DB_LABELS);
            targetCombo.setSelectedIndex(indexOf(currentTarget));

            init();
        }

        @Override
        protected @Nullable JComponent createCenterPanel() {
            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 10, 5, 10);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            gbc.gridx = 0; gbc.gridy = 0;
            panel.add(new JBLabel("源数据库:"), gbc);
            gbc.gridx = 1;
            panel.add(sourceCombo, gbc);

            gbc.gridx = 0; gbc.gridy = 1;
            panel.add(new JBLabel("目标数据库:"), gbc);
            gbc.gridx = 1;
            panel.add(targetCombo, gbc);

            JLabel hint = new JBLabel("<html><small>修改后重新打开文件或执行项目扫描生效</small></html>");
            gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
            panel.add(hint, gbc);

            return panel;
        }

        String getSourceDb() { return DB_TYPES[sourceCombo.getSelectedIndex()]; }
        String getTargetDb() { return DB_TYPES[targetCombo.getSelectedIndex()]; }

        private int indexOf(String dbType) {
            for (int i = 0; i < DB_TYPES.length; i++) {
                if (DB_TYPES[i].equals(dbType)) return i;
            }
            return 0;
        }
    }
}
