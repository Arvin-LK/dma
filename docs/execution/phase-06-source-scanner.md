# Phase 6: 源码扫描器

## 目标

实现 Java 文件 SQL 字符串扫描 + MyBatis Mapper XML 扫描。

## 前置条件

- [ ] Phase 5 完成（SQL 解析与转换可用）

## 步骤

### 6.1 JavaSourceScanner
- 递归遍历 .java 文件
- 使用正则匹配字符串字面量中的 SQL 关键词
- 提取 SQL 片段（含行号、列号）
- 调用兼容性分析器分析每个片段

### 6.2 MyBatisXmlScanner
- 递归遍历 *Mapper.xml 文件
- XML 解析，提取 `<select>/<insert>/<update>/<delete>` 标签内容
- 处理 `${}` 和 `#{}` 占位符（替换为标记）
- 调用兼容性分析器分析

### 6.3 ProjectScannerFacade
- 编排 JavaSourceScanner + MyBatisXmlScanner
- 统一入口：输入项目根目录 → 输出所有扫描结果
- 支持进度回调

### 6.4 DefaultSourceCodeScanner
- 实现 `SourceCodeScanner` 领域服务接口

## 验证标准

提供测试 fixtures（含 SQL 的 Java 文件、Mapper XML），扫描结果正确匹配预期。
