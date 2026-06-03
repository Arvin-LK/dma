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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(DmaDesktopApplication.class);

    private ConfigurableApplicationContext springContext;
    private HttpClient httpClient;
    private Label statusLeft, statusRules, statusApi;
    private StackPane contentArea;
    private Map<String, Button> navButtons = new LinkedHashMap<>();
    private String currentPage = "dashboard";

    // ── 体检页字段 ──
    private TextField dbHost, dbPort, dbUser, dbPassword;
    private ComboBox<String> dbSchema, dbSource, dbTarget;
    private TextArea dbResult;
    private ProgressIndicator dbProgress;
    // ── SQL页字段 ──
    private TextArea sqlInput, sqlResult;
    private ComboBox<String> sqlSource, sqlTarget;
    // ── 存储过程页字段 ──
    private TextArea spInput, spResult;
    private ComboBox<String> spSource, spTarget;
    // ── 项目扫描页字段 ──
    private TextField projPath;
    private ComboBox<String> projSource, projTarget;
    private TextArea projResult;
    private ProgressIndicator projProgress;
    // ── AI页字段 ──
    private TextArea aiInput, aiResult;
    private Label aiStatus;

    public static void main(String[] args) { launch(args); }

    @Override
    public void init() {
        springContext = SpringApplication.run(com.dma.core.DmaCoreApplication.class);
        httpClient = HttpClient.newHttpClient();
    }

    @Override
    public void start(Stage stage) {
        // 设置窗口图标
        try {
            stage.getIcons().add(new javafx.scene.image.Image(
                getClass().getResourceAsStream("/icon.png")));
            stage.getIcons().add(new javafx.scene.image.Image(
                getClass().getResourceAsStream("/icon-32.png")));
        } catch (Exception ignored) {
            // 图标加载失败则使用默认
        }

        stage.setTitle("DMA — Database Migration Assistant");
        stage.setWidth(1280); stage.setHeight(860);
        stage.setMinWidth(1024); stage.setMinHeight(700);

        BorderPane root = new BorderPane();
        root.setLeft(buildSidebar());
        root.setBottom(buildStatusbar());

        contentArea = new StackPane();
        contentArea.setStyle("-fx-background-color: #f0f2f5;");
        contentArea.getChildren().add(buildDashboard());
        root.setCenter(contentArea);

        stage.setScene(new Scene(root));
        stage.show();

        refreshStatus();
    }

    // ═══════════════════════════════════════════════════════════════
    // 侧边栏 — 浅色专业风格
    // ═══════════════════════════════════════════════════════════════
    private VBox buildSidebar() {
        VBox sidebar = new VBox(0);
        sidebar.setPrefWidth(220); sidebar.setMinWidth(220);
        sidebar.setStyle("-fx-background-color: #fafbfc; -fx-border-width: 0 1 0 0; -fx-border-color: #e2e5e9;");

        // Logo 区 — 白色背景 + Logo 图片
        VBox logoBox = new VBox(8);
        logoBox.setPadding(new Insets(20, 20, 18, 20));
        logoBox.setStyle("-fx-background-color: #fafbfc;");
        logoBox.setAlignment(Pos.CENTER);

        // Logo 图片
        javafx.scene.image.ImageView logoImg = new javafx.scene.image.ImageView();
        try {
            javafx.scene.image.Image img = new javafx.scene.image.Image(
                getClass().getResourceAsStream("/icon.png"));
            logoImg.setImage(img);
            logoImg.setFitWidth(64);
            logoImg.setPreserveRatio(true);
            logoImg.setSmooth(true);
        } catch (Exception e) {
            // 图片加载失败，显示文字兜底
            Label fallback = new Label("DMA");
            fallback.setFont(Font.font("System", FontWeight.BOLD, 20));
            fallback.setTextFill(Color.valueOf("#1a73e8"));
            logoBox.getChildren().add(fallback);
        }
        Label logoSub = new Label("Database Migration Assistant");
        logoSub.setFont(Font.font("System", 10));
        logoSub.setTextFill(Color.valueOf("#6b7280"));
        logoSub.setAlignment(Pos.CENTER);
        logoBox.getChildren().addAll(logoImg, logoSub);
        sidebar.getChildren().add(logoBox);

        // 导航分组标签
        Label navLabel = new Label("  功能导航");
        navLabel.setFont(Font.font("System", FontWeight.BOLD, 11));
        navLabel.setTextFill(Color.valueOf("#9ca3af"));
        navLabel.setPadding(new Insets(18, 20, 8, 20));
        sidebar.getChildren().add(navLabel);

        String[][] items = {
            {"🏠", "首页概览", "dashboard"},
            {"🏥", "数据库体检", "scan"},
            {"🔄", "SQL 转换", "sql"},
            {"📦", "存储过程迁移", "procedure"},
            {"📂", "项目源码扫描", "project"},
            {"🤖", "AI 顾问", "ai"}
        };

        for (String[] item : items) {
            Button btn = new Button("  " + item[0] + "   " + item[1]);
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setAlignment(Pos.CENTER_LEFT);
            btn.setPadding(new Insets(10, 20, 10, 20));
            btn.setFont(Font.font("System", 13));
            btn.setTextFill(Color.valueOf("#4b5563"));
            btn.setStyle("-fx-background-color: transparent; -fx-border-width: 0; -fx-cursor: hand;");

            final String page = item[2];
            btn.setOnMouseEntered(e -> {
                if (!page.equals(currentPage))
                    btn.setStyle("-fx-background-color: #e8f0fe; -fx-border-width: 0 0 0 3; -fx-border-color: transparent; -fx-cursor: hand;");
            });
            btn.setOnMouseExited(e -> {
                if (!page.equals(currentPage))
                    btn.setStyle("-fx-background-color: transparent; -fx-border-width: 0; -fx-cursor: hand;");
            });
            btn.setOnAction(e -> switchPage(page));
            navButtons.put(page, btn);
            sidebar.getChildren().add(btn);
        }

        // 弹性空间
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        sidebar.getChildren().add(spacer);

        // 底部版本
        Separator sep = new Separator();
        sep.setStyle("-fx-background: #e2e5e9;");
        sidebar.getChildren().add(sep);
        Label ver = new Label("  v1.0.0 · MVP");
        ver.setFont(Font.font("System", 11));
        ver.setTextFill(Color.valueOf("#9ca3af"));
        ver.setPadding(new Insets(12, 20, 14, 20));
        sidebar.getChildren().add(ver);

        return sidebar;
    }

    private void switchPage(String page) {
        currentPage = page;
        navButtons.forEach((k, btn) -> {
            boolean active = k.equals(page);
            btn.setStyle(active
                ? "-fx-background-color: #e8f0fe; -fx-border-width: 0 0 0 3; -fx-border-color: #1a73e8; -fx-cursor: hand;"
                : "-fx-background-color: transparent; -fx-border-width: 0; -fx-cursor: hand;");
            btn.setTextFill(active ? Color.valueOf("#1a73e8") : Color.valueOf("#4b5563"));
            btn.setFont(Font.font("System", active ? FontWeight.BOLD : FontWeight.NORMAL, 13));
        });
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

    // ═══════════════════════════════════════════════════════════════
    // 状态栏
    // ═══════════════════════════════════════════════════════════════
    private HBox buildStatusbar() {
        HBox bar = new HBox(16);
        bar.setPadding(new Insets(4, 20, 4, 20));
        bar.setMinHeight(28);
        bar.setStyle("-fx-background-color: #f0f2f5; -fx-border-width: 1 0 0 0; -fx-border-color: #e2e5e9;");
        bar.setAlignment(Pos.CENTER_LEFT);
        statusLeft = slbl("就绪", "#6b7280");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        statusApi  = slbl("API: --", "#9ca3af");
        statusRules = slbl("规则: 322", "#9ca3af");
        bar.getChildren().addAll(statusLeft, sp, statusApi, statusRules);
        return bar;
    }

    private void refreshStatus() {
        new Thread(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/api/v1/system/info")).GET().build();
                httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                Platform.runLater(() -> statusApi.setText("🔗 API 8080"));
            } catch (Exception e) { Platform.runLater(() -> statusApi.setText("⚫ 离线")); }
        }).start();
    }

    // ── 报告导出 ──

    // ═══════════════════════════════════════════════════════════════
    // 首页仪表盘
    // ═══════════════════════════════════════════════════════════════
    private ScrollPane buildDashboard() {
        VBox root = new VBox(24);
        root.setPadding(new Insets(30, 32, 30, 32));
        root.setStyle("-fx-background-color: #f0f2f5;");

        // 欢迎区
        Label welcome = new Label("欢迎使用 Database Migration Assistant");
        welcome.setFont(Font.font("System", FontWeight.BOLD, 22));
        welcome.setTextFill(Color.valueOf("#1f2937"));
        Label desc = new Label("数据库迁移与兼容性分析工具 — 支持 MySQL / Oracle / SQLServer → PostgreSQL / GaussDB / 达梦 / OceanBase / GoldenDB");
        desc.setFont(Font.font("System", 13));
        desc.setTextFill(Color.valueOf("#6b7280"));
        desc.setWrapText(true);
        root.getChildren().addAll(welcome, desc);

        // 统计卡片行
        HBox stats = new HBox(14);
        stats.getChildren().addAll(
            statCard("322", "兼容性规则", "#1a73e8"),
            statCard("15", "迁移路径", "#7c3aed"),
            statCard("26/26", "测试通过", "#059669"),
            statCard("3", "报告格式", "#d97706")
        );
        root.getChildren().add(stats);

        // 快捷功能标题
        Label quickTitle = new Label("快捷功能");
        quickTitle.setFont(Font.font("System", FontWeight.BOLD, 15));
        quickTitle.setTextFill(Color.valueOf("#374151"));
        quickTitle.setPadding(new Insets(8, 0, 0, 0));
        root.getChildren().add(quickTitle);

        // 2x2 快捷功能网格
        GridPane grid = new GridPane();
        grid.setHgap(14); grid.setVgap(14);

        grid.add(quickCard("🏥", "数据库体检",
            "连接源数据库，全量扫描存储过程、函数、表、视图，输出兼容率统计", "scan"), 0, 0);
        grid.add(quickCard("🔄", "SQL 转换",
            "粘贴 SQL 语句，自动匹配规则引擎，生成目标数据库兼容语法", "sql"), 1, 0);
        grid.add(quickCard("📦", "存储过程迁移",
            "PROCEDURE / FUNCTION / TRIGGER / VIEW DDL 自动分析与语法转换", "procedure"), 0, 1);
        grid.add(quickCard("📂", "项目源码扫描",
            "扫描 Java / XML / SQL 文件，风险分级统计，支持导出报告", "project"), 1, 1);

        root.getChildren().add(grid);

        ScrollPane sp = new ScrollPane(root);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color: #f0f2f5;");
        return sp;
    }

    private VBox statCard(String value, String label, String accentColor) {
        VBox c = new VBox(8);
        c.setPadding(new Insets(22, 24, 22, 24));
        c.setPrefWidth(220);
        c.setStyle("-fx-background-color: white; -fx-background-radius: 10; "
            + "-fx-border-color: #e2e5e9; -fx-border-radius: 10; -fx-border-width: 1;"
            + "-fx-border-width: 0 0 0 4; -fx-border-color: " + accentColor + " " + accentColor + " " + accentColor + " #e2e5e9;");
        Label v = new Label(value);
        v.setFont(Font.font("System", FontWeight.BOLD, 28));
        v.setTextFill(Color.valueOf(accentColor));
        Label l = new Label(label);
        l.setFont(Font.font("System", 13));
        l.setTextFill(Color.valueOf("#6b7280"));
        c.getChildren().addAll(v, l);
        return c;
    }

    private VBox quickCard(String icon, String title, String desc, String targetPage) {
        VBox c = new VBox(10);
        c.setPadding(new Insets(22, 24, 22, 24));
        c.setStyle("-fx-background-color: white; -fx-background-radius: 10; "
            + "-fx-border-color: #e2e5e9; -fx-border-radius: 10; -fx-border-width: 1; -fx-cursor: hand;");
        c.setPrefWidth(420);

        Label ic = new Label(icon); ic.setFont(Font.font(34));
        Label t = new Label(title); t.setFont(Font.font("System", FontWeight.BOLD, 14)); t.setTextFill(Color.valueOf("#1f2937"));
        Label d = new Label(desc); d.setFont(Font.font("System", 12)); d.setTextFill(Color.valueOf("#6b7280")); d.setWrapText(true);

        c.getChildren().addAll(ic, t, d);
        c.setOnMouseClicked(e -> switchPage(targetPage));
        c.setOnMouseEntered(e -> c.setStyle("-fx-background-color: #f8faff; -fx-background-radius: 10; "
            + "-fx-border-color: #1a73e8; -fx-border-radius: 10; -fx-border-width: 1; -fx-cursor: hand;"));
        c.setOnMouseExited(e -> c.setStyle("-fx-background-color: white; -fx-background-radius: 10; "
            + "-fx-border-color: #e2e5e9; -fx-border-radius: 10; -fx-border-width: 1; -fx-cursor: hand;"));
        return c;
    }

    // ═══════════════════════════════════════════════════════════════
    // 数据库体检页 — 三区布局
    // ═══════════════════════════════════════════════════════════════
    private ScrollPane buildScanPage() {
        VBox root = new VBox(12);
        root.setPadding(new Insets(16, 20, 16, 20));

        // ── Zone 1: 连接配置卡 ──
        VBox connCard = card();
        connCard.getChildren().add(secLabel("数据库连接配置"));

        dbHost = tfld("localhost", 130); dbPort = tfld("3306", 70);
        dbUser = tfld("root", 100);
        dbPassword = new PasswordField(); dbPassword.setPrefWidth(100); dbPassword.setPromptText("密码");
        dbPassword.setStyle(fieldStyle());
        dbSchema = cb("先获取 Schema", 160);
        dbSource = cbv("MYSQL", 130, "MYSQL", "ORACLE", "SQLSERVER");
        dbTarget = cbv("POSTGRESQL", 140, "POSTGRESQL", "DAMENG", "GAUSSDB", "OCEANBASE", "GOLDENDB");
        Button connectBtn = sbtn("🔗 获取 Schema"); connectBtn.setOnAction(e -> discoverSchemas());

        HBox connRow1 = hbox(8, lbl("主机:"), dbHost, lbl("端口:"), dbPort, lbl("用户:"), dbUser,
            lbl("密码:"), dbPassword, connectBtn, lbl("Schema:"), dbSchema);
        connCard.getChildren().add(connRow1);

        // ── Zone 2: 操作区 ──
        dbProgress = new ProgressIndicator(-1); dbProgress.setVisible(false); dbProgress.setPrefSize(28, 28);
        Button scanBtn = pbtn("🔍 开始扫描"); scanBtn.setOnAction(e -> runDbScan());
        Button exportBtn = sbtn("📄 导出报告");
        exportBtn.setOnAction(e -> exportReport("数据库体检报告", dbSource.getValue() + " → " + dbTarget.getValue(), dbResult.getText()));

        HBox actionRow = hbox(12, lbl("源数据库:"), dbSource, lbl("目标数据库:"), dbTarget, scanBtn, exportBtn, dbProgress);

        // ── Zone 3: 结果区（自动填满剩余空间）──
        dbResult = new TextArea();
        dbResult.setPromptText("扫描结果将在此显示 — 包含对象统计、兼容率及详细问题列表...");
        dbResult.setEditable(false);
        dbResult.setStyle("-fx-font-family: 'Consolas', 'Monaco', monospace; -fx-font-size: 13px; "
            + "-fx-background-color: #f8f9fb; -fx-background-radius: 8; "
            + "-fx-border-color: #c7d2fe; -fx-border-radius: 8; -fx-border-width: 1 0 0 0; "
            + "-fx-padding: 12; -fx-control-inner-background: #f8f9fb;");

        root.getChildren().addAll(connCard, actionRow, dbResult);
        VBox.setVgrow(dbResult, Priority.ALWAYS);

        ScrollPane sp = new ScrollPane(root);
        sp.setFitToWidth(true);
        sp.setFitToHeight(true);
        sp.setStyle("-fx-background-color: #f0f2f5;");
        return sp;
    }

    // ═══════════════════════════════════════════════════════════════
    // SQL 转换页 — 三区布局
    // ═══════════════════════════════════════════════════════════════
    private ScrollPane buildSqlPage() {
        VBox root = pageContainer();

        // ── Zone 1: 配置卡 ──
        VBox cfgCard = card();
        cfgCard.getChildren().add(secLabel("SQL 转换配置"));
        sqlSource = cbv("MYSQL", 130, "MYSQL", "ORACLE", "SQLSERVER");
        sqlTarget = cbv("POSTGRESQL", 140, "POSTGRESQL", "DAMENG", "GAUSSDB", "OCEANBASE", "GOLDENDB");
        Button scanBtn = pbtn("🔍 分析并转换");
        scanBtn.setOnAction(e -> runSqlConvert());
        Button exportBtn = sbtn("📄 导出");
        exportBtn.setOnAction(e -> exportReport("SQL 转换报告", sqlSource.getValue() + " → " + sqlTarget.getValue(), sqlResult.getText()));
        HBox cfgRow = hbox(12, lbl("源数据库:"), sqlSource, lbl("目标数据库:"), sqlTarget, scanBtn, exportBtn);
        cfgCard.getChildren().add(cfgRow);

        // ── Zone 2: SQL 输入区 ──
        sqlInput = editorArea("在此输入 SQL 语句...\n\n示例:\nSELECT IFNULL(name, '') FROM users LIMIT 0, 10;");

        // ── Zone 3: 结果输出区 ──
        sqlResult = resultArea("分析结果将显示在此 — 包含规则匹配详情、原始 SQL 与转换后 SQL 对比...");
        sqlResult.setEditable(true); // 允许复制

        root.getChildren().addAll(cfgCard, sqlInput, sqlResult);
        VBox.setVgrow(sqlResult, Priority.ALWAYS);
        return wrap(root);
    }

    // ═══════════════════════════════════════════════════════════════
    // 存储过程迁移页 — 三区布局
    // ═══════════════════════════════════════════════════════════════
    private ScrollPane buildProcedurePage() {
        VBox root = pageContainer();

        // ── Zone 1: 配置卡 ──
        VBox cfgCard = card();
        cfgCard.getChildren().add(secLabel("存储过程迁移配置"));
        spSource = cbv("MYSQL", 130, "MYSQL", "ORACLE", "SQLSERVER");
        spTarget = cbv("GAUSSDB", 140, "GAUSSDB", "POSTGRESQL", "DAMENG", "OCEANBASE", "GOLDENDB");
        Button convBtn = pbtn("🔄 执行转换");
        convBtn.setOnAction(e -> runProcedureConvert());
        HBox cfgRow = hbox(12, lbl("源数据库:"), spSource, lbl("目标数据库:"), spTarget, convBtn);
        cfgCard.getChildren().add(cfgRow);

        // ── Zone 2: DDL 输入区 ──
        spInput = editorArea("粘贴 CREATE PROCEDURE / FUNCTION / TRIGGER / VIEW 的 DDL 语句...");

        // ── Zone 3: 结果输出区 ──
        spResult = resultArea("转换结果将显示在此 — 包含原始 DDL、转换后 DDL 及变更清单...");

        root.getChildren().addAll(cfgCard, spInput, spResult);
        VBox.setVgrow(spResult, Priority.ALWAYS);
        return wrap(root);
    }

    // ═══════════════════════════════════════════════════════════════
    // 项目源码扫描页 — 三区布局
    // ═══════════════════════════════════════════════════════════════
    private ScrollPane buildProjectPage() {
        VBox root = pageContainer();

        // ── Zone 1: 配置卡 ──
        VBox cfgCard = card();
        cfgCard.getChildren().add(secLabel("项目扫描配置"));
        projPath = tfld(System.getProperty("user.dir"), 380);
        projSource = cbv("MYSQL", 120, "MYSQL", "ORACLE", "SQLSERVER");
        projTarget = cbv("POSTGRESQL", 130, "POSTGRESQL", "DAMENG", "GAUSSDB", "OCEANBASE", "GOLDENDB");
        projProgress = new ProgressIndicator(-1); projProgress.setVisible(false); projProgress.setPrefSize(28, 28);
        Button scanBtn = pbtn("🔍 开始扫描"); scanBtn.setOnAction(e -> runProjectScan());
        Button exportBtn = sbtn("📄 导出");
        exportBtn.setOnAction(e -> exportReport("项目源码扫描报告", projSource.getValue() + " → " + projTarget.getValue(), projResult.getText()));
        HBox cfgRow1 = hbox(10, lbl("项目路径:"), projPath);
        HBox cfgRow2 = hbox(12, lbl("源数据库:"), projSource, lbl("目标数据库:"), projTarget, scanBtn, exportBtn, projProgress);
        cfgCard.getChildren().addAll(cfgRow1, cfgRow2);

        // ── Zone 2: 结果输出区 ──
        projResult = resultArea("扫描结果将显示在此 — 包含文件统计、风险分级及评分详情...");

        root.getChildren().addAll(cfgCard, projResult);
        VBox.setVgrow(projResult, Priority.ALWAYS);
        return wrap(root);
    }

    // ═══════════════════════════════════════════════════════════════
    // AI 顾问页 — 三区布局
    // ═══════════════════════════════════════════════════════════════
    private ScrollPane buildAiPage() {
        VBox root = pageContainer();

        // ── Zone 1: 状态配置卡 ──
        VBox cfgCard = card();
        cfgCard.getChildren().add(secLabel("AI 服务状态"));
        aiStatus = lbl("检查 AI 服务连接状态..."); aiStatus.setFont(Font.font("System", 13));
        Button checkBtn = sbtn("🔄 检查连接"); checkBtn.setOnAction(e -> checkAiStatus());
        Button askBtn = pbtn("💡 咨询 AI"); askBtn.setOnAction(e -> askAi());
        HBox cfgRow = hbox(12, aiStatus, checkBtn, askBtn);
        cfgCard.getChildren().add(cfgRow);

        // ── Zone 2: 问题输入区 ──
        aiInput = editorArea("粘贴 SQL 或描述数据库迁移问题...\n\n支持: Ollama 本地模型 / OpenAI / 通义千问 / DeepSeek\n配置: application.yml → dma.ai.provider");

        // ── Zone 3: AI 回复区 ──
        aiResult = resultArea("AI 回复将显示在此...");
        aiResult.setStyle(resultStyle());

        root.getChildren().addAll(cfgCard, aiInput, aiResult);
        VBox.setVgrow(aiResult, Priority.ALWAYS);
        return wrap(root);
    }

    // ═══════════════════════════════════════════════════════════════
    // 业务逻辑（与旧版完全相同，保持不变）
    // ═══════════════════════════════════════════════════════════════

    // ── Schema 发现 ──
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
                    statusLeft.setText("发现 " + schemas.size() + " 个 Schema");
                });
            } catch (Exception e) { Platform.runLater(() -> statusLeft.setText("连接失败: " + e.getMessage())); }
        }).start();
    }

    // ── 报告导出 ──

    // ── 数据库体检 ──
    private void runDbScan() {
        dbProgress.setVisible(true); statusLeft.setText("正在扫描数据库...");
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
            } catch (Exception e) { log.error("Database scan failed", e); Platform.runLater(() -> { dbProgress.setVisible(false); statusLeft.setText("扫描失败: " + e.getMessage()); }); }
        }).start();
    }

    // ── 报告导出 ──

    // ── SQL 转换 ──
    private void runSqlConvert() {
        String sql = sqlInput.getText().trim(); if (sql.isEmpty()) return;
        statusLeft.setText("正在分析转换...");
        new Thread(() -> {
            try {
                String body = String.format("""
{"sql":"%s","sourceDbType":"%s","targetDbType":"%s"}""",
                        esc(sql), esc(sqlSource.getValue()), esc(sqlTarget.getValue()));
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/api/v1/scan/sql"))
                        .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
                String resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString()).body();
                Platform.runLater(() -> { statusLeft.setText("转换完成"); sqlResult.setText(formatSql(resp, sql)); });
            } catch (Exception e) { Platform.runLater(() -> statusLeft.setText("转换失败: " + e.getMessage())); }
        }).start();
    }

    // ── 报告导出 ──

    // ── 存储过程转换 ──
    private void runProcedureConvert() {
        String ddl = spInput.getText().trim(); if (ddl.isEmpty()) return;
        statusLeft.setText("正在转换存储过程...");
        new Thread(() -> {
            try {
                String body = String.format("""
{"ddl":"%s","sourceDbType":"%s","targetDbType":"%s"}""",
                        esc(ddl), esc(spSource.getValue()), esc(spTarget.getValue()));
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/api/v1/convert/procedure"))
                        .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
                String resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString()).body();
                Platform.runLater(() -> { statusLeft.setText("转换完成"); spResult.setText(formatSp(resp)); });
            } catch (Exception e) { Platform.runLater(() -> statusLeft.setText("转换失败: " + e.getMessage())); }
        }).start();
    }

    // ── 报告导出 ──

    // ── 项目扫描 ──
    private void runProjectScan() {
        projProgress.setVisible(true); statusLeft.setText("正在扫描项目...");
        new Thread(() -> {
            try {
                String body = String.format("""
{"projectPath":"%s","sourceDbType":"%s","targetDbType":"%s"}""",
                        esc(projPath.getText()), esc(projSource.getValue()), esc(projTarget.getValue()));
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/api/v1/scan/project-full"))
                        .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
                String resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString()).body();
                Platform.runLater(() -> { projProgress.setVisible(false); statusLeft.setText("扫描完成"); projResult.setText(formatProj(resp)); });
            } catch (Exception e) { Platform.runLater(() -> { projProgress.setVisible(false); statusLeft.setText("扫描失败: " + e.getMessage()); }); }
        }).start();
    }

    // ── 报告导出 ──

    // ── AI ──
    private void checkAiStatus() {
        new Thread(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/api/v1/ai/status")).GET().build();
                String resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString()).body();
                Platform.runLater(() -> {
                    if (resp.contains("\"available\":true")) { aiStatus.setText("🤖 AI 已连接"); aiStatus.setTextFill(Color.valueOf("#059669")); }
                    else { aiStatus.setText("⚠ AI 未启用（检查 application.yml 配置）"); aiStatus.setTextFill(Color.valueOf("#d97706")); }
                });
            } catch (Exception e) { Platform.runLater(() -> { aiStatus.setText("✗ AI 不可用"); aiStatus.setTextFill(Color.valueOf("#dc2626")); }); }
        }).start();
    }

    // ── 报告导出 ──

    private void askAi() {
        String input = aiInput.getText().trim(); if (input.isEmpty()) return;
        statusLeft.setText("AI 分析中...");
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
            } catch (Exception e) { Platform.runLater(() -> statusLeft.setText("AI 请求失败: " + e.getMessage())); }
        }).start();
    }

    // ── 报告导出 ──

    // ── 报告导出 ──
    private void exportReport(String title, String subtitle, String content) {
        if (content == null || content.isBlank()) return;
        showExportDialog(title, subtitle, content);
    }

    /** DBeaver 风格导出对话框 */
    private void showExportDialog(String title, String subtitle, String content) {
        Stage dialog = new Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setTitle("导出报告");

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: white; -fx-background-radius: 10;");

        // 标题栏
        HBox titleBar = new HBox();
        titleBar.setPadding(new Insets(20, 24, 16, 24));
        titleBar.setStyle("-fx-background-color: #fafbfc; -fx-background-radius: 10 10 0 0; "
            + "-fx-border-color: #e5e7eb; -fx-border-width: 0 0 1 0;");
        Label titleLbl = new Label("选择导出格式");
        titleLbl.setFont(Font.font("System", FontWeight.BOLD, 16));
        titleLbl.setTextFill(Color.valueOf("#1f2937"));
        titleBar.getChildren().add(titleLbl);

        // 格式选项区
        VBox optionsBox = new VBox(6);
        optionsBox.setPadding(new Insets(20, 24, 20, 24));

        // 三个格式选项的数据
        String[][] formats = {
            {"📄", "HTML", "浏览器查看", "适合在浏览器中查看，支持完整样式和排版", "#2563eb"},
            {"📕", "PDF", "便携文档", "固定版式，适合存档和分发，不可修改", "#dc2626"},
            {"📝", "Word", "可编辑文档", "适合需要进一步编辑的迁移方案文档", "#059669"}
        };

        final String[] selectedFmt = {"HTML"};

        for (String[] f : formats) {
            HBox row = new HBox(14);
            row.setPadding(new Insets(14, 16, 14, 16));
            row.setStyle("-fx-background-color: white; -fx-background-radius: 8; "
                + "-fx-border-color: #e5e7eb; -fx-border-radius: 8; -fx-border-width: 1; -fx-cursor: hand;");
            row.setAlignment(Pos.CENTER_LEFT);

            Label iconLbl = new Label(f[0]);
            iconLbl.setFont(Font.font(28));

            VBox textBox = new VBox(4);
            Label nameLbl = new Label(f[1] + " — " + f[2]);
            nameLbl.setFont(Font.font("System", FontWeight.BOLD, 14));
            nameLbl.setTextFill(Color.valueOf("#1f2937"));
            Label descLbl = new Label(f[3]);
            descLbl.setFont(Font.font("System", 12));
            descLbl.setTextFill(Color.valueOf("#6b7280"));
            textBox.getChildren().addAll(nameLbl, descLbl);

            Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);

            // Selection indicator
            Label checkLbl = new Label("○");
            checkLbl.setFont(Font.font(20));
            checkLbl.setTextFill(Color.valueOf("#9ca3af"));

            row.getChildren().addAll(iconLbl, textBox, spacer, checkLbl);
            final String fmt = f[1];

            row.setOnMouseClicked(e -> {
                // Update all rows
                for (javafx.scene.Node n : optionsBox.getChildren()) {
                    if (n instanceof HBox r) {
                        r.setStyle("-fx-background-color: white; -fx-background-radius: 8; "
                            + "-fx-border-color: #e5e7eb; -fx-border-radius: 8; -fx-border-width: 1; -fx-cursor: hand;");
                        // Reset checkmark
                        for (javafx.scene.Node cn : r.getChildren()) {
                            if (cn instanceof Label cl && (cl.getText().equals("●") || cl.getText().equals("○"))) {
                                cl.setText("○"); cl.setTextFill(Color.valueOf("#9ca3af"));
                            }
                        }
                    }
                }
                row.setStyle("-fx-background-color: #eff6ff; -fx-background-radius: 8; "
                    + "-fx-border-color: #3b82f6; -fx-border-radius: 8; -fx-border-width: 2; -fx-cursor: hand;");
                checkLbl.setText("●");
                checkLbl.setTextFill(Color.valueOf("#2563eb"));
                selectedFmt[0] = fmt;
            });

            // Default selection (HTML)
            if (fmt.equals("HTML")) {
                row.setStyle("-fx-background-color: #eff6ff; -fx-background-radius: 8; "
                    + "-fx-border-color: #3b82f6; -fx-border-radius: 8; -fx-border-width: 2; -fx-cursor: hand;");
                checkLbl.setText("●");
                checkLbl.setTextFill(Color.valueOf("#2563eb"));
            }

            row.setOnMouseEntered(e -> {
                if (!selectedFmt[0].equals(fmt)) {
                    row.setStyle("-fx-background-color: #f9fafb; -fx-background-radius: 8; "
                        + "-fx-border-color: #d1d5db; -fx-border-radius: 8; -fx-border-width: 1; -fx-cursor: hand;");
                }
            });
            row.setOnMouseExited(e -> {
                if (!selectedFmt[0].equals(fmt)) {
                    row.setStyle("-fx-background-color: white; -fx-background-radius: 8; "
                        + "-fx-border-color: #e5e7eb; -fx-border-radius: 8; -fx-border-width: 1; -fx-cursor: hand;");
                }
            });

            optionsBox.getChildren().add(row);
        }

        // 按钮栏
        HBox btnBar = new HBox(10);
        btnBar.setPadding(new Insets(16, 24, 20, 24));
        btnBar.setAlignment(Pos.CENTER_RIGHT);
        btnBar.setStyle("-fx-background-color: #fafbfc; -fx-background-radius: 0 0 10 10; "
            + "-fx-border-color: #e5e7eb; -fx-border-width: 1 0 0 0;");

        Button cancelBtn = new Button("取消");
        cancelBtn.setStyle("-fx-background-color: white; -fx-text-fill: #374151; "
            + "-fx-background-radius: 6; -fx-border-color: #d1d5db; -fx-border-radius: 6; "
            + "-fx-border-width: 1; -fx-padding: 9 22; -fx-font-size: 13px; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> dialog.close());

        Button okBtn = new Button("导出");
        okBtn.setStyle("-fx-background-color: #1a73e8; -fx-text-fill: white; "
            + "-fx-background-radius: 6; -fx-padding: 9 28; -fx-font-size: 13px; "
            + "-fx-font-weight: bold; -fx-cursor: hand;");
        okBtn.setOnAction(e -> {
            dialog.close();
            String fmt = selectedFmt[0];
            String ext = "PDF".equals(fmt) ? ".pdf" : "Word".equals(fmt) ? ".docx" : ".html";
            doExport(title, subtitle, content, fmt, ext);
        });

        btnBar.getChildren().addAll(cancelBtn, okBtn);
        root.getChildren().addAll(titleBar, optionsBox, btnBar);

        Scene scene = new Scene(root, 460, 440);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void doExport(String title, String subtitle, String content, String fmt, String ext) {
        new Thread(() -> {
            try {
                String body = String.format("""
{"title":"%s","subtitle":"%s","content":"%s","format":"%s"}""",
                        esc(title), esc(subtitle), esc(content), fmt);
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/api/v1/report/export"))
                        .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
                byte[] data = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray()).body();
                String desktop = System.getProperty("user.home") + "\\Desktop";
                String fn = "DMA_Report_" + java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ext;
                java.nio.file.Path fp = java.nio.file.Path.of(desktop, fn);
                java.nio.file.Files.write(fp, data);
                new Thread(() -> { try { java.awt.Desktop.getDesktop().open(fp.toFile()); } catch (Exception ignored) {} }).start();
                Platform.runLater(() -> statusLeft.setText("已导出: " + fn));
            } catch (Exception e) { log.error("Export failed", e); Platform.runLater(() -> statusLeft.setText("导出失败: " + e.getMessage())); }
        }).start();
    }

    // ── 报告导出 ──

    // ═══════════════════════════════════════════════════════════════
    // 格式化方法
    // ═══════════════════════════════════════════════════════════════
    private String formatScan(String json) {
        String b = json.contains("\"data\":") ? json.substring(json.indexOf("\"data\":") + 7) : json;
        StringBuilder sb = new StringBuilder();

        // ── 报告标题 ──
        int stored = ji(b, "storedProcedureCount"), funcs = ji(b, "functionCount");
        int tables = ji(b, "tableCount"), views = ji(b, "viewCount");
        int total = stored + funcs + tables + views;
        int compat = ji(b, "compatibleCount"), auto = ji(b, "autoConvertibleCount");
        int manual = ji(b, "manualReviewCount"), incompat = ji(b, "incompatibleCount");
        double rate = jd(b, "compatibilityRate");

        sb.append("  DMA 数据库兼容性体检报告\n\n");
        sb.append(String.format("  数据库: %s    迁移方向: %s → %s\n\n",
            ej(b, "databaseName"), ej(b, "sourceDbType"), ej(b, "targetDbType")));

        // ── 统计概要 ──
        String barColor;
        if (rate >= 90) barColor = "green";
        else if (rate >= 70) barColor = "yellow";
        else barColor = "red";
        String rateIcon = rate >= 90 ? "🟢" : rate >= 70 ? "🟡" : "🔴";

        sb.append("  ┌──────────────────────────────────────────────────┐\n");
        sb.append(String.format("  │  %s 兼容率: %.1f%%   对象总数: %d                     │\n", rateIcon, rate, total));
        sb.append(String.format("  │  ✓ 兼容: %d  ⚡ 可转换: %d  ⚠ 需审核: %d  ✗ 不兼容: %d   │\n", compat, auto, manual, incompat));
        sb.append(String.format("  │  存储过程: %d  函数: %d  表: %d  视图: %d              │\n", stored, funcs, tables, views));
        sb.append("  └──────────────────────────────────────────────────┘\n\n");

        // ── 详细对象分析 ──
        int objStart = b.indexOf("\"objects\":[");
        if (objStart < 0) {
            sb.append("  (无详细对象数据)\n");
            return sb.toString();
        }

        String objsSection = b.substring(objStart + 11);
        List<String> objJsons = new ArrayList<>();
        int depth = 0, start = -1;
        for (int i = 0; i < objsSection.length(); i++) {
            char c = objsSection.charAt(i);
            if (c == '{') { if (depth == 0) start = i; depth++; }
            else if (c == '}') { depth--; if (depth == 0 && start >= 0) { objJsons.add(objsSection.substring(start, i + 1)); start = -1; } }
            else if (c == ']' && depth == 0) break;
        }

        // Separate problematic from compatible
        List<String> problems = new ArrayList<>();
        List<String> ok = new ArrayList<>();
        for (String oj : objJsons) {
            String level = ej(oj, "compatibilityLevel");
            if ("COMPATIBLE".equals(level)) ok.add(oj);
            else problems.add(oj);
        }

        // ── Show problematic objects with full detail ──
        if (!problems.isEmpty()) {
            sb.append("  ▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀\n");
            sb.append(String.format("   发现问题对象: %d 个\n", problems.size()));
            sb.append("  ▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀\n\n");

            int no = 0;
            for (String oj : problems) {
                no++;
                String name = ej(oj, "objectName");
                String type = ej(oj, "objectType");
                String level = ej(oj, "compatibilityLevel");
                String rule = ej(oj, "ruleCode");
                String desc = ej(oj, "description");
                String sugDdl = ej(oj, "suggestedDdl");
                String ddl = ej(oj, "ddl");

                String typeEmoji = switch (type) {
                    case "TABLE" -> "📋"; case "VIEW" -> "👁"; case "PROCEDURE" -> "📦";
                    case "FUNCTION" -> "ƒ"; case "TRIGGER" -> "⚡"; default -> "📄";
                };
                String sevIcon = switch (level) {
                    case "MANUAL_REVIEW" -> "⚠️";
                    case "INCOMPATIBLE", "PARSE_ERROR" -> "🔴";
                    default -> "🟡";
                };
                String sevLabel = switch (level) {
                    case "AUTO_CONVERTIBLE" -> "可自动转换";
                    case "MANUAL_REVIEW" -> "需人工审核";
                    case "INCOMPATIBLE" -> "不兼容";
                    case "PARSE_ERROR" -> "解析错误";
                    default -> level;
                };

                // ── Object header with separator ──
                sb.append(String.format("  ═══ %d. %s %s  %s [%s] ═══\n\n",
                    no, typeEmoji, name, sevIcon, sevLabel));

                // ── Original DDL (full content, no truncation) ──
                if (!ddl.isEmpty()) {
                    sb.append("  ── 原始 DDL ────────────────────────────────\n");
                    String cleanDdl = ddl.replace("\\n", "\n").replace("\\t", "    ");
                    for (String line : cleanDdl.split("\n")) {
                        sb.append("  ").append(line).append("\n");
                    }
                    sb.append("  ────────────────────────────────────────────\n\n");
                }

                // ── Issues ──
                int issStartU = oj.indexOf("\"issues\":[");
                if (issStartU >= 0) {
                    String issPart = oj.substring(issStartU + 10);
                    Matcher im = Pattern.compile("\"([^\"]+)\"").matcher(issPart);
                    int issueNum = 0;
                    boolean first = true;
                    while (im.find()) {
                        String issue = im.group(1);
                        if (issue.length() > 2 && !issue.equals("issues")) {
                            if (first) {
                                sb.append("  ── 发现的问题 ──────────────────────────────\n");
                                first = false;
                            }
                            issueNum++;
                            sb.append(String.format("    %d. %s\n", issueNum, issue));
                        }
                    }
                    if (!first) sb.append("\n");
                }

                // ── Rule reference ──
                if (!rule.isEmpty()) {
                    sb.append(String.format("    匹配规则: [%s] %s\n\n", rule, desc));
                }

                // ── Suggested fix (full DDL, real solution) ──
                if (!sugDdl.isEmpty() && !sugDdl.equals(ddl)) {
                    sb.append("  ── 建议修改方案 ────────────────────────────\n");
                    String cleanSug = sugDdl.replace("\\n", "\n").replace("\\t", "    ");
                    for (String line : cleanSug.split("\n")) {
                        sb.append("  ").append(line).append("\n");
                    }
                    sb.append("  ────────────────────────────────────────────\n\n");
                }

                // ── Action ──
                String action = switch (level) {
                    case "AUTO_CONVERTIBLE" -> "  ▶ 处理方式: 已生成完整转换 DDL（见上方建议方案），可直接替换原对象";
                    case "MANUAL_REVIEW" -> "  ▶ 处理方式: 上方问题需逐一修改后重新验证；也可使用「AI 顾问」自动分析";
                    case "INCOMPATIBLE" -> "  ▶ 处理方式: 无法自动转换，建议使用「AI 顾问」生成目标库语法，或人工重写";
                    case "PARSE_ERROR" -> "  ▶ 处理方式: DDL 解析失败，请检查语法后重试，或使用「AI 顾问」辅助分析";
                    default -> "";
                };
                sb.append(action).append("\n\n");
            }
        }

        // ── Summary of compatible objects ──
        if (!ok.isEmpty()) {
            sb.append("  ▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀\n");
            sb.append(String.format("   完全兼容对象: %d 个 (无需处理)\n", ok.size()));
            sb.append("  ▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀\n\n");
            // List compatible objects compactly
            StringBuilder compatList = new StringBuilder("  ");
            int col = 0;
            for (String oj : ok) {
                String name = ej(oj, "objectName");
                String type = ej(oj, "objectType");
                String typeShort = switch (type) {
                    case "TABLE" -> "T"; case "VIEW" -> "V"; case "PROCEDURE" -> "P";
                    case "FUNCTION" -> "F"; default -> "?";
                };
                String item = String.format("✓%s:%s", typeShort, name);
                if (col + item.length() > 70) { compatList.append("\n  "); col = 0; }
                compatList.append(item).append("  ");
                col += item.length() + 2;
            }
            sb.append(compatList.toString().trim()).append("\n\n");
        }

        // ── Footer ──
        sb.append("  ──────────────────────────────────────────────────\n");
        sb.append(String.format("  总计: %d 对象 | ✓%d ⚡%d ⚠%d ✗%d | 兼容率 %.1f%%\n",
            total, compat, auto, manual, incompat, rate));
        sb.append("  ──────────────────────────────────────────────────\n");
        return sb.toString();
    }

    private String formatSql(String json, String orig) {
        String b = json.contains("\"data\":") ? json.substring(json.indexOf("\"data\":") + 7) : json;
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("══════════════════════════════════════\n  %s → %s  转换分析\n══════════════════════════════════════\n\n",
            sqlSource.getValue(), sqlTarget.getValue()));
        int n = 0;
        Matcher m = Pattern.compile("\"ruleCode\":\"([^\"]+)\".*?\"severity\":\"([^\"]+)\".*?\"message\":\"([^\"]+)\"").matcher(b);
        while (m.find()) {
            n++; String sev = m.group(2);
            sb.append(String.format("[%s] %s — %s\n── 原始 SQL ──\n  %s\n",
                "ERROR".equals(sev) ? "✗" : "WARNING".equals(sev) ? "⚠" : "ℹ", m.group(1), m.group(3), orig));
            Matcher sm = Pattern.compile("\"suggestedSql\":\"([^\"]+)\"").matcher(b);
            if (sm.find()) sb.append("── 转换后 SQL ──\n  ").append(sm.group(1).replace("\\\"", "\"")).append("\n");
            sb.append("\n");
        }
        sb.append(String.format("══════════════════════════════════════\n  共发现 %d 个问题\n", n));
        return sb.toString();
    }

    private String formatSp(String json) {
        String b = json.contains("\"data\":") ? json.substring(json.indexOf("\"data\":") + 6) : json;
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("══════════════════════════════════════\n  %s 迁移: %s → %s\n══════════════════════════════════════\n\n",
            "PROCEDURE".equals(ej(b, "objectType")) ? "存储过程" : ej(b, "objectType"), spSource.getValue(), spTarget.getValue()));
        sb.append("── 原始 DDL ──\n").append(ej(b, "originalDdl").replace("\\n", "\n").replace("\\t", "    ")).append("\n\n");
        sb.append("── 转换后 DDL ──\n").append(ej(b, "convertedDdl").replace("\\n", "\n").replace("\\t", "    ")).append("\n\n");
        int c = ji(b, "changeCount");
        if (c > 0) {
            sb.append("── 变更清单 (").append(c).append("项) ──\n");
            Matcher cm = Pattern.compile("\"changes\":\\[(.*?)\\]").matcher(b);
            if (cm.find()) {
                int x = 1;
                Matcher im = Pattern.compile("\"([^\"]+)\"").matcher(cm.group(1));
                while (im.find()) { String ch = im.group(1); if (ch.length() > 2) sb.append(x++).append(". ").append(ch).append("\n"); }
            }
        }
        return sb.toString();
    }

    private String formatProj(String json) {
        String b = json.contains("\"data\":") ? json.substring(json.indexOf("\"data\":") + 6) : json;
        StringBuilder sb = new StringBuilder();
        sb.append("══════════════════════════════════════\n");
        sb.append("  DMA 项目源码扫描报告\n");
        sb.append("══════════════════════════════════════\n\n");
        sb.append(String.format("  路径: %s\n  迁移: %s → %s\n\n", ej(b, "projectPath"), ej(b, "sourceDbType"), ej(b, "targetDbType")));
        sb.append(String.format("  文件总数: %d\n  Java: %d  XML: %d  SQL: %d\n\n",
            ji(b, "totalFiles"), ji(b, "javaFiles"), ji(b, "xmlFiles"), ji(b, "sqlFiles")));
        sb.append(String.format("  ✗ 高风险: %d  ⚠ 中风险: %d  ℹ 低风险: %d\n",
            ji(b, "highRisk"), ji(b, "mediumRisk"), ji(b, "lowRisk")));
        double s = jd(b, "riskScore");
        sb.append(String.format("\n  风险评分: %.0f / 100  %s\n", s, s >= 50 ? "⚠ 高风险" : s >= 20 ? "⚡ 中风险" : "✓ 低风险"));
        return sb.toString();
    }

    // ═══════════════════════════════════════════════════════════════
    // JSON 提取工具
    // ═══════════════════════════════════════════════════════════════
    private String ej(String json, String key) { Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json); return m.find() ? m.group(1) : ""; }
    private int ji(String json, String key) { Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*(-?\\d+)").matcher(json); return m.find() ? Integer.parseInt(m.group(1)) : 0; }
    private double jd(String json, String key) { Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*(-?[\\d.]+)").matcher(json); return m.find() ? Double.parseDouble(m.group(1)) : 0.0; }
    private String esc(String s) { if (s == null) return ""; return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\t", "\\t"); }
    private String extractData(String json) { int s = json.indexOf("\"data\":\"") + 8, e = json.lastIndexOf("\"}"); return e > s ? json.substring(s, e) : json; }

    // ═══════════════════════════════════════════════════════════════
    // UI 工厂方法（DBeaver 风格）
    // ═══════════════════════════════════════════════════════════════

    // ── 页面容器 ──
    private VBox pageContainer() {
        VBox r = new VBox(14);
        r.setPadding(new Insets(20, 24, 20, 24));
        return r;
    }
    private ScrollPane wrap(VBox c) { ScrollPane s = new ScrollPane(c); s.setFitToWidth(true); s.setFitToHeight(true); s.setStyle("-fx-background-color: #f0f2f5;"); return s; }

    // ── 白色卡片 ──
    private VBox card() {
        VBox c = new VBox(10);
        c.setPadding(new Insets(16, 20, 16, 20));
        c.setStyle("-fx-background-color: white; -fx-background-radius: 8; "
            + "-fx-border-color: #e2e5e9; -fx-border-radius: 8; -fx-border-width: 1;");
        return c;
    }

    // ── 分区标题 ──
    private Label secLabel(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("System", FontWeight.BOLD, 13));
        l.setTextFill(Color.valueOf("#374151"));
        l.setPadding(new Insets(0, 0, 8, 0));
        l.setStyle("-fx-border-color: #e5e7eb; -fx-border-width: 0 0 1 0;");
        l.setMaxWidth(Double.MAX_VALUE);
        return l;
    }

    // ── Label ──
    private Label lbl(String t) { Label l = new Label(t); l.setFont(Font.font("System", 13)); l.setTextFill(Color.valueOf("#4b5563")); return l; }
    private Label slbl(String t, String color) { Label l = new Label(t); l.setFont(Font.font("System", 11)); l.setTextFill(Color.valueOf(color)); return l; }

    // ── TextField ──
    private TextField tfld(String value, int width) {
        TextField f = new TextField(value);
        f.setPrefWidth(width);
        f.setStyle(fieldStyle());
        return f;
    }
    private String fieldStyle() {
        return "-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #d1d5db; "
            + "-fx-border-width: 1; -fx-padding: 7 10; -fx-font-size: 13px;";
    }

    // ── ComboBox ──
    private ComboBox<String> cb(String prompt, int width) {
        ComboBox<String> c = new ComboBox<>();
        c.setPrefWidth(width); c.setPromptText(prompt);
        c.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #d1d5db; -fx-font-size: 13px;");
        return c;
    }
    private ComboBox<String> cbv(String value, int width, String... items) {
        ComboBox<String> c = new ComboBox<>(FXCollections.observableArrayList(items));
        c.setValue(value); c.setPrefWidth(width);
        c.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #d1d5db; -fx-font-size: 13px;");
        return c;
    }

    // ── Button ──
    private Button pbtn(String text) { // 主按钮（蓝色）
        Button b = new Button(text);
        b.setStyle("-fx-background-color: #1a73e8; -fx-text-fill: white; -fx-background-radius: 6; "
            + "-fx-padding: 9 22; -fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand;");
        b.setOnMouseEntered(e -> b.setStyle("-fx-background-color: #1557b0; -fx-text-fill: white; "
            + "-fx-background-radius: 6; -fx-padding: 9 22; -fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand;"));
        b.setOnMouseExited(e -> b.setStyle("-fx-background-color: #1a73e8; -fx-text-fill: white; "
            + "-fx-background-radius: 6; -fx-padding: 9 22; -fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand;"));
        return b;
    }
    private Button sbtn(String text) { // 次按钮（白底灰边框）
        Button b = new Button(text);
        b.setStyle("-fx-background-color: white; -fx-text-fill: #374151; -fx-background-radius: 6; "
            + "-fx-border-color: #d1d5db; -fx-border-radius: 6; -fx-border-width: 1; "
            + "-fx-padding: 7 16; -fx-font-size: 12px; -fx-cursor: hand;");
        b.setOnMouseEntered(e -> b.setStyle("-fx-background-color: #f3f4f6; -fx-text-fill: #1f2937; "
            + "-fx-background-radius: 6; -fx-border-color: #9ca3af; -fx-border-radius: 6; -fx-border-width: 1; "
            + "-fx-padding: 7 16; -fx-font-size: 12px; -fx-cursor: hand;"));
        b.setOnMouseExited(e -> b.setStyle("-fx-background-color: white; -fx-text-fill: #374151; "
            + "-fx-background-radius: 6; -fx-border-color: #d1d5db; -fx-border-radius: 6; -fx-border-width: 1; "
            + "-fx-padding: 7 16; -fx-font-size: 12px; -fx-cursor: hand;"));
        return b;
    }

    // ── TextArea ──
    private TextArea editorArea(String prompt) { // 输入区
        TextArea a = new TextArea();
        a.setPrefRowCount(6); a.setPromptText(prompt);
        a.setStyle("-fx-font-family: 'Consolas', 'Monaco', monospace; -fx-font-size: 13px; "
            + "-fx-background-color: #fafbfc; -fx-background-radius: 8; "
            + "-fx-border-color: #d1d5db; -fx-border-radius: 8; -fx-border-width: 1; "
            + "-fx-padding: 12;");
        return a;
    }
    private TextArea resultArea(String prompt) { // 结果输出区
        TextArea a = new TextArea();
        a.setPrefRowCount(6); a.setPromptText(prompt); a.setEditable(false);
        a.setStyle(resultStyle());
        return a;
    }
    private String resultStyle() {
        return "-fx-font-family: 'Consolas', 'Monaco', monospace; -fx-font-size: 13px; "
            + "-fx-background-color: #f8f9fb; -fx-background-radius: 8; "
            + "-fx-border-color: #c7d2fe; -fx-border-radius: 8; -fx-border-width: 1 0 0 0; "
            + "-fx-padding: 12; -fx-control-inner-background: #f8f9fb;";
    }

    // ── HBox ──
    private HBox hbox(int sp, javafx.scene.Node... nodes) { HBox b = new HBox(sp); b.getChildren().addAll(nodes); b.setAlignment(Pos.CENTER_LEFT); return b; }

    @Override public void stop() { if (springContext != null) springContext.close(); Platform.exit(); }
}
