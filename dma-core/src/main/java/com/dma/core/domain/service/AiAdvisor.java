package com.dma.core.domain.service;

import com.dma.core.domain.model.scanner.ScanResult;
import java.util.List;

/**
 * SPI 扩展点：AI 迁移顾问。
 *
 * 支持两种部署模式：
 *   1. 云端模式 — OpenAI / Claude / 通义千问 等云端 API
 *   2. 本地模式 — Ollama / vLLM / LocalAI 等内网本地大模型
 *
 * 通过 application.yml 中的 dma.ai.provider 切换。
 */
public interface AiAdvisor {

    /**
     * 针对单个兼容性问题生成 AI 建议。
     */
    String generateAdvice(ScanResult result);

    /**
     * 批量分析所有问题，生成迁移计划。
     */
    String generateMigrationPlan(List<ScanResult> issues);

    /**
     * 将源数据库 SQL/DDL 转换为目标数据库语法（AI 辅助）。
     */
    String convertWithAI(String sourceDdl, String sourceDb, String targetDb);

    /**
     * 判断 AI 服务是否可用。
     */
    boolean isAvailable();

    /**
     * 获取当前使用的模型名称。
     */
    String getModelName();

    /**
     * 简短描述：由哪个模型驱动的建议。
     */
    default String getProviderInfo() {
        return getModelName();
    }
}
