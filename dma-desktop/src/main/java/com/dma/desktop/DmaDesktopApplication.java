package com.dma.desktop;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * DMA 桌面端启动类。
 * 内嵌 Spring Boot 后端，通过 HTTP localhost 调用 API。
 */
public class DmaDesktopApplication extends Application {

    private ConfigurableApplicationContext springContext;
    private HttpClient httpClient;
    private TextArea sqlInput;
    private TextArea resultOutput;
    private ComboBox<String> sourceDbCombo;
    private ComboBox<String> targetDbCombo;
    private Label statusLabel;

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
        stage.setTitle("Database Migration Assistant (DMA) — MVP v1.0.0");
        stage.setWidth(1200);
        stage.setHeight(800);

        // === 顶部工具栏 ===
        sourceDbCombo = new ComboBox<>(FXCollections.observableArrayList(
                "MYSQL", "ORACLE", "SQLSERVER"));
        sourceDbCombo.setValue("MYSQL");
        sourceDbCombo.setPrefWidth(140);

        targetDbCombo = new ComboBox<>(FXCollections.observableArrayList(
                "POSTGRESQL", "DAMENG", "GAUSSDB", "GOLDENDB", "OCEANBASE"));
        targetDbCombo.setValue("POSTGRESQL");
        targetDbCombo.setPrefWidth(140);

        Button scanBtn = new Button("扫描 SQL");
        scanBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-size: 14px;");
        scanBtn.setOnAction(e -> runScan());

        Button healthBtn = new Button("健康检查");
        healthBtn.setOnAction(e -> checkHealth());

        ToolBar toolbar = new ToolBar(
                new Label("源数据库: "), sourceDbCombo,
                new Separator(), new Label("目标数据库: "), targetDbCombo,
                new Separator(), scanBtn, healthBtn
        );

        // === 中间区域：SQL 输入 + 结果输出 ===
        sqlInput = new TextArea();
        sqlInput.setPromptText("在此输入 SQL 语句，例如：\nSELECT IFNULL(name, 'N/A') FROM users WHERE id > 0 LIMIT 10, 20;\nSELECT NOW();\nCREATE TABLE t (id INT AUTO_INCREMENT, created DATETIME) ENGINE=InnoDB;");
        sqlInput.setPrefRowCount(10);
        sqlInput.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 13px;");

        resultOutput = new TextArea();
        resultOutput.setEditable(false);
        resultOutput.setPromptText("扫描结果将显示在这里...");
        resultOutput.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 13px;");

        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(javafx.geometry.Orientation.VERTICAL);
        splitPane.getItems().addAll(sqlInput, resultOutput);
        splitPane.setDividerPosition(0, 0.35);

        // === 状态栏 ===
        statusLabel = new Label("就绪 | 规则数: 83 条 (3条路径) | API: http://localhost:8080");
        statusLabel.setPadding(new Insets(4, 8, 4, 8));

        // === 主布局 ===
        BorderPane root = new BorderPane();
        root.setTop(toolbar);
        root.setCenter(splitPane);
        root.setBottom(statusLabel);

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();

        // 启动后自动检查健康状态
        checkHealth();
    }

    private void runScan() {
        String sql = sqlInput.getText().trim();
        if (sql.isEmpty()) {
            statusLabel.setText("请输入 SQL 语句");
            return;
        }

        statusLabel.setText("正在扫描...");
        resultOutput.setText("分析中...\n");
        resultOutput.appendText("源: " + sourceDbCombo.getValue() + " → 目标: " + targetDbCombo.getValue() + "\n");
        resultOutput.appendText("SQL: " + sql + "\n");
        resultOutput.appendText("=".repeat(60) + "\n\n");

        try {
            String jsonBody = "{"
                    + "\"sql\": \"" + escapeJson(sql) + "\","
                    + "\"sourceDbType\": \"" + sourceDbCombo.getValue() + "\","
                    + "\"targetDbType\": \"" + targetDbCombo.getValue() + "\""
                    + "}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/v1/scan/sql"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            resultOutput.appendText("HTTP " + response.statusCode() + "\n");
            resultOutput.appendText(prettyPrint(response.body()));
            statusLabel.setText("扫描完成 ✓");
        } catch (Exception e) {
            resultOutput.appendText("错误: " + e.getMessage() + "\n");
            statusLabel.setText("扫描失败 ✗: " + e.getMessage());
        }
    }

    private void checkHealth() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/v1/system/health"))
                    .GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            statusLabel.setText("后端服务: ✓ 正常 | API: http://localhost:8080 | 规则: 83条");
        } catch (Exception e) {
            statusLabel.setText("后端服务: ✗ 未连接 | " + e.getMessage());
        }
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private String prettyPrint(String json) {
        try {
            return json.replace("},{", "},\n{")
                       .replace("[{", "[\n{")
                       .replace("}]", "}\n]")
                       .replace(",\"", ",\n\"");
        } catch (Exception e) {
            return json;
        }
    }

    @Override
    public void stop() {
        if (springContext != null) {
            springContext.close();
        }
        Platform.exit();
    }
}
