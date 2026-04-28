package com.burpautoai.core;

import burp.IBurpExtenderCallbacks;
import burp.IExtensionHelpers;
import burp.IHttpRequestResponse;
import burp.IHttpService;
import burp.IRequestInfo;
import com.burpautoai.model.ScanTask;
import com.burpautoai.ui.LogPanel;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AIEngine {
    private final IBurpExtenderCallbacks callbacks;
    private final IExtensionHelpers helpers;
    private final LogPanel logPanel;
    private final OkHttpClient httpClient;
    private final Gson gson;
    private static final MediaType JSON_TYPE = MediaType.parse("application/json; charset=utf-8");

    public AIEngine(IBurpExtenderCallbacks callbacks, IExtensionHelpers helpers, LogPanel logPanel) {
        this.callbacks = callbacks;
        this.helpers = helpers;
        this.logPanel = logPanel;
        this.gson = new Gson();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(60L, TimeUnit.SECONDS)
                .writeTimeout(60L, TimeUnit.SECONDS)
                .readTimeout(600L, TimeUnit.SECONDS)
                .build();
    }

    public void scanRequest(ScanTask task) {
        try {
            task.setStatus(ScanTask.TaskStatus.SCANNING);
            task.setAiAnalysis("");
            this.logPanel.logAI("[任务 #" + task.getId() + "] 开始分析...");
            
            String requestInfo = this.buildRequestInfo(task.getOriginalRequest());
            
            // 1. 获取 AI 响应原文
            String rawJsonResponse = this.getRawAIResponse(task, requestInfo);
            if (rawJsonResponse == null || rawJsonResponse.isEmpty()) {
                task.setStatus(ScanTask.TaskStatus.FINISHED);
                this.logPanel.logError("[任务 #" + task.getId() + "] AI 未返回任何数据");
                return;
            }

            // 2. 提取并保存分析文本
            String aiContent = this.extractContentFromRaw(rawJsonResponse);
            if (aiContent == null || aiContent.trim().isEmpty()) {
                aiContent = rawJsonResponse;
            }
            task.setAiAnalysis(aiContent);
            this.logPanel.logSuccess("[任务 #" + task.getId() + "] 成功获取 AI 分析文本");

            // 3. 尝试解析并执行 Payloads
            this.processPayloads(task, aiContent, requestInfo);
            
            task.setStatus(ScanTask.TaskStatus.FINISHED);
        } catch (Exception e) {
            this.callbacks.printError("scanRequest error: " + e.getMessage());
            task.setStatus(ScanTask.TaskStatus.FINISHED);
        }
    }

    private String getRawAIResponse(ScanTask task, String requestInfo) throws IOException {
        ConfigManager.Config config = ConfigManager.getInstance().getConfig();
        String prompt = config.getPrompt();
        if (prompt == null || prompt.isEmpty()) {
            prompt = "你是一个Web安全专家。分析提供的 HTTP 请求与响应，识别潜在漏洞并提供详细分析。如果你能生成测试 Payloads，请在回复末尾以 JSON 格式提供：{\"testPayloads\": [{\"type\": \"...\", \"payload\": \"...\", \"position\": \"...\"}]}";
        }
        
        String userPrompt = "请全方位分析以下 HTTP 请求与响应：\n\n" + requestInfo;
        
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", config.getSelectedAgent());
        JsonArray messages = new JsonArray();
        
        JsonObject sMsg = new JsonObject();
        sMsg.addProperty("role", "system");
        sMsg.addProperty("content", prompt);
        messages.add(sMsg);
        
        JsonObject uMsg = new JsonObject();
        uMsg.addProperty("role", "user");
        uMsg.addProperty("content", userPrompt);
        messages.add(uMsg);
        
        requestBody.add("messages", messages);
        requestBody.addProperty("temperature", 0.0);

        String jsonRequest = this.gson.toJson(requestBody);
        this.logPanel.logAI("[发送请求] 模型: " + config.getSelectedAgent());
        this.logPanel.logRequest(jsonRequest);
        
        RequestBody body = RequestBody.create(JSON_TYPE, jsonRequest);
        Request request = new Request.Builder()
                .url(config.getApiEndpoint())
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + config.getApiKey())
                .post(body).build();
        
        try (Response response = this.httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String err = response.body() != null ? response.body().string() : "Unknown Error";
                this.logPanel.logError("[API 错误] HTTP " + response.code() + ": " + err);
                return null;
            }
            String resp = response.body().string();
            // 在日志中记录原始响应，方便调试
            this.logPanel.logAI("[接收响应原文]");
            this.logPanel.logInfo(resp);
            return resp;
        }
    }

    private String extractContentFromRaw(String rawJson) {
        try {
            JsonObject json = JsonParser.parseString(rawJson).getAsJsonObject();
            if (json.has("choices")) {
                JsonArray choices = json.getAsJsonArray("choices");
                if (choices.size() > 0) {
                    JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");
                    if (message != null && message.has("content")) {
                        return message.get("content").getAsString();
                    }
                }
            }
            // 兜底方案：如果不是标准的 OpenAI 格式，但有 text 字段（某些模型）
            if (json.has("text")) return json.get("text").getAsString();
            
            return null;
        } catch (Exception e) {
            return rawJson;
        }
    }

    private void processPayloads(ScanTask task, String content, String requestInfo) {
        String jsonStr = this.cleanJsonResponse(content);
        if (jsonStr == null || !jsonStr.contains("{")) return;

        try {
            JsonObject analysisResult = JsonParser.parseString(jsonStr).getAsJsonObject();
            if (analysisResult.has("testPayloads")) {
                JsonArray testPayloads = analysisResult.getAsJsonArray("testPayloads");
                this.logPanel.logInfo("[扫描] 识别到 " + testPayloads.size() + " 个测试载荷");
                for (int i = 0; i < testPayloads.size(); ++i) {
                    JsonObject payloadObj = testPayloads.get(i).getAsJsonObject();
                    this.executePayloadTest(task, payloadObj, i + 1);
                }
            }
        } catch (Exception e) {
            this.callbacks.printError("processPayloads error: " + e.getMessage());
        }
    }

    private String cleanJsonResponse(String content) {
        if (content == null || content.trim().isEmpty()) return null;
        String cleaned = content.trim();
        if (cleaned.contains("```")) {
            int first = cleaned.indexOf("```");
            String sub = cleaned.substring(first);
            if (sub.startsWith("```json")) first += 7; else first += 3;
            int last = cleaned.lastIndexOf("```");
            if (last > first) cleaned = cleaned.substring(first, last);
        }
        int start = cleaned.indexOf("{");
        int end = cleaned.lastIndexOf("}");
        if (start != -1 && end != -1 && end > start) {
            return cleaned.substring(start, end + 1);
        }
        return null;
    }

    private void executePayloadTest(ScanTask task, JsonObject payloadObj, int index) {
        try {
            String vulnType = payloadObj.has("type") ? payloadObj.get("type").getAsString() : "UNKNOWN";
            String payload = payloadObj.has("payload") ? payloadObj.get("payload").getAsString() : "";
            String position = payloadObj.has("position") ? payloadObj.get("position").getAsString() : "auto";
            
            this.logPanel.logRequest("[探测 #" + index + "] 类型: " + vulnType + " | Payload: " + payload + " | 位置: " + position);
            
            IHttpRequestResponse testReqRes = this.sendTestRequest(task, payload, position);
            if (testReqRes != null) {
                task.addProbeRecord(new ScanTask.ProbeRecord(index, vulnType, payload, position, testReqRes));
            }
        } catch (Exception e) {}
    }

    private String buildRequestInfo(IHttpRequestResponse message) {
        StringBuilder info = new StringBuilder();
        byte[] requestBytes = message.getRequest();
        byte[] responseBytes = message.getResponse();

        info.append("=== ORIGINAL REQUEST ===\n");
        if (requestBytes != null) {
            info.append(new String(requestBytes, StandardCharsets.UTF_8));
        } else {
            info.append("(Empty Request)");
        }

        if (responseBytes != null && responseBytes.length > 0) {
            String respStr = new String(responseBytes, StandardCharsets.UTF_8);
            // 记录日志：发现回显包
            this.logPanel.logInfo("[数据准备] 捕获到原始响应数据 (" + responseBytes.length + " 字节)");
            
            info.append("\n\n=== ORIGINAL RESPONSE ===\n");
            // 智能截断：如果响应超过 15000 字符，保留头部和前段部分
            if (respStr.length() > 15000) {
                info.append(respStr.substring(0, 15000)).append("\n...(Response truncated due to size)");
            } else {
                info.append(respStr);
            }
        } else {
            this.logPanel.logWarning("[数据准备] 未发现原始响应数据，AI 将仅根据请求包进行分析");
            info.append("\n\n=== ORIGINAL RESPONSE ===\n(No response data available)");
        }
        return info.toString();
    }

    private IHttpRequestResponse sendTestRequest(ScanTask task, String payload, String position) {
        try {
            IHttpService service = task.getOriginalRequest().getHttpService();
            byte[] rawRequest = task.getOriginalRequest().getRequest();
            return this.callbacks.makeHttpRequest(service, rawRequest);
        } catch (Exception e) { return null; }
    }
}
