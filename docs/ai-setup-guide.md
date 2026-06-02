# AI 迁移顾问部署指南

DMA 支持两种 AI 部署模式，满足不同网络环境需求。

---

## 模式一：本地大模型（内网环境 — 推荐）

使用 [Ollama](https://ollama.com) 在本地运行开源大模型，**完全离线，无需联网，数据不出内网**。

### 1. 安装 Ollama

```bash
# Windows: 下载安装包
# https://ollama.com/download/windows

# Linux:
curl -fsSL https://ollama.com/install.sh | sh
```

### 2. 下载推荐模型

```bash
# 推荐：通义千问 2.5（中文能力最强，7B 参数，约 4.5GB）
ollama pull qwen2.5:7b

# 或者
ollama pull llama3:8b         # Meta Llama3，通用能力强
ollama pull deepseek-r1:8b    # DeepSeek 推理模型，代码能力强
ollama pull qwen2.5:14b       # 通义千问 14B，更强但需更多内存
```

### 3. 验证安装

```bash
ollama list
# 应显示已下载的模型

ollama run qwen2.5:7b
# 输入: 你好
# 应返回模型的回复
```

### 4. 配置 DMA

编辑 `dma-core/src/main/resources/application.yml`：

```yaml
dma:
  ai:
    provider: ollama              # 切换为 ollama
    ollama:
      url: http://localhost:11434/v1
      model: qwen2.5:7b           # 你下载的模型名
```

### 5. 启动 DMA

```bash
run.bat
# 切换到「AI 顾问」Tab，点击「🔄 检查状态」
# 看到 "🤖 AI 已连接" 即可使用
```

---

## 模式二：云端 API（需联网）

### OpenAI

```yaml
dma:
  ai:
    provider: openai
    openai:
      api-key: sk-your-api-key-here
      model: gpt-4o-mini
```

### 通义千问（阿里云）

通义千问提供 OpenAI 兼容接口：

```yaml
dma:
  ai:
    provider: custom
    custom:
      url: https://dashscope.aliyuncs.com/compatible-mode/v1
      api-key: sk-your-dashscope-key
      model: qwen-plus
```

### DeepSeek

```yaml
dma:
  ai:
    provider: custom
    custom:
      url: https://api.deepseek.com/v1
      api-key: sk-your-deepseek-key
      model: deepseek-chat
```

---

## 模式三：内网部署大模型服务

如果团队共用一台 GPU 服务器：

### 选项 A：Ollama 远程访问

```bash
# 在 GPU 服务器上
ollama serve &
# 默认监听 11434 端口

# 客户端配置
dma.ai.provider=ollama
dma.ai.ollama.url=http://192.168.1.100:11434/v1
```

### 选项 B：vLLM（高性能推理）

```bash
# GPU 服务器
pip install vllm
vllm serve Qwen/Qwen2.5-7B-Instruct --port 8000

# 客户端配置
dma.ai.provider=custom
dma.ai.custom.url=http://192.168.1.100:8000/v1
dma.ai.custom.model=Qwen2.5-7B-Instruct
```

---

## 模型推荐

| 场景 | 推荐模型 | 大小 | 说明 |
|------|---------|------|------|
| **SQL 分析（中文）** | `qwen2.5:7b` | 4.5GB | 通义千问2.5，中文理解能力强 |
| **代码转换** | `deepseek-r1:8b` | 5GB | DeepSeek推理，代码能力强 |
| **通用咨询** | `llama3:8b` | 4.7GB | Meta Llama3，平衡性好 |
| **高精度** | `qwen2.5:14b` | 9GB | 需 16GB+ 内存 |

---

## 常见问题

**Q: Ollama 启动失败？**
```bash
# 检查 Ollama 是否在运行
ollama list

# 如果未运行，手动启动
ollama serve
```

**Q: 模型下载慢？**
```bash
# 设置国内镜像（如果 Ollama 官方源慢）
# 可使用 ModelScope 等国内源下载 GGUF 模型后导入
```

**Q: 内网环境如何获取模型？**
1. 在外网机器下载模型：`ollama pull qwen2.5:7b`
2. 导出模型：参考 Ollama 文档的模型迁移
3. 拷贝到内网机器导入
