# Phase 10: JavaFX 桌面端

## 目标

可独立运行的 Windows 桌面应用。

## 前置条件

- [ ] Phase 9 完成（REST API 可用）

## 步骤

### 10.1 启动类
- `DmaApplication.java` — Spring Boot + JavaFX 集成启动
- Spring Boot 作为后端服务运行在 localhost:8080
- JavaFX 作为前端，通过 HTTP 调用 API

### 10.2 页面清单（共 5 个功能页 + 首页）

| 页面 | FXML 文件 | 功能 |
|------|-----------|------|
| 首页 | `main.fxml` | 导航入口 + 最近任务列表 |
| 连接管理 | `connection.fxml` | 新增/编辑/删除/测试连接 |
| SQL 转换 | `sql-convert.fxml` | 输入 SQL→扫描→显示转换建议 |
| 项目扫描 | `project-scan.fxml` | 选择目录→扫描→结果表格 |
| 迁移任务 | `migration-task.fxml` | 创建/执行任务→进度→结果 |
| 报告查看 | `report.fxml` | HTML 预览 + PDF/Word 导出按钮 |

### 10.3 通用组件
- 数据表格组件（排序、筛选、分页）
- 状态栏组件
- 错误弹窗组件
- Loading 进度指示器

### 10.4 打包
- `mvn package` 生成可执行 JAR
- 后续用 `jpackage` 打包为 Windows EXE

## 验证标准

应用启动，完成"连接管理→SQL转换→项目扫描→报告导出"完整操作流程。
