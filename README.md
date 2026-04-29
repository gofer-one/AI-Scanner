# 🧠 AI-Scanner

> 🚀 基于大语言模型（LLM）的智能 Web 漏洞扫描工具（Burp Suite 扩展）

---

## ✨ 项目亮点

* 🧠 **AI 驱动漏洞分析**：理解 HTTP 语义，而不仅仅是规则匹配
* 🎯 **动态 Payload 生成**：根据上下文自动构造攻击Payload
* ⚡ **Burp 无缝集成**：直接嵌入日常渗透测试流程
* 📉 **降低误报率**：比传统扫描器更“聪明”
* 🔍 **支持逻辑漏洞分析**（传统工具弱项）

---

## 🎥 Demo

> * 配置界面截图
<img width="1664" height="1508" alt="image" src="https://github.com/user-attachments/assets/0d33c03e-d89b-4f9d-8808-ed00509c7620" />




> * 扫描界面截图
<img width="1700" height="1400" alt="image" src="https://github.com/user-attachments/assets/03102aba-36ae-4aae-a98d-e14018765823" />


---

## 🏗️ 架构设计

```text
HTTP Request → AI Engine → Payload Generator → Scanner → Result Analyzer → UI
```

### 核心模块：

| 模块        | 说明        |
| --------- | --------- |
| `core`    | AI 分析引擎（支持流式响应与上下文简化） |
| `model`   | 核心数据模型（任务、服务商、自定义 Prompt） |
| `ui`      | 响应式 Burp 插件界面（日志、配置、任务管理） |
| `util`    | 辅助工具类（URL 修复、UI 渲染组件） |

---

## 🚀 功能特性

### 🧠 AI 智能分析
* **上下文精简**：自动过滤 HTTP 请求/响应中的冗余信息（如 Base64、CSS、SVG），节省 Token。
* **自定义 Prompt**：支持全局及任务级的自定义提示词，灵活应对不同扫描场景。

### 🛠️ 多服务商管理
* **独立记忆系统**：支持 OpenAI、Anthropic 及自定义端点（如 Ollama, LM Studio）。
* **配置持久化**：每个服务商独立存储 API Key、端点及**专属模型列表**，切换不丢失配置。

### 🛡️ 智能保护机制
* **垃圾输出检测**：实时监控 AI 输出，自动识别并过滤循环垃圾字符（如重复的 📐）。
* **保护性熔断**：若检测到持续垃圾输出，自动开启 **1 分钟倒计时保护**，超时强制断开连接并记录错误，防止 Token 无谓消耗。

---

### 🔍 漏洞检测能力

支持但不限于：

* SQL Injection
* XSS
* SSRF
* 权限绕过（IDOR）
* 敏感信息泄露

---

### ⚡ 智能扫描流程

1. 捕获 HTTP 请求
2. AI 分析接口含义
3. 自动生成 Payload
4. 发起测试请求
5. 分析响应结果
6. 输出漏洞报告

---

### 🧩 Payload 生成优势

| 传统方式 | AI-Scanner |
| ---- | ---------- |
| 固定字典 | 动态生成       |
| 无上下文 | 理解语义       |
| 命中率低 | 命中率高       |

---

## 📦 安装方式

### 1️⃣ 编译项目

```bash
mvn clean package
```

生成：

```
target/*-jar-with-dependencies.jar
```

---

### 2️⃣ 加载到 Burp Suite

1. 打开 Burp Suite
2. Extender → Extensions
3. Add → 选择 jar 文件
4. 加载成功 🎉

---

## 🧪 使用方法

* 在 Proxy / Repeater 中拦截请求
* 右键 → 启动 AI 扫描
* 查看 UI 面板中的分析结果

---

## 📊 扫描结果示例

<img width="2702" height="1330" alt="image" src="https://github.com/user-attachments/assets/1a36071a-c8b6-40cf-bdcc-f11b940ab39c" />



---

## 🆚 对比传统扫描器

| 能力      | 传统扫描器 | AI-Scanner |
| ------- | ----- | ---------- |
| Payload | 固定规则  | AI生成       |
| 逻辑漏洞    | ❌     | ✅          |
| 误报率     | 高     | 低          |
| 自动化     | 高     | 更智能        |

---

## ⚠️ 安全声明

* 请仅用于**授权测试**
* AI 分析结果需人工复核
* 不要提交 API Key 到仓库

---

## 📦 Releases

👉 推荐从 Releases 下载插件：

* `.jar` 文件
* 版本更新说明
---

## 🤝 关于

* 功能完全由gemini pro重构，我不会任何代码。
* 提交 Issue 不理
* 提交 PR 不理
* 提出功能建议 不理
* 一切功能跟Ai说去吧

---

## ⭐ Star 支持

如果这个项目对你有帮助，欢迎点个 ⭐

---

## 📄 License

MIT License
