package com.burpautoai.model;

import burp.IHttpRequestResponse;
import java.util.Date;

public class ScanTask {
    private ScanMode scanMode;
    private int id;
    private String method;
    private String url;
    private TaskStatus status;
    private IHttpRequestResponse originalRequest;
    private Date createTime;
    private Date finishTime;
    private String errorMessage;
    private String aiAnalysis;
    private String customPrompt;
    private volatile boolean isStopped = false;

    public ScanTask(int id, IHttpRequestResponse request, String method, String url) {
        this(id, request, method, url, ScanMode.CUSTOM);
    }

    public ScanTask(int id, IHttpRequestResponse request, String method, String url, ScanMode scanMode) {
        this.id = id;
        this.originalRequest = request;
        this.method = method;
        this.url = url;
        this.scanMode = scanMode == null ? ScanMode.CUSTOM : scanMode;
        this.status = TaskStatus.PENDING;
        this.createTime = new Date();
    }

    public int getId() {
        return this.id;
    }

    public String getMethod() {
        return this.method;
    }

    public String getUrl() {
        return this.url;
    }

    public TaskStatus getStatus() {
        return this.status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
        if (status == TaskStatus.FINISHED) {
            this.finishTime = new Date();
        }
    }

    public IHttpRequestResponse getOriginalRequest() {
        return this.originalRequest;
    }

    public Date getCreateTime() {
        return this.createTime;
    }

    public Date getFinishTime() {
        return this.finishTime;
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }

    public ScanMode getScanMode() {
        return this.scanMode;
    }

    public void setScanMode(ScanMode scanMode) {
        this.scanMode = scanMode == null ? ScanMode.CUSTOM : scanMode;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getAiAnalysis() {
        return this.aiAnalysis != null ? this.aiAnalysis : "";
    }

    public void setAiAnalysis(String aiAnalysis) {
        this.aiAnalysis = aiAnalysis;
    }

    public String getCustomPrompt() {
        return this.customPrompt;
    }

    public void setCustomPrompt(String customPrompt) {
        this.customPrompt = customPrompt;
    }

    public boolean isStopped() {
        return isStopped;
    }

    public void setStopped(boolean stopped) {
        isStopped = stopped;
    }

    public static enum TaskStatus {
        PENDING("\u5f85\u5904\u7406"),
        SCANNING("\u8fdb\u884c\u4e2d"),
        FINISHED("\u5df2\u5b8c\u6210");

        private String displayName;

        private TaskStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return this.displayName;
        }
    }

    public static enum ScanMode {
        CUSTOM("AI智能扫描", "CUSTOM");

        private String displayName;
        private String typeKey;

        private ScanMode(String displayName, String typeKey) {
            this.displayName = displayName;
            this.typeKey = typeKey;
        }

        public String getDisplayName() {
            return this.displayName;
        }

        public String getTypeKey() {
            return this.typeKey;
        }

        public boolean isCustom() {
            return this == CUSTOM;
        }
    }
}
