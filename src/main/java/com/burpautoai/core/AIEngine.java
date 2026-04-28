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

    public interface StreamingCallback {
        void onChunk(String text);
    }

    public void scanRequest(ScanTask task, StreamingCallback callback) {
        try {
            task.setStatus(ScanTask.TaskStatus.SCANNING);
            task.setAiAnalysis("");
            this.logPanel.logAI("[任务 #" + task.getId() + "] 开始流式分析...");
            
            String requestInfo = this.buildRequestInfo(task.getOriginalRequest());
            
            // 开启流式获取
            this.executeStreamRequest(task, requestInfo, callback);
            
            task.setStatus(ScanTask.TaskStatus.FINISHED);
        } catch (Exception e) {
            this.callbacks.printError("scanRequest error: " + e.getMessage());
            task.setStatus(ScanTask.TaskStatus.FINISHED);
        }
    }

    private void executeStreamRequest(ScanTask task, String requestInfo, StreamingCallback callback) throws IOException {
        ConfigManager.Config config = ConfigManager.getInstance().getConfig();
        String prompt = config.getPrompt();
        if (prompt == null || prompt.isEmpty()) {
            prompt = "你是一个Web安全专家。分析提供的 HTTP 请求与响应，识别潜在漏洞并提供详细分析。";
        }
        
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", config.getSelectedAgent());
        requestBody.addProperty("stream", true); // 开启流式
        
        JsonArray messages = new JsonArray();
        JsonObject sMsg = new JsonObject();
        sMsg.addProperty("role", "system");
        sMsg.addProperty("content", prompt);
        messages.add(sMsg);
        
        JsonObject uMsg = new JsonObject();
        uMsg.addProperty("role", "user");
        uMsg.addProperty("content", "请分析以下请求与响应：\n\n" + requestInfo);
        messages.add(uMsg);
        
        requestBody.add("messages", messages);
        requestBody.addProperty("temperature", 0.0);

        String jsonRequest = this.gson.toJson(requestBody);
        this.logPanel.logRequest(jsonRequest);
        
        RequestBody body = RequestBody.create(JSON_TYPE, jsonRequest);
        Request request = new Request.Builder()
                .url(config.getApiEndpoint())
                .addHeader("Authorization", "Bearer " + config.getApiKey())
                .post(body).build();

        StringBuilder fullContent = new StringBuilder();
        try (Response response = this.httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) return;
            
            okio.BufferedSource source = response.body().source();
            while (!source.exhausted()) {
                String line = source.readUtf8Line();
                if (line != null && line.startsWith("data: ")) {
                    String data = line.substring(6);
                    if (data.equals("[DONE]")) break;
                    
                    try {
                        JsonObject streamJson = JsonParser.parseString(data).getAsJsonObject();
                        JsonArray choices = streamJson.getAsJsonArray("choices");
                        if (choices != null && choices.size() > 0) {
                            JsonObject delta = choices.get(0).getAsJsonObject().getAsJsonObject("delta");
                            if (delta != null && delta.has("content")) {
                                String chunk = delta.get("content").getAsString();
                                fullContent.append(chunk);
                                task.setAiAnalysis(fullContent.toString());
                                if (callback != null) callback.onChunk(chunk);
                            }
                        }
                    } catch (Exception e) {}
                }
            }
        }
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
}
