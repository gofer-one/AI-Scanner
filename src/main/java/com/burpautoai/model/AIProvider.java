package com.burpautoai.model;

import java.util.ArrayList;
import java.util.List;

public class AIProvider {
    private String name;
    private String apiEndpoint;
    private String modelsEndpoint;
    private String authType;
    private boolean isCustom;
    private List<String> availableModels = new ArrayList<>();
    private String selectedAgent = "";

    public AIProvider(String name, String apiEndpoint, String modelsEndpoint, String authType) {
        this.name = name;
        this.apiEndpoint = apiEndpoint;
        this.modelsEndpoint = modelsEndpoint;
        this.authType = authType;
        this.isCustom = false;
    }

    public AIProvider(String name, String apiEndpoint, String modelsEndpoint, String authType, boolean isCustom) {
        this.name = name;
        this.apiEndpoint = apiEndpoint;
        this.modelsEndpoint = modelsEndpoint;
        this.authType = authType;
        this.isCustom = isCustom;
    }

    public List<String> getAvailableModels() {
        if (this.availableModels == null) this.availableModels = new ArrayList<>();
        return this.availableModels;
    }

    public void setAvailableModels(List<String> availableModels) {
        this.availableModels = availableModels;
    }

    public String getSelectedAgent() {
        return this.selectedAgent != null ? this.selectedAgent : "";
    }

    public void setSelectedAgent(String selectedAgent) {
        this.selectedAgent = selectedAgent;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getApiEndpoint() {
        return this.apiEndpoint;
    }

    public void setApiEndpoint(String apiEndpoint) {
        this.apiEndpoint = apiEndpoint;
    }

    public String getModelsEndpoint() {
        return this.modelsEndpoint;
    }

    public void setModelsEndpoint(String modelsEndpoint) {
        this.modelsEndpoint = modelsEndpoint;
    }

    public String getAuthType() {
        return this.authType;
    }

    public void setAuthType(String authType) {
        this.authType = authType;
    }

    public boolean isCustom() {
        return this.isCustom;
    }

    public void setCustom(boolean custom) {
        this.isCustom = custom;
    }

    public String toString() {
        return this.name;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        }
        AIProvider that = (AIProvider)obj;
        if (this.name == null && that.name == null) {
            return true;
        }
        if (this.name == null || that.name == null) {
            return false;
        }
        return this.name.equals(that.name);
    }

    public int hashCode() {
        return this.name != null ? this.name.hashCode() : 0;
    }

    public static List<AIProvider> getDefaultProviders() {
        return new ArrayList<AIProvider>();
    }
}

