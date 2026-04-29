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
        String prompt = task.getCustomPrompt();
        if (prompt == null || prompt.isEmpty()) {
            prompt = config.getPrompt();
        }
        if (prompt == null || prompt.isEmpty()) {
            prompt = "Analyze the safety of this request.";
        }
        
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", config.getSelectedAgent());
        requestBody.addProperty("stream", true); 
        
        JsonArray messages = new JsonArray();
        JsonObject sMsg = new JsonObject();
        sMsg.addProperty("role", "system");
        sMsg.addProperty("content", prompt);
        messages.add(sMsg);
        
        JsonObject uMsg = new JsonObject();
        uMsg.addProperty("role", "user");
        uMsg.addProperty("content", requestInfo);
        messages.add(uMsg);
        
        requestBody.add("messages", messages);
        requestBody.addProperty("temperature", 0.0);
        // 从配置中动态读取最大输出 Token 限制
        requestBody.addProperty("max_tokens", config.getMaxTokens()); 

        String jsonRequest = this.gson.toJson(requestBody);
        this.logPanel.logRequest(jsonRequest);
        
        RequestBody body = RequestBody.create(JSON_TYPE, jsonRequest);
        Request.Builder requestBuilder = new Request.Builder()
                .url(config.getApiEndpoint())
                .post(body);
        
        String authType = config.getAuthType().toLowerCase();
        String apiKey = config.getApiKey();
        if (authType.equals("x-api-key")) {
            requestBuilder.addHeader("x-api-key", apiKey);
        } else if (authType.equals("api-key")) {
            requestBuilder.addHeader("api-key", apiKey);
        } else {
            requestBuilder.addHeader("Authorization", "Bearer " + apiKey);
        }
        if (config.getApiEndpoint().toLowerCase().contains("anthropic.com")) {
            requestBuilder.addHeader("anthropic-version", "2023-06-01");
        }

        StringBuilder fullContent = new StringBuilder();
        int maxChars = 80000; // 进一步放宽字符限制
        
        try (Response response = this.httpClient.newCall(requestBuilder.build()).execute()) {
            if (!response.isSuccessful()) {
                String errorInfo = "HTTP " + response.code();
                task.setErrorMessage(errorInfo);
                return;
            }
            
            okio.BufferedSource source = response.body().source();
            while (!source.exhausted()) {
                if (task.isStopped()) break;

                if (fullContent.length() > maxChars) {
                    fullContent.append("\n\n[系统提示: 内容达到插件保护上限，已停止接收]");
                    break;
                }
                
                String line = source.readUtf8Line();
                if (line == null) break;
                
                if (line.startsWith("data: ")) {
                    String data = line.substring(6).trim();
                    if (data.equals("[DONE]")) {
                        this.logPanel.logSuccess("[任务 #" + task.getId() + "] AI 分析流正常结束。");
                        break;
                    }
                    
                    try {
                        JsonObject streamJson = JsonParser.parseString(data).getAsJsonObject();
                        JsonArray choices = streamJson.getAsJsonArray("choices");
                        if (choices != null && choices.size() > 0) {
                            JsonObject choice = choices.get(0).getAsJsonObject();
                            
                            // 检查停止原因
                            if (choice.has("finish_reason") && !choice.get("finish_reason").isJsonNull()) {
                                String reason = choice.get("finish_reason").getAsString();
                                if ("length".equals(reason)) {
                                    this.logPanel.logWarning("[任务 #" + task.getId() + "] 警告: AI 输出达到模型最大长度限制，分析可能不完整。");
                                }
                            }

                            JsonObject delta = choice.getAsJsonObject("delta");
                            if (delta != null && delta.has("content")) {
                                String chunk = delta.get("content").getAsString();
                                // 只进行最基础的控制符过滤，不进行业务逻辑熔断，确保内容完整
                                if (chunk.contains("<|im_") || chunk.contains("📐")) {
                                    // 如果确实出现了超过 100 个三角形，那才是真的崩溃
                                    if (fullContent.length() > 0 && chunk.equals(fullContent.substring(fullContent.length()-1))) {
                                        // 连续重复字符检测可以在 UI 渲染层做，这里保命即可
                                    }
                                }
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
        info.append("=== HTTP REQUEST ===\n");
        info.append(this.simplifyRequest(message));
        byte[] responseBytes = message.getResponse();
        if (responseBytes != null && responseBytes.length > 0) {
            info.append("\n\n=== HTTP RESPONSE ===\n");
            info.append(this.simplifyResponse(message));
        } else {
            info.append("\n\n=== HTTP RESPONSE ===\n(No response data available)");
        }
        return info.toString();
    }

    private String simplifyRequest(IHttpRequestResponse message) {
        byte[] requestBytes = message.getRequest();
        if (requestBytes == null || requestBytes.length == 0) return "(Empty Request)";
        IRequestInfo reqInfo = this.helpers.analyzeRequest(message);
        List<String> headers = reqInfo.getHeaders();
        int bodyOffset = reqInfo.getBodyOffset();
        String body = new String(Arrays.copyOfRange(requestBytes, bodyOffset, requestBytes.length), StandardCharsets.UTF_8);
        String contentType = "";
        for (String h : headers) { if (h.toLowerCase().startsWith("content-type:")) { contentType = h.toLowerCase(); break; } }
        
        // 适当放宽 Body 保留长度，给 AI 更多上下文
        if (contentType.contains("multipart/form-data")) {
            body = body.replaceAll("(?s)(\r\n\r\n).*?(\r\n--)", "$1(Binary data omitted)$2");
        } else if (body.length() > 10000) {
            body = body.substring(0, 10000) + "\n...(Request body truncated)";
        }
        
        StringBuilder sb = new StringBuilder();
        for (String h : headers) {
            String lh = h.toLowerCase();
            if (lh.startsWith("accept:") || lh.startsWith("user-agent:") || lh.startsWith("connection:") || lh.startsWith("sec-ch-ua")) continue;
            sb.append(h).append("\n");
        }
        sb.append("\n").append(body);
        return sb.toString();
    }

    private String simplifyResponse(IHttpRequestResponse message) {
        byte[] responseBytes = message.getResponse();
        if (responseBytes == null || responseBytes.length == 0) return "(No Response)";
        IResponseInfo responseInfo = this.helpers.analyzeResponse(responseBytes);
        List<String> headers = responseInfo.getHeaders();
        int bodyOffset = responseInfo.getBodyOffset();
        String body = new String(Arrays.copyOfRange(responseBytes, bodyOffset, responseBytes.length), StandardCharsets.UTF_8);
        String contentType = "";
        for (String h : headers) { if (h.toLowerCase().startsWith("content-type:")) { contentType = h.toLowerCase(); break; } }
        
        if (contentType.contains("image/") || contentType.contains("font/") || contentType.contains("video/") || contentType.contains("audio/") || contentType.contains("application/zip") || contentType.contains("application/pdf")) {
            return String.join("\n", headers) + "\n\n(Binary content omitted)";
        }
        
        body = body.replaceAll("data:[^;]+;base64,[A-Za-z0-9+/=]{100,}", "data:(Base64 Truncated)");
        body = body.replaceAll("(?is)<style.*?>.*?</style>", "<style>(CSS Omitted)</style>");
        body = body.replaceAll("(?is)<svg.*?>.*?</svg>", "<svg>(SVG Omitted)</svg>");
        
        // 适当放宽 Response 保留长度
        if (body.length() > 15000) body = body.substring(0, 15000) + "\n...(Response body truncated)";
        
        StringBuilder sb = new StringBuilder();
        for (String h : headers) {
            String lh = h.toLowerCase();
            if (lh.startsWith("server:") || lh.startsWith("date:") || lh.startsWith("content-length:")) continue;
            sb.append(h).append("\n");
        }
        sb.append("\n").append(body);
        return sb.toString();
    }
}
