package com.dma.desktop;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DMA 桌面端应用。
 * 包含两个核心页面：
 *   1. 数据库体检 — 连接源库，全面扫描兼容性
 *   2. SQL 转换 — 手动输入 SQL，查看转换建议
 */
public class DmaDesktopApplication extends Application {

    private ConfigurableApplicationContext springContext;
    private HttpClient httpClient;

    // === 数据库体检页组件 ===
    private TextField dbHostField, dbPortField, dbUserField, dbPasswordField, dbNameField;
    private ComboBox<String> dbSourceCombo, dbTargetCombo;
    private TextArea dbResultArea;
    private Label dbStatusLabel;
    private ProgressIndicator dbProgress;

    // === SQL 转换页组件 ===
    private TextArea sqlInput;
    private TextArea sqlResult;
    private ComboBox<String> sqlSourceCombo, sqlTargetCombo;
    private Label sqlStatusLabel;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void init() {
        springContext = SpringApplication.run(com.dma.core.DmaCoreApplication.class);
        httpClient = HttpClient.newHttpClient();
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("Database Migration Assistant (DMA) — v1.0");
        stage.setWidth(1280);
        stage.setHeight(860);

        TabPane tabPane = new TabPane();

        // Tab 1: 数据库体检
        Tab dbScanTab = new Tab("数据库体检");
        dbScanTab.setClosable(false);
        dbScanTab.setContent(buildDatabaseScanPage());
        // Tab 2: SQL 转换
        Tab sqlTab = new Tab("SQL 转换");
        sqlTab.setClosable(false);
        sqlTab.setContent(buildSqlConvertPage());

        tabPane.getTabs().addAll(dbScanTab, sqlTab);
        tabPane.getSelectionModel().select(0);

        Scene scene = new Scene(tabPane);
        stage.setScene(scene);
        stage.show();
    }

    // ==================== 数据库体检页面 ====================

    private VBox buildDatabaseScanPage() {
        VBox root = new VBox(12);
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color: #f8fafc;");

        // 标题
        Label title = new Label("数据库兼容性体检");
        title.setFont(Font.font("System", FontWeight.BOLD, 22));
        title.setTextFill(Color.valueOf("#1e40af"));

        Label subtitle = new Label("连接源数据库，自动扫描所有对象的兼容性");
        subtitle.setFont(Font.font("System", 14));
        subtitle.setTextFill(Color.valueOf("#64748b"));

        // 连接配置区
        HBox connRow = new HBox(8);
        connRow.setPadding(new Insets(12));
        connRow.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-radius: 8; -fx-background-radius: 8;");
        connRow.setAlignment(Pos.CENTER_LEFT);

        dbHostField = new TextField("localhost"); dbHostField.setPrefWidth(120); dbHostField.setPromptText("主机");
        dbPortField = new TextField("3306"); dbPortField.setPrefWidth(70); dbPortField.setPromptText("端口");
        dbUserField = new TextField("root"); dbUserField.setPrefWidth(100); dbUserField.setPromptText("用户名");
        dbPasswordField = new PasswordField(); dbPasswordField.setPrefWidth(100); dbPasswordField.setPromptText("密码"); dbPasswordField.setText("");
        dbNameField = new TextField("test"); dbNameField.setPrefWidth(120); dbNameField.setPromptText("数据库名");

        connRow.getChildren().addAll(
                new Label("主机:"), dbHostField,
                new Label("端口:"), dbPortField,
                new Label("用户:"), dbUserField,
                new Label("密码:"), dbPasswordField,
                new Label("库名:"), dbNameField
        );

        // 数据库类型选择 + 扫描按钮
        HBox actionRow = new HBox(16);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        dbSourceCombo = new ComboBox<>(FXCollections.observableArrayList(
                "MYSQL", "ORACLE", "SQLSERVER"));
        dbSourceCombo.setValue("MYSQL");
        dbSourceCombo.setPrefWidth(140);

        dbTargetCombo = new ComboBox<>(FXCollections.observableArrayList(
                "POSTGRESQL", "DAMENG", "GAUSSDB", "OCEANBASE", "GOLDENDB"));
        dbTargetCombo.setValue("POSTGRESQL");
        dbTargetCombo.setPrefWidth(140);

        Button scanBtn = new Button("开始扫描");
        scanBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 10 30;");
        scanBtn.setOnAction(e -> runDatabaseScan());

        dbProgress = new ProgressIndicator(-1);
        dbProgress.setVisible(false);
        dbProgress.setPrefSize(30, 30);

        dbStatusLabel = new Label("就绪");
        dbStatusLabel.setTextFill(Color.valueOf("#64748b"));

        actionRow.getChildren().addAll(
                new Label("源数据库:"), dbSourceCombo,
                new Label("→ 目标数据库:"), dbTargetCombo,
                scanBtn, dbProgress, dbStatusLabel
        );

        // 结果展示区
        dbResultArea = new TextArea();
        dbResultArea.setEditable(false);
        dbResultArea.setPrefRowCount(24);
        dbResultArea.setStyle("-fx-font-family: 'Consolas', 'Microsoft YaHei', monospace; -fx-font-size: 13px;");
        dbResultArea.setPromptText("扫描结果将在此显示...");
        VBox.setVgrow(dbResultArea, Priority.ALWAYS);

        root.getChildren().addAll(title, subtitle, connRow, actionRow, dbResultArea);
        return root;
    }

    private void runDatabaseScan() {
        dbProgress.setVisible(true);
        dbStatusLabel.setText("正在连接数据库...");
        dbResultArea.clear();

        String jsonBody = String.format("""
            {
                "host": "%s",
                "port": %s,
                "username": "%s",
                "password": "%s",
                "database": "%s",
                "sourceDbType": "%s",
                "targetDbType": "%s"
            }
            """,
            dbHostField.getText(), dbPortField.getText(),
            dbUserField.getText(), dbPasswordField.getText(),
            dbNameField.getText(),
            dbSourceCombo.getValue(), dbTargetCombo.getValue()
        );

        new Thread(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/v1/scan/database"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                Platform.runLater(() -> {
                    dbProgress.setVisible(false);
                    if (response.statusCode() == 200) {
                        dbStatusLabel.setText("扫描完成 ✓");
                        dbStatusLabel.setTextFill(Color.valueOf("#16a34a"));
                        dbResultArea.setText(formatScanResult(response.body()));
                    } else {
                        dbStatusLabel.setText("扫描失败 ✗");
                        dbStatusLabel.setTextFill(Color.valueOf("#dc2626"));
                        dbResultArea.setText("HTTP " + response.statusCode() + "\n" + response.body());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    dbProgress.setVisible(false);
                    dbStatusLabel.setText("连接失败: " + e.getMessage());
                    dbStatusLabel.setTextFill(Color.valueOf("#dc2626"));
                    dbResultArea.setText("错误: " + e.getMessage()
                            + "\n\n请确认:\n1. 数据库服务已启动\n2. 主机/端口/用户名/密码正确\n3. 网络连接正常");
                });
            }
        }).start();
    }

    private String formatScanResult(String json) {
        StringBuilder sb = new StringBuilder();
        sb.append("╔══════════════════════════════════════╗\n");
        sb.append("║      DMA 数据库兼容性体检报告        ║\n");
        sb.append("╚══════════════════════════════════════╝\n\n");

        try {
            String body = json;
            if (json.contains("\"data\":")) {
                body = json.substring(json.indexOf("\"data\":") + 7);
                if (body.endsWith("}}")) body = body.substring(0, body.length() - 1);
            }

            String sourceDb = extractJsonValue(body, "sourceDbType");
            String targetDb = extractJsonValue(body, "targetDbType");
            String dbName = extractJsonValue(body, "databaseName");
            int procCount = extractJsonInt(body, "storedProcedureCount");
            int funcCount = extractJsonInt(body, "functionCount");
            int tableCount = extractJsonInt(body, "tableCount");
            int viewCount = extractJsonInt(body, "viewCount");
            int totalObjects = extractJsonInt(body, "totalObjects");
            int compatible = extractJsonInt(body, "compatibleCount");
            int autoConvert = extractJsonInt(body, "autoConvertibleCount");
            int manual = extractJsonInt(body, "manualReviewCount");
            int incompatible = extractJsonInt(body, "incompatibleCount");
            double rate = extractJsonDouble(body, "compatibilityRate");

            sb.append(String.format("  数据库: %s (%s → %s)\n\n", dbName, sourceDb, targetDb));
            sb.append("  ┌────────────────────────────────────┐\n");
            sb.append(String.format("  │  存储过程: %-4d  函数: %-4d       │\n", procCount, funcCount));
            sb.append(String.format("  │  表:       %-4d  视图:  %-4d       │\n", tableCount, viewCount));
            sb.append(String.format("  │  总对象数: %d                        │\n", totalObjects));
            sb.append("  └────────────────────────────────────┘\n\n");
            sb.append("  ┌────────────────────────────────────┐\n");
            sb.append(String.format("  │  ✓ 完全兼容:   %-4d               │\n", compatible));
            sb.append(String.format("  │  ⚡ 可自动转换: %-4d               │\n", autoConvert));
            sb.append(String.format("  │  ⚠ 需人工审核: %-4d               │\n", manual));
            sb.append(String.format("  │  ✗ 不兼容:     %-4d               │\n", incompatible));
            sb.append("  └────────────────────────────────────┘\n\n");
            sb.append(String.format("  ★ 兼容率: %.1f%%\n\n", rate));

            // 进度条
            int barLen = 40;
            int filled = (int) (rate / 100.0 * barLen);
            sb.append("  [");
            for (int i = 0; i < barLen; i++) {
                sb.append(i < filled ? "█" : "░");
            }
            sb.append(String.format("] %.1f%%\n", rate));

            // 列出有问题的对象
            if (manual + incompatible > 0) {
                sb.append("\n  ══════ 需关注的对象 ══════\n");
                // Extract objects from JSON (simplified)
                Pattern p = Pattern.compile("\"objectName\":\"([^\"]+)\".*?\"objectType\":\"([^\"]+)\".*?\"compatibilityLevel\":\"([^\"]+)\".*?\"severity\":\"([^\"]+)\"");
                Matcher m = p.matcher(body);
                int issueCount = 0;
                while (m.find() && issueCount < 30) {
                    String level = m.group(3);
                    if (!"COMPATIBLE".equals(level)) {
                        sb.append(String.format("  [%s] %s (%s) — %s\n",
                                m.group(4), m.group(1), m.group(2), level));
                        issueCount++;
                    }
                }
            }
        } catch (Exception e) {
            sb.append("  (解析错误: ").append(e.getMessage()).append(")\n");
            sb.append("\n  原始数据:\n").append(json);
        }
        return sb.toString();
    }

    // ==================== SQL 转换页面 ====================

    private VBox buildSqlConvertPage() {
        VBox root = new VBox(12);
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color: #f8fafc;");

        Label title = new Label("SQL 兼容性转换");
        title.setFont(Font.font("System", FontWeight.BOLD, 22));
        title.setTextFill(Color.valueOf("#1e40af"));

        HBox selectorRow = new HBox(16);
        selectorRow.setAlignment(Pos.CENTER_LEFT);
        sqlSourceCombo = new ComboBox<>(FXCollections.observableArrayList("MYSQL", "ORACLE", "SQLSERVER"));
        sqlSourceCombo.setValue("MYSQL"); sqlSourceCombo.setPrefWidth(140);
        sqlTargetCombo = new ComboBox<>(FXCollections.observableArrayList("POSTGRESQL", "DAMENG", "GAUSSDB", "OCEANBASE", "GOLDENDB"));
        sqlTargetCombo.setValue("POSTGRESQL"); sqlTargetCombo.setPrefWidth(140);

        Button scanBtn = new Button("扫描 SQL");
        scanBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white;");
        scanBtn.setOnAction(e -> runSqlScan());

        sqlStatusLabel = new Label("");
        selectorRow.getChildren().addAll(
                new Label("源:"), sqlSourceCombo,
                new Label("目标:"), sqlTargetCombo,
                scanBtn, sqlStatusLabel
        );

        sqlInput = new TextArea();
        sqlInput.setPromptText("输入 SQL 语句...");
        sqlInput.setPrefRowCount(10);
        sqlInput.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 13px;");

        sqlResult = new TextArea();
        sqlResult.setEditable(false);
        sqlResult.setPrefRowCount(14);
        sqlResult.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 13px;");

        SplitPane split = new SplitPane();
        split.setOrientation(javafx.geometry.Orientation.VERTICAL);
        split.getItems().addAll(sqlInput, sqlResult);
        split.setDividerPosition(0, 0.38);
        VBox.setVgrow(split, Priority.ALWAYS);

        root.getChildren().addAll(title, selectorRow, split);
        return root;
    }

    private void runSqlScan() {
        String sql = sqlInput.getText().trim();
        if (sql.isEmpty()) return;

        sqlStatusLabel.setText("扫描中...");
        String jsonBody = String.format("""
            {"sql": "%s", "sourceDbType": "%s", "targetDbType": "%s"}
            """, escapeJson(sql), sqlSourceCombo.getValue(), sqlTargetCombo.getValue());

        new Thread(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/v1/scan/sql"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody)).build();
                String resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString()).body();
                Platform.runLater(() -> {
                    sqlStatusLabel.setText("完成");
                    sqlResult.setText(resp.replace("},{", "},\n{"));
                });
            } catch (Exception e) {
                Platform.runLater(() -> sqlStatusLabel.setText("失败: " + e.getMessage()));
            }
        }).start();
    }

    // ==================== 工具方法 ====================

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\t", "\\t");
    }

    private String extractJsonValue(String json, String key) {
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : "";
    }

    private int extractJsonInt(String json, String key) {
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*(-?\\d+)");
        Matcher m = p.matcher(json);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }

    private double extractJsonDouble(String json, String key) {
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*(-?[\\d.]+)");
        Matcher m = p.matcher(json);
        return m.find() ? Double.parseDouble(m.group(1)) : 100.0;
    }

    @Override
    public void stop() {
        if (springContext != null) springContext.close();
        Platform.exit();
    }
}
