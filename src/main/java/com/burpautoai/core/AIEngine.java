package com.burpautoai.core;

import burp.IBurpExtenderCallbacks;
import burp.IExtensionHelpers;
import burp.IHttpRequestResponse;
import burp.IRequestInfo;
import burp.IResponseInfo;
import com.burpautoai.model.ScanTask;
import com.burpautoai.ui.LogPanel;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
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
    private static final int MAX_RESPONSE_CHARS = 80000;
    private static final int MAX_BODY_TRUNCATE = 10000;
    private static final int DEFAULT_MAX_TOKENS = 8192;

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
            this.logPanel.logAI("[任务 #" + task.getId() + "] 开始深度流式分析...");
            
            String requestInfo = this.buildRequestInfo(task.getOriginalRequest());
            this.executeStreamRequest(task, requestInfo, callback);
            
            task.setStatus(ScanTask.TaskStatus.FINISHED);
        } catch (Exception e) {
            this.callbacks.printError("scanRequest error: " + e.getMessage());
            task.setStatus(ScanTask.TaskStatus.FINISHED);
            task.setErrorMessage("系统异常: " + e.getMessage());
        }
    }

    private void executeStreamRequest(ScanTask task, String requestInfo, StreamingCallback callback) throws IOException {
        ConfigManager.Config config = ConfigManager.getInstance().getConfig();
        String prompt = (task.getCustomPrompt() != null && !task.getCustomPrompt().isEmpty()) 
                        ? task.getCustomPrompt() : config.getPrompt();
        
        if (prompt == null || prompt.isEmpty()) prompt = "Analyze the safety of this request.";
        
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", config.getSelectedAgent());
        requestBody.addProperty("stream", true); 
        requestBody.addProperty("temperature", 0.0);
        requestBody.addProperty("max_tokens", config.getMaxTokens() > 0 ? config.getMaxTokens() : DEFAULT_MAX_TOKENS); 

        JsonArray messages = new JsonArray();
        messages.add(createMsg("system", prompt));
        messages.add(createMsg("user", requestInfo));
        requestBody.add("messages", messages);

        String jsonRequest = this.gson.toJson(requestBody);
        this.logPanel.logRequest(jsonRequest);
        
        Request.Builder rb = new Request.Builder().url(config.getApiEndpoint()).post(RequestBody.create(JSON_TYPE, jsonRequest));
        applyAuthHeaders(rb, config);

        StringBuilder fullContent = new StringBuilder();
        try (Response response = this.httpClient.newCall(rb.build()).execute()) {
            if (!response.isSuccessful()) {
                task.setErrorMessage(handleHttpError(response.code()));
                return;
            }
            
            okio.BufferedSource source = response.body().source();
            while (!source.exhausted() && !task.isStopped()) {
                if (fullContent.length() > MAX_RESPONSE_CHARS) {
                    fullContent.append("\n\n[系统提示: 内容达到安全上限已截断]");
                    break;
                }
                
                String line = source.readUtf8Line();
                if (line == null) break;
                if (!line.startsWith("data: ")) continue;
                
                String data = line.substring(6).trim();
                if (data.equals("[DONE]")) break;
                
                parseStreamData(data, fullContent, task, callback);
            }
        }
    }

    private JsonObject createMsg(String role, String content) {
        JsonObject m = new JsonObject();
        m.addProperty("role", role);
        m.addProperty("content", content);
        return m;
    }

    private void applyAuthHeaders(Request.Builder rb, ConfigManager.Config config) {
        String authType = config.getAuthType().toLowerCase();
        String key = config.getApiKey();
        if (authType.equals("x-api-key")) rb.addHeader("x-api-key", key);
        else if (authType.equals("api-key")) rb.addHeader("api-key", key);
        else rb.addHeader("Authorization", "Bearer " + key);
        
        if (config.getApiEndpoint().contains("anthropic.com")) rb.addHeader("anthropic-version", "2023-06-01");
    }

    private String handleHttpError(int code) {
        if (code == 401) return "API Key 鉴权失败 (401)";
        if (code == 404) return "端点路径错误 (404)";
        if (code == 429) return "额度耗尽或限频 (429)";
        return "HTTP Error " + code;
    }

    private void parseStreamData(String data, StringBuilder full, ScanTask task, StreamingCallback cb) {
        try {
            JsonObject json = JsonParser.parseString(data).getAsJsonObject();
            JsonArray choices = json.getAsJsonArray("choices");
            if (choices != null && choices.size() > 0) {
                JsonObject choice = choices.get(0).getAsJsonObject();
                if (choice.has("finish_reason") && "length".equals(choice.get("finish_reason").getAsString())) {
                    this.logPanel.logWarning("AI 输出由于长度限制被截断");
                }
                JsonObject delta = choice.getAsJsonObject("delta");
                if (delta != null && delta.has("content")) {
                    String chunk = delta.get("content").getAsString();
                    full.append(chunk);

                    // 检查是否存在异常循环输出 (如连续的 📐 或重复字符)
                    if (full.length() > 20 && isGarbageOutput(full)) {
                        long now = System.currentTimeMillis();
                        if (task.getGarbageDetectedTime() == 0) {
                            task.setGarbageDetectedTime(now);
                            this.logPanel.logWarning("[任务 #" + task.getId() + "] 检测到异常循环输出，开启1分钟熔断倒计时...");
                        } else if (now - task.getGarbageDetectedTime() > 60000) {
                            task.setErrorMessage("AI 分析错误：检测到持续循环输出超过1分钟，已自动终止");
                            task.setStopped(true);
                        }
                    } else {
                        // 如果恢复正常输出，重置计时器
                        if (task.getGarbageDetectedTime() != 0) {
                            task.setGarbageDetectedTime(0);
                            this.logPanel.logAI("[任务 #" + task.getId() + "] AI 已恢复正常输出，重置熔断计时器");
                        }
                    }

                    task.setAiAnalysis(full.toString());
                    if (cb != null) cb.onChunk(chunk);
                }
            }
        } catch (Exception e) {}
    }

    private boolean isGarbageOutput(StringBuilder sb) {
        int len = sb.length();
        // 1. 检查末尾是否出现大量连续的 📐
        int triangleCount = 0;
        for (int i = len - 1; i >= Math.max(0, len - 60); i--) {
            char c = sb.charAt(i);
            if (c == '\uD83D' || c == '\uDCC4') { // 📐 is \uD83D\uDCC4
                triangleCount++;
            } else if (!Character.isWhitespace(c)) {
                if (triangleCount > 0) break; // 只统计末尾连续的
            }
        }
        // 📐 是由两个 char 组成的，所以 count / 2
        if (triangleCount / 2 >= 3) return true;

        // 2. 检查末尾单一字符极大量重复
        char lastChar = sb.charAt(len - 1);
        if (!Character.isWhitespace(lastChar)) {
            int repeatCount = 0;
            for (int i = len - 1; i >= Math.max(0, len - 100); i--) {
                if (sb.charAt(i) == lastChar) repeatCount++;
                else break;
            }
            if (repeatCount >= 50) return true;
        }

        return false;
    }

    private String buildRequestInfo(IHttpRequestResponse message) {
        StringBuilder info = new StringBuilder();
        info.append("=== HTTP REQUEST ===\n").append(this.simplifyRequest(message));
        if (message.getResponse() != null) {
            info.append("\n\n=== HTTP RESPONSE ===\n").append(this.simplifyResponse(message));
        }
        return info.toString();
    }

    private String simplifyRequest(IHttpRequestResponse message) {
        IRequestInfo reqInfo = this.helpers.analyzeRequest(message);
        int bodyOffset = reqInfo.getBodyOffset();
        byte[] reqBytes = message.getRequest();
        String body = new String(Arrays.copyOfRange(reqBytes, bodyOffset, reqBytes.length), StandardCharsets.UTF_8);
        
        if (body.length() > MAX_BODY_TRUNCATE) body = body.substring(0, MAX_BODY_TRUNCATE) + "\n...(Truncated)";
        
        StringBuilder sb = new StringBuilder();
        for (String h : reqInfo.getHeaders()) {
            String lh = h.toLowerCase();
            if (lh.startsWith("accept:") || lh.startsWith("user-agent:") || lh.startsWith("sec-ch-ua")) continue;
            sb.append(h).append("\n");
        }
        return sb.append("\n").append(body).toString();
    }

    private String simplifyResponse(IHttpRequestResponse message) {
        IResponseInfo respInfo = this.helpers.analyzeResponse(message.getResponse());
        byte[] respBytes = message.getResponse();
        String body = new String(Arrays.copyOfRange(respBytes, respInfo.getBodyOffset(), respBytes.length), StandardCharsets.UTF_8);
        
        body = body.replaceAll("data:[^;]+;base64,[A-Za-z0-9+/=]{100,}", "data:(Base64 Truncated)");
        body = body.replaceAll("(?is)<style.*?>.*?</style>", "<style>(CSS Omitted)</style>");
        body = body.replaceAll("(?is)<svg.*?>.*?</svg>", "<svg>(SVG Omitted)</svg>");
        
        if (body.length() > 15000) body = body.substring(0, 15000) + "\n...(Truncated)";
        
        StringBuilder sb = new StringBuilder();
        for (String h : respInfo.getHeaders()) {
            String lh = h.toLowerCase();
            if (lh.startsWith("server:") || lh.startsWith("date:") || lh.startsWith("content-length:")) continue;
            sb.append(h).append("\n");
        }
        return sb.append("\n").append(body).toString();
    }
}
