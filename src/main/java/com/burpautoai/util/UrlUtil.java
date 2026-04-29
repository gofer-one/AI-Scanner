package com.burpautoai.util;

public class UrlUtil {
    /**
     * 根据基础地址智能补全聊天端点
     */
    public static String fixChatEndpoint(String url) {
        if (url == null || url.trim().isEmpty()) return url;
        String trimmed = url.trim();
        
        // 如果是 OpenAI 格式的基础路径
        if (trimmed.endsWith("/v1") || trimmed.endsWith("/v1/")) {
            String base = trimmed.endsWith("/") ? trimmed : trimmed + "/";
            return base + "chat/completions";
        }
        
        // 如果是 Ollama 默认地址 (通常是 11434 端口且没加路径)
        if (trimmed.matches(".*:11434/?")) {
            String base = trimmed.endsWith("/") ? trimmed : trimmed + "/";
            return base + "api/chat";
        }
        
        return trimmed;
    }

    /**
     * 根据基础地址智能补全模型端点
     */
    public static String fixModelsEndpoint(String url) {
        if (url == null || url.trim().isEmpty()) return url;
        String trimmed = url.trim();
        
        if (trimmed.endsWith("/v1") || trimmed.endsWith("/v1/")) {
            String base = trimmed.endsWith("/") ? trimmed : trimmed + "/";
            return base + "models";
        }
        
        if (trimmed.matches(".*:11434/?")) {
            String base = trimmed.endsWith("/") ? trimmed : trimmed + "/";
            return base + "api/tags";
        }
        
        return trimmed;
    }
}
