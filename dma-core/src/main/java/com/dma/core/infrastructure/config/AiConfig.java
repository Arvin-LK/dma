package com.dma.core.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * AI 配置属性。
 *
 * 使用示例（application.yml）:
 *   dma.ai.provider=ollama          # ollama | openai | custom
 *   dma.ai.ollama.url=http://localhost:11434/v1
 *   dma.ai.ollama.model=qwen2.5:7b
 *
 *   # 云端模式
 *   dma.ai.provider=openai
 *   dma.ai.openai.api-key=sk-xxx
 *   dma.ai.openai.model=gpt-4o-mini
 *
 *   # 内网自定义
 *   dma.ai.provider=custom
 *   dma.ai.custom.url=http://192.168.1.100:8000/v1
 *   dma.ai.custom.model=deepseek-r1:8b
 */
@Configuration
@ConfigurationProperties(prefix = "dma.ai")
public class AiConfig {

    /** 提供商: ollama | openai | custom | noop */
    private String provider = "noop";

    /** 请求超时秒数 */
    private int timeout = 120;

    /** 系统提示词 */
    private String systemPrompt = """
            你是一位资深数据库迁移专家。你精通 MySQL、Oracle、SQLServer、PostgreSQL、
            GaussDB、达梦、OceanBase、GoldenDB 等数据库的 SQL 语法差异和迁移最佳实践。
            请根据用户提供的 SQL 兼容性问题，给出专业、具体的迁移建议。
            如果涉及代码修改，请给出修改前后的对比。
            如果存在风险，请明确说明。""";

    private Ollama ollama = new Ollama();
    private OpenAI openai = new OpenAI();
    private Custom custom = new Custom();

    // === 子配置 ===

    public static class Ollama {
        private String url = "http://localhost:11434/v1";
        private String model = "qwen2.5:7b";
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
    }

    public static class OpenAI {
        private String url = "https://api.openai.com/v1";
        private String apiKey = "";
        private String model = "gpt-4o-mini";
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
    }

    public static class Custom {
        private String url = "http://localhost:8000/v1";
        private String apiKey = "";
        private String model = "deepseek-r1:8b";
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
    }

    // === Getters ===

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public int getTimeout() { return timeout; }
    public void setTimeout(int timeout) { this.timeout = timeout; }
    public String getSystemPrompt() { return systemPrompt; }
    public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }
    public Ollama getOllama() { return ollama; }
    public void setOllama(Ollama ollama) { this.ollama = ollama; }
    public OpenAI getOpenai() { return openai; }
    public void setOpenai(OpenAI openai) { this.openai = openai; }
    public Custom getCustom() { return custom; }
    public void setCustom(Custom custom) { this.custom = custom; }
}
