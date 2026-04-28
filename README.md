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
<img width="1600" height="1574" alt="image" src="https://github.com/user-attachments/assets/a804d5ff-c243-46e4-a2f4-435dde94c99a" />



> * 扫描界面截图
<img width="2690" height="1096" alt="image" src="https://github.com/user-attachments/assets/f70b65eb-bfad-4044-b02d-ab4c495e21b8" />



> * 漏洞分析结果
<img width="1700" height="1400" alt="image" src="https://github.com/user-attachments/assets/5cd82504-00ca-4bca-9727-9d245486a601" />


---

## 🏗️ 架构设计

```text
HTTP Request → AI Engine → Payload Generator → Scanner → Result Analyzer → UI
```

### 核心模块：

| 模块        | 说明        |
| --------- | --------- |
| `core`    | AI 分析引擎   |
| `scanner` | 扫描逻辑      |
| `model`   | 数据结构      |
| `ui`      | Burp 插件界面 |

---

## 🚀 功能特性

### 🧠 AI 智能分析

* 自动理解接口参数语义
* 分析请求/响应上下文
* 输出漏洞解释 + 修复建议

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

* 风险等级标记
* AI 分析说明
* 请求/响应对比
* Payload 详情
<img width="1600" height="1300" alt="image" src="https://github.com/user-attachments/assets/f15cf0c8-1d3b-48ab-8a53-4594ff05c0b7" />


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
