package com.burpautoai.ui;

import burp.IBurpExtenderCallbacks;
import burp.IExtensionHelpers;
import com.burpautoai.core.ConfigManager;
import com.burpautoai.model.AIProvider;
import com.burpautoai.util.UIUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ConfigDialog extends JDialog {
    private IBurpExtenderCallbacks callbacks;
    private IExtensionHelpers helpers;
    
    private JComboBox<AIProvider> providerComboBox;
    private JTextField apiKeyField, apiEndpointField, modelsEndpointField, maxTokensField;
    private JComboBox<String> authTypeComboBox, agentComboBox;
    private JTextArea promptArea;
    private JLabel statusLabel;
    private JButton quickSaveButton;
    private boolean isUpdatingProvider = false;

    private static final String ADD_NEW_PROVIDER = "➕ 新增 AI 接口 / 自定义";

    public ConfigDialog(Frame owner, IBurpExtenderCallbacks callbacks, IExtensionHelpers helpers) {
        super(owner, "配置中心", true);
        this.callbacks = callbacks;
        this.helpers = helpers;
        this.initUI();
        SwingUtilities.invokeLater(this::loadConfig);
        this.setSize(850, 900);
        this.setLocationRelativeTo(owner);
    }

    private void initUI() {
        getContentPane().setBackground(UIUtil.BG_WHITE);
        setLayout(new BorderLayout());

        JPanel content = new JPanel();
        content.setLayout(new javax.swing.BoxLayout(content, javax.swing.BoxLayout.Y_AXIS));
        content.setBackground(UIUtil.BG_WHITE);
        content.setBorder(BorderFactory.createEmptyBorder(25, 40, 25, 40));

        JLabel title = new JLabel("配置中心");
        title.setFont(UIUtil.FONT_BOLD.deriveFont(26f));
        title.setForeground(UIUtil.TEXT_DARK);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(title);
        content.add(javax.swing.Box.createVerticalStrut(20));

        content.add(createSection("API 核心设置", this::renderApiFields));
        content.add(javax.swing.Box.createVerticalStrut(15));
        content.add(createSection("扫描 Prompt 设置", this::renderPromptFields));
        content.add(javax.swing.Box.createVerticalStrut(20));

        statusLabel = new JLabel("状态: 就绪");
        statusLabel.setFont(UIUtil.FONT_MAIN);
        statusLabel.setForeground(UIUtil.ACCENT_GREEN);
        content.add(statusLabel);
        content.add(javax.swing.Box.createVerticalStrut(15));

        content.add(renderButtons());
        add(new JScrollPane(content), BorderLayout.CENTER);
    }

    private JPanel createSection(String title, java.util.function.Consumer<JPanel> renderer) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(UIUtil.BG_WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(BorderFactory.createLineBorder(UIUtil.BORDER_COLOR), title),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        renderer.accept(p);
        return p;
    }

    private void renderApiFields(JPanel p) {
        addLabel(p, "AI 服务商:", 0);
        JPanel row0 = new JPanel(new BorderLayout(10, 0));
        row0.setBackground(UIUtil.BG_WHITE);
        providerComboBox = new JComboBox<>();
        providerComboBox.addActionListener(e -> { if (!isUpdatingProvider) onProviderChanged(); });
        row0.add(providerComboBox, BorderLayout.CENTER);
        
        JPanel row0Btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        row0Btns.setBackground(UIUtil.BG_WHITE);
        quickSaveButton = UIUtil.createButton("💾 存为新端点", false);
        quickSaveButton.setPreferredSize(new Dimension(110, 30));
        quickSaveButton.addActionListener(e -> quickSaveEndpoint());
        row0Btns.add(quickSaveButton);
        JButton mBtn = UIUtil.createButton("⚙️ 管理", false);
        mBtn.setPreferredSize(new Dimension(70, 30));
        mBtn.addActionListener(e -> { new EndpointManagerDialog((Frame)getParent()).setVisible(true); reloadProviders(); });
        row0Btns.add(mBtn);
        row0.add(row0Btns, BorderLayout.EAST);
        addComponent(p, row0, 0);

        addLabel(p, "API Key:", 1);
        apiKeyField = UIUtil.createTextField();
        addComponent(p, apiKeyField, 1);

        addLabel(p, "认证方式:", 2);
        authTypeComboBox = new JComboBox<>(new String[]{"bearer", "x-api-key", "api-key"});
        authTypeComboBox.setBackground(UIUtil.BG_WHITE);
        addComponent(p, authTypeComboBox, 2);

        addLabel(p, "最大生成 Token:", 3);
        maxTokensField = UIUtil.createTextField();
        addComponent(p, maxTokensField, 3);

        addLabel(p, "API 端点:", 4);
        apiEndpointField = UIUtil.createTextField();
        apiEndpointField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusLost(java.awt.event.FocusEvent e) { autoCompleteUrls(); }
        });
        addComponent(p, apiEndpointField, 4);

        addLabel(p, "模型端点:", 5);
        modelsEndpointField = UIUtil.createTextField();
        addComponent(p, modelsEndpointField, 5);

        addLabel(p, "当前模型:", 6);
        agentComboBox = new JComboBox<>();
        agentComboBox.setBackground(UIUtil.BG_WHITE);
        addComponent(p, agentComboBox, 6);
    }

    private void renderPromptFields(JPanel p) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH; gbc.weightx = 1.0; gbc.weighty = 1.0;
        promptArea = UIUtil.createTextArea(6);
        p.add(new JScrollPane(promptArea), gbc);
    }

    private JPanel renderButtons() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        p.setBackground(UIUtil.BG_WHITE); p.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton save = UIUtil.createButton("保存配置", true); save.addActionListener(e -> saveConfig());
        JButton verify = UIUtil.createButton("验证密钥", false); verify.addActionListener(e -> verifyApiKey());
        JButton refresh = UIUtil.createButton("获取模型", false); refresh.addActionListener(e -> refreshAgents());
        JButton close = UIUtil.createButton("关闭", false); close.addActionListener(e -> dispose());
        p.add(save); p.add(verify); p.add(refresh); p.add(close);
        return p;
    }

    private void addLabel(JPanel p, String text, int row) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = row; gbc.anchor = GridBagConstraints.WEST; gbc.insets = new Insets(6, 5, 6, 15);
        JLabel l = new JLabel(text); l.setFont(UIUtil.FONT_MAIN); l.setForeground(UIUtil.TEXT_DARK);
        p.add(l, gbc);
    }

    private void addComponent(JPanel p, Component c, int row) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 1; gbc.gridy = row; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.insets = new Insets(6, 5, 6, 5);
        p.add(c, gbc);
    }

    private String currentProviderName = "";

    private void onProviderChanged() {
        AIProvider s = (AIProvider)providerComboBox.getSelectedItem();
        if (s == null) return;
        
        // If the provider name hasn't changed, don't reset the fields or models
        if (s.getName().equals(currentProviderName)) return;
        currentProviderName = s.getName();

        if (s.getName().equals(ADD_NEW_PROVIDER)) {
            apiKeyField.setText(""); apiEndpointField.setText(""); modelsEndpointField.setText(""); 
            maxTokensField.setText("8192"); authTypeComboBox.setSelectedIndex(0);
            agentComboBox.removeAllItems(); agentComboBox.addItem("等待填写...");
            quickSaveButton.setVisible(true); statusLabel.setText("状态: 请填写新的 API 配置");
        } else {
            ConfigManager.Config c = ConfigManager.getInstance().getConfig();
            apiKeyField.setText(c.getApiKey()); 
            apiEndpointField.setText(s.getApiEndpoint());
            modelsEndpointField.setText(s.getModelsEndpoint());
            maxTokensField.setText(String.valueOf(c.getMaxTokens()));
            authTypeComboBox.setSelectedItem(s.getAuthType().toLowerCase());
            
            // Load this provider's specific models
            agentComboBox.removeAllItems();
            if (s.getAvailableModels() != null && !s.getAvailableModels().isEmpty()) {
                for (String m : s.getAvailableModels()) agentComboBox.addItem(m);
                agentComboBox.setSelectedItem(s.getSelectedAgent());
                if (agentComboBox.getSelectedItem() == null && agentComboBox.getItemCount() > 0) {
                    agentComboBox.setSelectedIndex(0);
                }
            } else {
                agentComboBox.addItem("请获取模型列表");
            }
            
            quickSaveButton.setVisible(false); statusLabel.setText("状态: 已加载 " + s.getName());
        }
    }

    private void loadConfig() {
        ConfigManager.Config c = ConfigManager.getInstance().getConfig();
        isUpdatingProvider = true;
        try {
            apiKeyField.setText(c.getApiKey()); promptArea.setText(c.getPrompt());
            apiEndpointField.setText(c.getApiEndpoint()); modelsEndpointField.setText(c.getModelsEndpoint());
            maxTokensField.setText(String.valueOf(c.getMaxTokens()));
            authTypeComboBox.setSelectedItem(c.getAuthType().toLowerCase());
            reloadProviders();
            
            // Find and select the correct provider
            currentProviderName = c.getSelectedProvider();
            AIProvider selectedP = null;
            for (int i = 0; i < providerComboBox.getItemCount(); i++) {
                AIProvider p = providerComboBox.getItemAt(i);
                if (p != null && p.getName().equals(currentProviderName)) {
                    providerComboBox.setSelectedIndex(i);
                    selectedP = p;
                    break;
                }
            }
            
            // Refresh agent list from the specific provider
            agentComboBox.removeAllItems();
            if (selectedP != null && selectedP.getAvailableModels() != null && !selectedP.getAvailableModels().isEmpty()) {
                for (String m : selectedP.getAvailableModels()) agentComboBox.addItem(m);
                agentComboBox.setSelectedItem(selectedP.getSelectedAgent());
                if (agentComboBox.getSelectedItem() == null && agentComboBox.getItemCount() > 0) {
                    agentComboBox.setSelectedIndex(0);
                }
            } else {
                agentComboBox.addItem("请获取模型列表");
            }
        } finally {
            isUpdatingProvider = false;
        }
    }

    private void saveConfig() {
        ConfigManager.Config c = ConfigManager.getInstance().getConfig();
        try { c.setMaxTokens(Integer.parseInt(maxTokensField.getText().trim())); } catch (Exception e) { c.setMaxTokens(8192); }
        c.setApiKey(apiKeyField.getText().trim()); 
        c.setApiEndpoint(apiEndpointField.getText().trim());
        c.setModelsEndpoint(modelsEndpointField.getText().trim()); 
        c.setPrompt(promptArea.getText().trim());
        c.setAuthType((String)authTypeComboBox.getSelectedItem());
        
        AIProvider p = (AIProvider)providerComboBox.getSelectedItem();
        if (p != null && !p.getName().equals(ADD_NEW_PROVIDER)) {
            c.setSelectedProvider(p.getName());
            
            // Update provider's own fields
            p.setApiEndpoint(apiEndpointField.getText().trim());
            p.setModelsEndpoint(modelsEndpointField.getText().trim());
            p.setAuthType((String)authTypeComboBox.getSelectedItem());
            
            // Update provider's models and selection
            Object m = agentComboBox.getSelectedItem();
            if (m != null && !m.toString().contains("获取") && !m.toString().contains("等待")) {
                p.setSelectedAgent(m.toString());
                c.setSelectedAgent(m.toString()); // Also update global active agent
            }
            
            List<String> currentModels = new ArrayList<>();
            for (int i = 0; i < agentComboBox.getItemCount(); i++) {
                String item = agentComboBox.getItemAt(i).toString();
                if (!item.contains("获取") && !item.contains("等待")) {
                    currentModels.add(item);
                }
            }
            p.setAvailableModels(currentModels);
            c.setAvailableModels(currentModels); // Sync to global for backward compatibility/active use
        }
        
        ConfigManager.getInstance().saveConfig();
        statusLabel.setText("状态: 配置已成功保存");
    }

    private void reloadProviders() {
        boolean wasUpdating = isUpdatingProvider;
        isUpdatingProvider = true;
        AIProvider cur = (AIProvider)providerComboBox.getSelectedItem();
        String name = cur != null ? cur.getName() : "";
        providerComboBox.removeAllItems();
        for (AIProvider p : ConfigManager.getInstance().getConfig().getAllProviders()) providerComboBox.addItem(p);
        providerComboBox.addItem(new AIProvider(ADD_NEW_PROVIDER, "", "", "bearer", true));
        for (int i = 0; i < providerComboBox.getItemCount(); i++) {
            if (providerComboBox.getItemAt(i).getName().equals(name)) { providerComboBox.setSelectedIndex(i); break; }
        }
        isUpdatingProvider = wasUpdating;
    }

    private void quickSaveEndpoint() {
        String name = JOptionPane.showInputDialog(this, "端点名称:", "快速保存", JOptionPane.QUESTION_MESSAGE);
        if (name != null && !name.trim().isEmpty()) {
            AIProvider newP = new AIProvider(name.trim(), apiEndpointField.getText().trim(), modelsEndpointField.getText().trim(), (String)authTypeComboBox.getSelectedItem(), true);
            ConfigManager.getInstance().getConfig().getCustomProviders().add(newP);
            ConfigManager.getInstance().saveConfig(); reloadProviders();
            JOptionPane.showMessageDialog(this, "已存入管理列表");
        }
    }

    private void autoCompleteUrls() {
        String cur = apiEndpointField.getText().trim();
        if (cur.endsWith("/v1") || cur.endsWith("/v1/")) {
            apiEndpointField.setText(com.burpautoai.util.UrlUtil.fixChatEndpoint(cur));
            if (modelsEndpointField.getText().trim().isEmpty() || modelsEndpointField.getText().contains("/v1")) {
                modelsEndpointField.setText(com.burpautoai.util.UrlUtil.fixModelsEndpoint(cur));
            }
        }
    }

    private void verifyApiKey() {
        Object mod = agentComboBox.getSelectedItem();
        if (mod == null || mod.toString().contains("获取") || mod.toString().contains("等待")) {
            JOptionPane.showMessageDialog(this, "请选择有效模型"); return;
        }
        statusLabel.setText("状态: 验证中...");
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).build();
                String testJson = "{\"model\":\"" + mod + "\",\"messages\":[{\"role\":\"user\",\"content\":\"hi\"}],\"max_tokens\":1}";
                RequestBody body = RequestBody.create(MediaType.parse("application/json"), testJson);
                Request.Builder rb = new Request.Builder().url(apiEndpointField.getText().trim()).post(body);
                setupAuthHeader(rb, apiKeyField.getText().trim());
                try (Response resp = client.newCall(rb.build()).execute()) {
                    boolean ok = resp.isSuccessful();
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText(ok ? "状态: 验证成功" : "状态: 验证失败 (" + resp.code() + ")");
                        JOptionPane.showMessageDialog(this, ok ? "API Key 验证通过！" : "验证失败，请检查配置");
                    });
                }
            } catch (Exception e) { SwingUtilities.invokeLater(() -> statusLabel.setText("状态: 连接异常")); }
        }).start();
    }

    private void refreshAgents() {
        if (apiEndpointField.getText().trim().isEmpty()) { JOptionPane.showMessageDialog(this, "请填写端点"); return; }
        statusLabel.setText("状态: 获取模型中...");
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).build();
                Request.Builder rb = new Request.Builder().url(modelsEndpointField.getText().trim()).get();
                setupAuthHeader(rb, apiKeyField.getText().trim());
                try (Response resp = client.newCall(rb.build()).execute()) {
                    if (resp.isSuccessful()) {
                        JsonObject json = JsonParser.parseString(resp.body().string()).getAsJsonObject();
                        List<String> list = new ArrayList<>();
                        if (json.has("data")) {
                            JsonArray arr = json.getAsJsonArray("data");
                            for (int i = 0; i < arr.size(); i++) {
                                JsonObject m = arr.get(i).getAsJsonObject();
                                if (m.has("id")) list.add(m.get("id").getAsString());
                            }
                        }
                        SwingUtilities.invokeLater(() -> {
                            agentComboBox.removeAllItems();
                            for (String s : list) agentComboBox.addItem(s);
                            
                            AIProvider p = (AIProvider)providerComboBox.getSelectedItem();
                            if (p != null && !p.getName().equals(ADD_NEW_PROVIDER)) {
                                if (agentComboBox.getItemCount() > 0) {
                                    agentComboBox.setSelectedIndex(0);
                                    String selectedModel = agentComboBox.getSelectedItem().toString();
                                    p.setSelectedAgent(selectedModel);
                                    ConfigManager.getInstance().getConfig().setSelectedAgent(selectedModel);
                                }
                                p.setAvailableModels(list);
                                ConfigManager.getInstance().getConfig().setAvailableModels(list);
                                ConfigManager.getInstance().saveConfig();
                            }
                            
                            statusLabel.setText("状态: 已获取 " + list.size() + " 个模型并自动保存");
                        });
                    } else {
                        SwingUtilities.invokeLater(() -> statusLabel.setText("状态: 获取失败 (" + resp.code() + ")"));
                    }
                }
            } catch (Exception e) { 
                SwingUtilities.invokeLater(() -> statusLabel.setText("状态: 获取异常")); 
            }
        }).start();
    }

    private void setupAuthHeader(Request.Builder rb, String key) {
        String type = ((String)authTypeComboBox.getSelectedItem()).toLowerCase();
        if (type.equals("x-api-key")) rb.addHeader("x-api-key", key);
        else if (type.equals("api-key")) rb.addHeader("api-key", key);
        else rb.addHeader("Authorization", "Bearer " + key);
        if (apiEndpointField.getText().contains("anthropic.com")) rb.addHeader("anthropic-version", "2023-06-01");
    }
}
