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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DmaDesktopApplication extends Application {

    private ConfigurableApplicationContext springContext;
    private HttpClient httpClient;
    private Label statusLeft, statusConn, statusRules, statusApi;
    private StackPane contentArea;
    private Map<String, Button> navButtons = new LinkedHashMap<>();
    private String currentPage = "dashboard";

    // 体检页
    private TextField dbHost, dbPort, dbUser, dbPassword;
    private ComboBox<String> dbSchema, dbSource, dbTarget, savedConn;
    private TextArea dbResult;
    private ProgressIndicator dbProgress;
    // SQL页
    private TextArea sqlInput, sqlResult;
    private ComboBox<String> sqlSource, sqlTarget;
    // SP页
    private TextArea spInput, spResult;
    private ComboBox<String> spSource, spTarget;
    // 项目页
    private TextField projPath;
    private ComboBox<String> projSource, projTarget;
    private TextArea projResult;
    private ProgressIndicator projProgress;
    // AI页
    private TextArea aiInput, aiResult;
    private Label aiStatus;
    // 连接缓存
    private Map<String, Map<String, String>> savedConns = new HashMap<>();

    public static void main(String[] args) { launch(args); }

    @Override
    public void init() {
        springContext = SpringApplication.run(com.dma.core.DmaCoreApplication.class);
        httpClient = HttpClient.newHttpClient();
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("DMA — Database Migration Assistant");
        stage.setWidth(1280); stage.setHeight(860);
        stage.setMinWidth(1024); stage.setMinHeight(680);

        BorderPane root = new BorderPane();
        root.setLeft(buildSidebar());
        root.setBottom(buildStatusbar());
        contentArea = new StackPane();
        contentArea.setStyle("-fx-background-color: #f1f5f9;");
        contentArea.getChildren().add(buildDashboard());
        root.setCenter(contentArea);

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();

        refreshStatus();
        loadSavedConnections();
    }

    // ==================== 侧边栏 ====================
    private VBox buildSidebar() {
        VBox sidebar = new VBox(0);
        sidebar.setPrefWidth(210); sidebar.setMinWidth(210);
        sidebar.setStyle("-fx-background-color: #1e293b;");

        VBox logoBox = new VBox(6);
        logoBox.setPadding(new Insets(20, 18, 16, 18));
        logoBox.setStyle("-fx-background-color: #0f172a;");
        Label logoTitle = new Label("DMA");
        logoTitle.setFont(Font.font("System", FontWeight.BOLD, 22));
        logoTitle.setTextFill(Color.WHITE);
        Label logoSub = new Label("Database Migration Assistant");
        logoSub.setFont(Font.font("System", 11));
        logoSub.setTextFill(Color.valueOf("#94a3b8"));
        logoBox.getChildren().addAll(logoTitle, logoSub);
        sidebar.getChildren().add(logoBox);

        String[][] items = {{"🏠", "首页概览", "dashboard"}, {"🏥", "数据库体检", "scan"}, {"🔄", "SQL 转换", "sql"},
                {"📦", "存储过程迁移", "procedure"}, {"📂", "项目源码扫描", "project"}, {"🤖", "AI 顾问", "ai"}};

        for (String[] item : items) {
            Button btn = new Button(item[0] + "  " + item[1]);
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setAlignment(Pos.CENTER_LEFT);
            btn.setPadding(new Insets(13, 22, 13, 22));
            btn.setFont(Font.font("System", 14));
            btn.setTextFill(Color.valueOf("#cbd5e1"));
            btn.setStyle("-fx-background-color: transparent; -fx-border-width: 0; -fx-cursor: hand;");

            final String page = item[2];
            btn.setOnMouseEntered(e -> { if (!page.equals(currentPage)) btn.setStyle("-fx-background-color: #334155; -fx-border-width: 0; -fx-cursor: hand;"); });
            btn.setOnMouseExited(e -> { if (!page.equals(currentPage)) btn.setStyle("-fx-background-color: transparent; -fx-border-width: 0; -fx-cursor: hand;"); });
            btn.setOnAction(e -> switchPage(page));
            navButtons.put(page, btn);
            sidebar.getChildren().add(btn);
        }

        Region spacer = new Region(); VBox.setVgrow(spacer, Priority.ALWAYS);
        sidebar.getChildren().add(spacer);
        Label ver = new Label("v1.0.0");
        ver.setFont(Font.font("System", 11)); ver.setTextFill(Color.valueOf("#475569"));
        ver.setPadding(new Insets(12, 22, 12, 22));
        sidebar.getChildren().add(ver);
        return sidebar;
    }

    private void switchPage(String page) {
        currentPage = page;
        navButtons.forEach((k, btn) -> btn.setStyle(k.equals(page)
                ? "-fx-background-color: #2563eb; -fx-border-width: 0 0 0 3; -fx-border-color: #60a5fa; -fx-cursor: hand;"
                : "-fx-background-color: transparent; -fx-border-width: 0; -fx-cursor: hand;"));
        contentArea.getChildren().clear();
        contentArea.getChildren().add(switch (page) {
            case "scan" -> buildScanPage();
            case "sql" -> buildSqlPage();
            case "procedure" -> buildProcedurePage();
            case "project" -> buildProjectPage();
            case "ai" -> buildAiPage();
            default -> buildDashboard();
        });
    }

    // ==================== 状态栏 ====================
    private HBox buildStatusbar() {
        HBox bar = new HBox(20);
        bar.setPadding(new Insets(5, 18, 5, 18));
        bar.setStyle("-fx-background-color: #e2e8f0; -fx-border-width: 1 0 0 0; -fx-border-color: #cbd5e1;");
        bar.setAlignment(Pos.CENTER_LEFT);
        statusLeft = lbl("就绪", 12, "#475569");
        statusApi = lbl("API: --", 11, "#6366f1");
        statusRules = lbl("规则: 138条", 11, "#059669");
        statusConn = lbl("连接: --", 11, "#d97706");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        bar.getChildren().addAll(statusLeft, sp, statusApi, statusRules, statusConn);
        return bar;
    }

    private void refreshStatus() {
        new Thread(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/api/v1/system/info")).GET().build();
                httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                Platform.runLater(() -> statusApi.setText("API: ✓ 8080"));
            } catch (Exception e) { Platform.runLater(() -> statusApi.setText("API: ✗")); }
        }).start();
    }

    // ==================== 首页 ====================
    private ScrollPane buildDashboard() {
        VBox root = new VBox(22);
        root.setPadding(new Insets(28));
        root.setStyle("-fx-background-color: #f1f5f9;");
        root.getChildren().add(title("欢迎使用 Database Migration Assistant"));
        root.getChildren().add(subtitle("数据库迁移与兼容性分析工具 — 支持 MySQL/Oracle/SQLServer → PG/GaussDB/达梦/OceanBase/GoldenDB"));

        HBox cards = new HBox(16);
        cards.getChildren().addAll(
                statCard("138", "兼容性规则", "#2563eb"),
                statCard("5", "迁移路径", "#7c3aed"),
                statCard("19/19", "测试通过", "#059669"),
                statCard("3", "报告格式", "#d97706"));
        root.getChildren().add(cards);

        root.getChildren().add(titleSm("快捷功能"));
        HBox quick = new HBox(12);
        quick.getChildren().addAll(
                quickCard("🏥", "数据库体检", "连接源库 · 全量扫描 · 兼容率", () -> switchPage("scan")),
                quickCard("🔄", "SQL 转换", "粘贴SQL · 自动转换 · 语法对比", () -> switchPage("sql")),
                quickCard("📦", "存储过程", "PROCEDURE/FUNCTION/TRIGGER/VIEW", () -> switchPage("procedure")),
                quickCard("📂", "项目扫描", "源码扫描 · 风险分级 · 报告", () -> switchPage("project")));
        root.getChildren().add(quick);

        ScrollPane sp = new ScrollPane(root); sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color: #f1f5f9;");
        return sp;
    }

    private VBox statCard(String val, String lab, String color) {
        VBox c = new VBox(6); c.setPadding(new Insets(20)); c.setPrefWidth(200);
        c.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 8, 0, 0, 2);");
        Label v = new Label(val); v.setFont(Font.font("System", FontWeight.BOLD, 30)); v.setTextFill(Color.valueOf(color));
        Label l = new Label(lab); l.setFont(Font.font("System", 13)); l.setTextFill(Color.valueOf("#64748b"));
        c.getChildren().addAll(v, l); return c;
    }

    private VBox quickCard(String icon, String t, String desc, Runnable action) {
        VBox c = new VBox(8); c.setPadding(new Insets(18)); c.setPrefWidth(190);
        c.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 8, 0, 0, 2);");
        c.setOnMouseClicked(e -> action.run());
        c.setOnMouseEntered(e -> c.setStyle("-fx-background-color: #eff6ff; -fx-background-radius: 10; -fx-cursor: hand;"));
        c.setOnMouseExited(e -> c.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-cursor: hand;"));
        Label ic = new Label(icon); ic.setFont(Font.font(30));
        Label tl = new Label(t); tl.setFont(Font.font("System", FontWeight.BOLD, 14));
        Label ds = new Label(desc); ds.setFont(Font.font("System", 11)); ds.setTextFill(Color.valueOf("#64748b"));
        c.getChildren().addAll(ic, tl, ds); return c;
    }

    // ==================== 体检页 ====================
    private ScrollPane buildScanPage() {
        VBox root = pageRoot("数据库兼容性体检", "连接源库 → 提取对象 → 分析兼容性 → 输出兼容率");

        savedConn = cb(240, "选择已保存的连接...");
        savedConn.setOnAction(e -> onConnSelected());
        Button refreshBtn = sbtn("🔄 刷新", "#3b82f6"); refreshBtn.setOnAction(e -> loadSavedConnections());
        Button saveBtn = sbtn("💾 保存", "#10b981"); saveBtn.setOnAction(e -> saveConn());
        Button delBtn = sbtn("🗑 删除", "#ef4444"); delBtn.setOnAction(e -> deleteConn());
        HBox savedRow = hbox(8, savedConn, refreshBtn, saveBtn, delBtn);
        savedRow.setPadding(new Insets(8, 14, 8, 14));
        savedRow.setStyle("-fx-background-color: #eff6ff; -fx-background-radius: 8;");

        dbHost = tf("localhost", 130, "主机"); dbPort = tf("3306", 70, "端口");
        dbUser = tf("root", 100, "用户名");
        dbPassword = new PasswordField(); dbPassword.setPrefWidth(100); dbPassword.setPromptText("密码");
        dbPassword.setStyle("-fx-background-radius: 4; -fx-border-radius: 4; -fx-border-color: #d1d5db; -fx-padding: 6 10;");
        dbSchema = cb(160, "先连接后选择");
        dbSource = cbv(140, "MYSQL", "MYSQL", "ORACLE", "SQLSERVER");
        dbTarget = cbv(140, "POSTGRESQL", "POSTGRESQL", "DAMENG", "GAUSSDB", "OCEANBASE", "GOLDENDB");
        Button connectBtn = sbtn("获取Schema", "#0891b2"); connectBtn.setOnAction(e -> discoverSchemas());

        HBox connRow = hbox(8, lbl("主机:"), dbHost, lbl("端口:"), dbPort, lbl("用户:"), dbUser,
                lbl("密码:"), dbPassword, connectBtn, lbl("Schema:"), dbSchema);
        connRow.setPadding(new Insets(8, 14, 8, 14));
        connRow.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");

        dbProgress = new ProgressIndicator(-1); dbProgress.setVisible(false); dbProgress.setPrefSize(24, 24);
        Button scanBtn = bbtn("🔍 开始扫描", "#2563eb"); scanBtn.setOnAction(e -> runDbScan());
        Button exportBtn = sbtn("📄 导出", "#f59e0b");
        exportBtn.setOnAction(e -> exportReport("数据库体检报告", dbSource.getValue() + " → " + dbTarget.getValue(), dbResult.getText()));

        HBox act = hbox(16, lbl("源:"), dbSource, lbl("目标:"), dbTarget, scanBtn, exportBtn, dbProgress);

        dbResult = ta(20, "扫描结果将在此显示...");
        VBox.setVgrow(dbResult, Priority.ALWAYS);
        root.getChildren().addAll(savedRow, connRow, act, dbResult);
        return wrap(root);
    }

    // ==================== SQL页 ====================
    private ScrollPane buildSqlPage() {
        VBox root = pageRoot("SQL 兼容性转换", "输入 SQL → 规则引擎匹配 → 自动转换 → 原SQL/新SQL对比");
        sqlSource = cbv(130, "MYSQL", "MYSQL", "ORACLE", "SQLSERVER");
        sqlTarget = cbv(140, "POSTGRESQL", "POSTGRESQL", "DAMENG", "GAUSSDB", "OCEANBASE", "GOLDENDB");
        Button scanBtn = bbtn("🔍 扫描 SQL", "#2563eb"); scanBtn.setOnAction(e -> runSqlConvert());
        Button exportBtn = sbtn("📄 导出", "#f59e0b");
        exportBtn.setOnAction(e -> exportReport("SQL 转换报告", sqlSource.getValue() + " → " + sqlTarget.getValue(), sqlResult.getText()));
        HBox row = hbox(12, lbl("源:"), sqlSource, lbl("目标:"), sqlTarget, scanBtn, exportBtn);

        sqlInput = ta(9, "输入 SQL 语句...\n\n例: SELECT IFNULL(name, '') FROM users LIMIT 0, 10;");
        sqlResult = ta(13, "转换结果将显示在此...");
        SplitPane split = splitV(sqlInput, sqlResult, 0.38);
        root.getChildren().addAll(row, split); VBox.setVgrow(split, Priority.ALWAYS);
        return wrap(root);
    }

    // ==================== 存储过程页 ====================
    private ScrollPane buildProcedurePage() {
        VBox root = pageRoot("存储过程迁移", "粘贴 PROCEDURE/FUNCTION/TRIGGER/VIEW → 自动转换语法");
        spSource = cbv(130, "MYSQL", "MYSQL", "ORACLE", "SQLSERVER");
        spTarget = cbv(140, "GAUSSDB", "GAUSSDB", "POSTGRESQL", "DAMENG", "OCEANBASE", "GOLDENDB");
        Button convBtn = bbtn("🔄 转换", "#7c3aed"); convBtn.setOnAction(e -> runProcedureConvert());
        HBox row = hbox(12, lbl("源:"), spSource, lbl("目标:"), spTarget, convBtn);
        spInput = ta(11, "粘贴 CREATE PROCEDURE/FUNCTION/TRIGGER/VIEW ...");
        spResult = ta(13, "转换结果将显示在此...");
        SplitPane split = splitV(spInput, spResult, 0.42);
        root.getChildren().addAll(row, split); VBox.setVgrow(split, Priority.ALWAYS);
        return wrap(root);
    }

    // ==================== 项目扫描页 ====================
    private ScrollPane buildProjectPage() {
        VBox root = pageRoot("项目源码扫描", "扫描整个项目目录: Java/XML/SQL → 风险分级统计");
        projPath = tf(System.getProperty("user.dir"), 420, "项目根目录");
        projSource = cbv(120, "MYSQL", "MYSQL", "ORACLE", "SQLSERVER");
        projTarget = cbv(130, "POSTGRESQL", "POSTGRESQL", "DAMENG", "GAUSSDB", "OCEANBASE", "GOLDENDB");
        projProgress = new ProgressIndicator(-1); projProgress.setVisible(false); projProgress.setPrefSize(24, 24);
        Button scanBtn = bbtn("🔍 开始扫描", "#059669"); scanBtn.setOnAction(e -> runProjectScan());
        Button exportBtn = sbtn("📄 导出", "#f59e0b");
        exportBtn.setOnAction(e -> exportReport("项目源码扫描报告", projSource.getValue() + " → " + projTarget.getValue(), projResult.getText()));
        HBox row = hbox(12, lbl("路径:"), projPath, lbl("源:"), projSource, lbl("目标:"), projTarget, scanBtn, exportBtn, projProgress);
        projResult = ta(20, "扫描结果将在此显示...");
        VBox.setVgrow(projResult, Priority.ALWAYS);
        root.getChildren().addAll(row, projResult);
        return wrap(root);
    }

    // ==================== AI页 ====================
    private ScrollPane buildAiPage() {
        VBox root = pageRoot("AI 迁移顾问", "本地 Ollama / 云端 OpenAI / 通义千问 / DeepSeek 多模式支持");
        aiStatus = lbl("检查 AI 服务...", 13, "#64748b");
        Button checkBtn = sbtn("🔄 检查状态", "#3b82f6"); checkBtn.setOnAction(e -> checkAiStatus());
        Button askBtn = bbtn("💡 AI 分析", "#7c3aed"); askBtn.setOnAction(e -> askAi());
        HBox row = hbox(12, aiStatus, checkBtn, askBtn);
        aiInput = ta(8, "粘贴 SQL 或描述迁移问题...\n\n配置: application.yml → dma.ai.provider=ollama|openai|custom");
        aiResult = ta(14, "AI 回复将显示在此..."); aiResult.setEditable(false);
        SplitPane split = splitV(aiInput, aiResult, 0.32);
        root.getChildren().addAll(row, split); VBox.setVgrow(split, Priority.ALWAYS);
        return wrap(root);
    }

    // ==================== 业务逻辑 ====================
    private void loadSavedConnections() {
        new Thread(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/api/v1/connections?page=1&size=50")).GET().build();
                String resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString()).body();
                Platform.runLater(() -> {
                    savedConns.clear(); if (savedConn != null) savedConn.getItems().clear();
                    for (String obj : resp.split("\\},\\s*\\{")) {
                        String name = ej(obj, "name"); if (name.isEmpty()) continue;
                        if (savedConn != null) savedConn.getItems().add(name);
                        Map<String, String> info = new HashMap<>();
                        info.put("id", ej(obj, "id")); info.put("dbType", ej(obj, "dbType"));
                        info.put("host", ej(obj, "host")); info.put("port", ej(obj, "port"));
                        info.put("username", ej(obj, "username")); info.put("databaseName", ej(obj, "databaseName"));
                        savedConns.put(name, info);
                    }
                    statusConn.setText("连接: " + savedConns.size() + "个");
                });
            } catch (Exception ignored) {}
        }).start();
    }

    private void onConnSelected() {
        if (savedConn == null || savedConn.getValue() == null) return;
        Map<String, String> info = savedConns.get(savedConn.getValue());
        if (info == null) return;
        dbHost.setText(info.getOrDefault("host", "localhost"));
        dbPort.setText(info.getOrDefault("port", "3306"));
        dbUser.setText(info.getOrDefault("username", "root"));
        dbPassword.setText("");
        String dbType = info.getOrDefault("dbType", "MYSQL");
        if (dbSource.getItems().contains(dbType)) dbSource.setValue(dbType);
        statusLeft.setText("已选择: " + savedConn.getValue());
    }

    private void saveConn() {
        String name = dbSchema.getValue() != null ? dbSchema.getValue() : (dbHost.getText() + "_db");
        TextInputDialog d = new TextInputDialog(name);
        d.setTitle("保存连接"); d.setHeaderText("给连接命名");
        d.showAndWait().ifPresent(n -> new Thread(() -> {
            try {
                String body = String.format("""
{"name":"%s","dbType":"%s","host":"%s","port":%s,"username":"%s","password":"%s","databaseName":"%s"}""",
                        esc(n), esc(dbSource.getValue()), esc(dbHost.getText()), esc(dbPort.getText()),
                        esc(dbUser.getText()), esc(dbPassword.getText()), esc(dbSchema.getValue() != null ? dbSchema.getValue() : ""));
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/api/v1/connections"))
                        .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
                httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                Platform.runLater(() -> { statusLeft.setText("已保存: " + n); loadSavedConnections(); });
            } catch (Exception ignored) {}
        }).start());
    }

    private void deleteConn() {
        if (savedConn == null || savedConn.getValue() == null) return;
        String name = savedConn.getValue();
        String id = savedConns.containsKey(name) ? savedConns.get(name).get("id") : null;
        if (id == null || id.isEmpty()) return;
        new Thread(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/api/v1/connections/" + id)).DELETE().build();
                httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                Platform.runLater(() -> { statusLeft.setText("已删除: " + name); loadSavedConnections(); });
            } catch (Exception ignored) {}
        }).start();
    }

    private void discoverSchemas() {
        statusLeft.setText("获取 Schema...");
        new Thread(() -> {
            try {
                String body = String.format("""
{"host":"%s","port":%s,"username":"%s","password":"%s","sourceDbType":"%s"}""",
                        esc(dbHost.getText()), esc(dbPort.getText()), esc(dbUser.getText()), esc(dbPassword.getText()), esc(dbSource.getValue()));
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/api/v1/scan/schemas"))
                        .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
                String resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString()).body();
                Platform.runLater(() -> {
                    List<String> schemas = new ArrayList<>();
                    Matcher m = Pattern.compile("\"([^\"]+)\"").matcher(resp);
                    while (m.find()) { String s = m.group(1); if (s.length() > 1 && !s.equals("data")) schemas.add(s); }
                    dbSchema.getItems().setAll(schemas);
                    if (!schemas.isEmpty()) dbSchema.setValue(schemas.get(0));
                    statusLeft.setText("找到 " + schemas.size() + " 个 Schema");
                });
            } catch (Exception e) { Platform.runLater(() -> statusLeft.setText("连接失败: " + e.getMessage())); }
        }).start();
    }

    private void runDbScan() {
        dbProgress.setVisible(true); statusLeft.setText("扫描中...");
        new Thread(() -> {
            try {
                String schema = dbSchema.getValue() != null ? dbSchema.getValue() : "";
                String body = String.format("""
{"host":"%s","port":%s,"username":"%s","password":"%s","database":"%s","sourceDbType":"%s","targetDbType":"%s"}""",
                        esc(dbHost.getText()), esc(dbPort.getText()), esc(dbUser.getText()), esc(dbPassword.getText()),
                        esc(schema), esc(dbSource.getValue()), esc(dbTarget.getValue()));
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/api/v1/scan/database"))
                        .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
                String resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString()).body();
                Platform.runLater(() -> { dbProgress.setVisible(false); statusLeft.setText("扫描完成"); dbResult.setText(formatScan(resp)); });
            } catch (Exception e) { Platform.runLater(() -> { dbProgress.setVisible(false); statusLeft.setText("失败"); }); }
        }).start();
    }

    private void runSqlConvert() {
        String sql = sqlInput.getText().trim(); if (sql.isEmpty()) return;
        statusLeft.setText("转换中...");
        new Thread(() -> {
            try {
                String body = String.format("""
{"sql":"%s","sourceDbType":"%s","targetDbType":"%s"}""",
                        esc(sql), esc(sqlSource.getValue()), esc(sqlTarget.getValue()));
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/api/v1/scan/sql"))
                        .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
                String resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString()).body();
                Platform.runLater(() -> { statusLeft.setText("转换完成"); sqlResult.setText(formatSql(resp, sql)); });
            } catch (Exception e) { Platform.runLater(() -> statusLeft.setText("失败")); }
        }).start();
    }

    private void runProcedureConvert() {
        String ddl = spInput.getText().trim(); if (ddl.isEmpty()) return;
        statusLeft.setText("转换中...");
        new Thread(() -> {
            try {
                String body = String.format("""
{"ddl":"%s","sourceDbType":"%s","targetDbType":"%s"}""",
                        esc(ddl), esc(spSource.getValue()), esc(spTarget.getValue()));
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/api/v1/convert/procedure"))
                        .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
                String resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString()).body();
                Platform.runLater(() -> { statusLeft.setText("转换完成"); spResult.setText(formatSp(resp)); });
            } catch (Exception e) { Platform.runLater(() -> statusLeft.setText("失败")); }
        }).start();
    }

    private void runProjectScan() {
        projProgress.setVisible(true); statusLeft.setText("扫描中...");
        new Thread(() -> {
            try {
                String body = String.format("""
{"projectPath":"%s","sourceDbType":"%s","targetDbType":"%s"}""",
                        esc(projPath.getText()), esc(projSource.getValue()), esc(projTarget.getValue()));
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/api/v1/scan/project-full"))
                        .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
                String resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString()).body();
                Platform.runLater(() -> { projProgress.setVisible(false); statusLeft.setText("扫描完成"); projResult.setText(formatProj(resp)); });
            } catch (Exception e) { Platform.runLater(() -> { projProgress.setVisible(false); statusLeft.setText("失败"); }); }
        }).start();
    }

    private void checkAiStatus() {
        new Thread(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/api/v1/ai/status")).GET().build();
                String resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString()).body();
                Platform.runLater(() -> {
                    if (resp.contains("\"available\":true")) { aiStatus.setText("🤖 AI 已连接"); aiStatus.setTextFill(Color.valueOf("#16a34a")); }
                    else { aiStatus.setText("⚠ AI 未启用"); aiStatus.setTextFill(Color.valueOf("#d97706")); }
                });
            } catch (Exception e) { Platform.runLater(() -> { aiStatus.setText("✗ 不可用"); aiStatus.setTextFill(Color.valueOf("#dc2626")); }); }
        }).start();
    }

    private void askAi() {
        String input = aiInput.getText().trim(); if (input.isEmpty()) return;
        statusLeft.setText("AI 思考中...");
        new Thread(() -> {
            try {
                String body = String.format("""
{"sourceSql":"%s","message":"%s","severity":"WARNING","compatibilityLevel":"MANUAL_REVIEW","ruleCode":"USER_ASK"}""",
                        esc(input), esc(input));
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/api/v1/ai/advice"))
                        .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
                String resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString()).body();
                Platform.runLater(() -> {
                    String data = resp.contains("\"data\":\"") ? extractData(resp) : resp;
                    aiResult.setText("🤖 AI 建议:\n\n" + data.replace("\\n", "\n").replace("\\t", "    "));
                    statusLeft.setText("AI 回复就绪");
                });
            } catch (Exception e) { Platform.runLater(() -> statusLeft.setText("AI 失败")); }
        }).start();
    }

    private void exportReport(String title, String subtitle, String content) {
        if (content == null || content.isBlank()) return;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("导出报告"); alert.setHeaderText("选择导出格式");
        ButtonType htmlBtn = new ButtonType("HTML (浏览器)"), pdfBtn = new ButtonType("PDF (便携)"), wordBtn = new ButtonType("Word (可编辑)");
        alert.getButtonTypes().setAll(htmlBtn, pdfBtn, wordBtn, ButtonType.CANCEL);
        alert.showAndWait().ifPresent(btn -> {
            String fmt = btn == pdfBtn ? "PDF" : btn == wordBtn ? "WORD" : "HTML";
            String ext = btn == pdfBtn ? ".pdf" : btn == wordBtn ? ".docx" : ".html";
            new Thread(() -> {
                try {
                    String body = String.format("""
{"title":"%s","subtitle":"%s","content":"%s","format":"%s"}""",
                            esc(title), esc(subtitle), esc(content), fmt);
                    HttpRequest req = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/api/v1/report/export"))
                            .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
                    byte[] data = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray()).body();
                    String desktop = System.getProperty("user.home") + "\\Desktop";
                    String fn = "DMA_Report_" + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ext;
                    java.nio.file.Path fp = java.nio.file.Path.of(desktop, fn);
                    java.nio.file.Files.write(fp, data);
                    new Thread(() -> { try { java.awt.Desktop.getDesktop().open(fp.toFile()); } catch (Exception ignored) {} }).start();
                    Platform.runLater(() -> statusLeft.setText("已导出: " + fn));
                } catch (Exception ignored) {}
            }).start();
        });
    }

    // ==================== 格式化 ====================
    private String formatScan(String json) {
        String b = json.contains("\"data\":") ? json.substring(json.indexOf("\"data\":") + 7) : json;
        StringBuilder sb = new StringBuilder("╔══════════════════════════════════╗\n║    DMA 数据库兼容性体检报告       ║\n╚══════════════════════════════════╝\n\n");
        sb.append(String.format("  数据库: %s (%s → %s)\n\n", ej(b, "databaseName"), ej(b, "sourceDbType"), ej(b, "targetDbType")));
        sb.append(String.format("  存储过程: %d  函数: %d  表: %d  视图: %d\n\n", ji(b, "storedProcedureCount"), ji(b, "functionCount"), ji(b, "tableCount"), ji(b, "viewCount")));
        sb.append(String.format("  ✓ 完全兼容: %d  ⚡ 可自动转换: %d\n  ⚠ 需人工: %d  ✗ 不兼容: %d\n", ji(b, "compatibleCount"), ji(b, "autoConvertibleCount"), ji(b, "manualReviewCount"), ji(b, "incompatibleCount")));
        double rate = jd(b, "compatibilityRate");
        sb.append(String.format("\n  ★ 兼容率: %.1f%%\n  [", rate));
        int bar = (int)(rate / 100 * 40);
        sb.append("█".repeat(bar)).append("░".repeat(40-bar)).append(String.format("] %.1f%%\n", rate));
        return sb.toString();
    }

    private String formatSql(String json, String orig) {
        String b = json.contains("\"data\":") ? json.substring(json.indexOf("\"data\":") + 7) : json;
        StringBuilder sb = new StringBuilder(String.format("╔══════════════════════════════════╗\n║  %s → %s\n╚══════════════════════════════════╝\n\n", sqlSource.getValue(), sqlTarget.getValue()));
        int n = 0;
        Matcher m = Pattern.compile("\"ruleCode\":\"([^\"]+)\".*?\"severity\":\"([^\"]+)\".*?\"message\":\"([^\"]+)\"").matcher(b);
        while (m.find()) {
            n++; String sev = m.group(2);
            sb.append(String.format("[%s] %s — %s\n── 原 SQL ──\n  %s\n", "ERROR".equals(sev) ? "✗" : "WARNING".equals(sev) ? "⚠" : "ℹ", m.group(1), m.group(3), orig));
            Matcher sm = Pattern.compile("\"suggestedSql\":\"([^\"]+)\"").matcher(b);
            if (sm.find()) sb.append("  ↓↓↓ 转换后 ↓↓↓\n  ").append(sm.group(1).replace("\\\"", "\"")).append("\n");
            sb.append("\n");
        }
        sb.append(String.format("══════════════════════════════════\n  发现 %d 个问题\n", n));
        return sb.toString();
    }

    private String formatSp(String json) {
        String b = json.contains("\"data\":") ? json.substring(json.indexOf("\"data\":") + 6) : json;
        StringBuilder sb = new StringBuilder(String.format("╔══════════════════════════════════╗\n║  %s 迁移: %s → %s\n╚══════════════════════════════════╝\n\n",
                "PROCEDURE".equals(ej(b, "objectType")) ? "存储过程" : ej(b, "objectType"), spSource.getValue(), spTarget.getValue()));
        sb.append("── 原 DDL ──\n").append(ej(b, "originalDdl").replace("\\n", "\n").replace("\\t", "    ")).append("\n\n");
        sb.append("  ↓↓↓ 转换后 ↓↓↓\n\n── 转换后 DDL ──\n").append(ej(b, "convertedDdl").replace("\\n", "\n").replace("\\t", "    ")).append("\n\n");
        int c = ji(b, "changeCount");
        if (c > 0) { sb.append("── 变更 (").append(c).append("项) ──\n"); Matcher cm = Pattern.compile("\"changes\":\\[(.*?)\\]").matcher(b);
            if (cm.find()) { int x = 1; Matcher im = Pattern.compile("\"([^\"]+)\"").matcher(cm.group(1));
                while (im.find()) { String ch = im.group(1); if (ch.length() > 2) sb.append(x++).append(". ").append(ch).append("\n"); } } }
        return sb.toString();
    }

    private String formatProj(String json) {
        String b = json.contains("\"data\":") ? json.substring(json.indexOf("\"data\":") + 6) : json;
        StringBuilder sb = new StringBuilder("╔══════════════════════════════════╗\n║  DMA 项目源码扫描报告            ║\n╚══════════════════════════════════╝\n\n");
        sb.append(String.format("  路径: %s\n  迁移: %s → %s\n\n", ej(b, "projectPath"), ej(b, "sourceDbType"), ej(b, "targetDbType")));
        sb.append(String.format("  共扫描: %d 个文件\n  Java: %d  XML: %d  SQL: %d\n\n", ji(b, "totalFiles"), ji(b, "javaFiles"), ji(b, "xmlFiles"), ji(b, "sqlFiles")));
        sb.append(String.format("  ✗ 高风险: %d  ⚠ 中风险: %d  ℹ 低风险: %d\n", ji(b, "highRisk"), ji(b, "mediumRisk"), ji(b, "lowRisk")));
        double s = jd(b, "riskScore");
        sb.append(String.format("\n  风险评分: %.0f/100  %s\n", s, s >= 50 ? "⚠ 高风险" : s >= 20 ? "⚡ 中风险" : "✓ 低风险"));
        return sb.toString();
    }

    // ==================== 工具 ====================
    private String ej(String json, String key) { Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json); return m.find() ? m.group(1) : ""; }
    private int ji(String json, String key) { Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*(-?\\d+)").matcher(json); return m.find() ? Integer.parseInt(m.group(1)) : 0; }
    private double jd(String json, String key) { Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*(-?[\\d.]+)").matcher(json); return m.find() ? Double.parseDouble(m.group(1)) : 0.0; }
    private String esc(String s) { if (s == null) return ""; return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\t", "\\t"); }
    private String extractData(String json) { int s = json.indexOf("\"data\":\"") + 8, e = json.lastIndexOf("\"}"); return e > s ? json.substring(s, e) : json; }

    // ==================== UI工厂 ====================
    private VBox pageRoot(String t, String sub) { VBox r = new VBox(14); r.setPadding(new Insets(20, 24, 20, 24)); r.getChildren().add(title(t)); if (sub != null) { Label sl = new Label(sub); sl.setFont(Font.font("System", 13)); sl.setTextFill(Color.valueOf("#64748b")); r.getChildren().add(sl); } return r; }
    private ScrollPane wrap(VBox c) { ScrollPane s = new ScrollPane(c); s.setFitToWidth(true); s.setStyle("-fx-background-color: #f1f5f9;"); return s; }
    private Label title(String t) { Label l = new Label(t); l.setFont(Font.font("System", FontWeight.BOLD, 20)); l.setTextFill(Color.valueOf("#1e293b")); return l; }
    private Label titleSm(String t) { Label l = new Label(t); l.setFont(Font.font("System", FontWeight.BOLD, 15)); l.setPadding(new Insets(10, 0, 0, 0)); return l; }
    private Label subtitle(String t) { Label l = new Label(t); l.setFont(Font.font("System", 13)); l.setTextFill(Color.valueOf("#64748b")); return l; }
    private Label lbl(String t) { return lbl(t, 13, "#334155"); }
    private Label lbl(String t, int s, String c) { Label l = new Label(t); l.setFont(Font.font("System", s)); l.setTextFill(Color.valueOf(c)); return l; }
    private TextField tf(String v, int w, String p) { TextField f = new TextField(v); f.setPrefWidth(w); f.setPromptText(p); f.setStyle("-fx-background-radius: 4; -fx-border-radius: 4; -fx-border-color: #d1d5db; -fx-padding: 6 10;"); return f; }
    private ComboBox<String> cb(int w, String p) { ComboBox<String> c = new ComboBox<>(); c.setPrefWidth(w); c.setPromptText(p); c.setStyle("-fx-background-radius: 4; -fx-border-radius: 4;"); return c; }
    private ComboBox<String> cbv(int w, String v, String... items) { ComboBox<String> c = new ComboBox<>(FXCollections.observableArrayList(items)); c.setValue(v); c.setPrefWidth(w); c.setStyle("-fx-background-radius: 4; -fx-border-radius: 4;"); return c; }
    private Button sbtn(String t, String color) { Button b = new Button(t); b.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-background-radius: 4; -fx-padding: 6 14; -fx-font-size: 12px;"); return b; }
    private Button bbtn(String t, String color) { Button b = new Button(t); b.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 10 24; -fx-font-size: 14px; -fx-font-weight: bold;"); return b; }
    private TextArea ta(int rows, String prompt) { TextArea a = new TextArea(); a.setPrefRowCount(rows); a.setPromptText(prompt); a.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 13px; -fx-background-radius: 4; -fx-border-radius: 4; -fx-border-color: #d1d5db;"); return a; }
    private HBox hbox(int sp, javafx.scene.Node... nodes) { HBox b = new HBox(sp); b.getChildren().addAll(nodes); b.setAlignment(Pos.CENTER_LEFT); return b; }
    private SplitPane splitV(javafx.scene.Node top, javafx.scene.Node bottom, double ratio) { SplitPane s = new SplitPane(); s.setOrientation(javafx.geometry.Orientation.VERTICAL); s.getItems().addAll(top, bottom); s.setDividerPosition(0, ratio); return s; }

    @Override public void stop() { if (springContext != null) springContext.close(); Platform.exit(); }
}
