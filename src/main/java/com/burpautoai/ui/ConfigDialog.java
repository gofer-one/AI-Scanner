package com.burpautoai.ui;

import burp.IBurpExtenderCallbacks;
import burp.IExtensionHelpers;
import com.burpautoai.core.ConfigManager;
import com.burpautoai.model.AIProvider;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ConfigDialog extends JDialog {
    private IBurpExtenderCallbacks callbacks;
    private IExtensionHelpers helpers;
    
    private JComboBox<AIProvider> providerComboBox;
    private JTextField apiKeyField;
    private JComboBox<String> authTypeComboBox;
    private JTextField maxTokensField;
    private JTextField apiEndpointField;
    private JTextField modelsEndpointField;
    private JComboBox<String> agentComboBox;
    private javax.swing.JTextArea promptArea;
    private JLabel statusLabel;
    private JButton quickSaveButton;
    private boolean isUpdatingProvider = false;

    private static final String ADD_NEW_PROVIDER = "➕ 新增 AI 接口 / 自定义";

    private static final Color BG_WHITE = Color.WHITE;
    private static final Color TEXT_DARK = new Color(45, 55, 72);
    private static final Color PANEL_LIGHT = new Color(248, 250, 252);
    private static final Color BORDER_COLOR = new Color(226, 232, 240);
    private static final Color ACCENT_GREEN = new Color(56, 161, 105);

    public ConfigDialog(Frame owner, IBurpExtenderCallbacks callbacks, IExtensionHelpers helpers) {
        super(owner, "配置中心", true);
        this.callbacks = callbacks;
        this.helpers = helpers;
        this.initUI();
        SwingUtilities.invokeLater(this::loadConfig);
        this.setSize(850, 900); // 稍微调高以容纳新字段
        this.setLocationRelativeTo(owner);
    }

    private void initUI() {
        this.getContentPane().setBackground(BG_WHITE);
        this.setLayout(new BorderLayout());

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new javax.swing.BoxLayout(contentPanel, javax.swing.BoxLayout.Y_AXIS));
        contentPanel.setBackground(BG_WHITE);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(25, 40, 25, 40));

        JLabel titleLabel = new JLabel("配置中心");
        titleLabel.setForeground(TEXT_DARK);
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 26));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(titleLabel);
        contentPanel.add(javax.swing.Box.createVerticalStrut(20));

        JPanel apiSection = this.createSectionPanel("API 核心设置");
        this.renderApiFields(apiSection);
        contentPanel.add(apiSection);
        contentPanel.add(javax.swing.Box.createVerticalStrut(15));

        JPanel promptSection = this.createSectionPanel("扫描 Prompt 设置");
        this.renderPromptFields(promptSection);
        contentPanel.add(promptSection);
        contentPanel.add(javax.swing.Box.createVerticalStrut(20));

        this.statusLabel = new JLabel("状态: 就绪");
        this.statusLabel.setForeground(ACCENT_GREEN);
        this.statusLabel.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        this.statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(this.statusLabel);
        contentPanel.add(javax.swing.Box.createVerticalStrut(15));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        buttonPanel.setBackground(BG_WHITE);
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JButton[] buttons = {
            createStyledButton("保存配置", true),
            createStyledButton("验证密钥", false),
            createStyledButton("获取模型", false),
            createStyledButton("关闭", false)
        };
        
        buttons[0].addActionListener(e -> saveConfig());
        buttons[1].addActionListener(e -> verifyApiKey());
        buttons[2].addActionListener(e -> refreshAgents());
        buttons[3].addActionListener(e -> dispose());
        
        for (int i = 0; i < buttons.length; i++) {
            buttonPanel.add(buttons[i]);
            if (i < buttons.length - 1) buttonPanel.add(javax.swing.Box.createHorizontalStrut(15));
        }
        contentPanel.add(buttonPanel);

        this.add(new javax.swing.JScrollPane(contentPanel), BorderLayout.CENTER);
    }

    private JPanel createSectionPanel(String title) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(BG_WHITE);
        TitledBorder border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1, true), title, TitledBorder.LEFT, TitledBorder.TOP, 
            new Font("微软雅黑", Font.BOLD, 14), TEXT_DARK
        );
        panel.setBorder(BorderFactory.createCompoundBorder(border, BorderFactory.createEmptyBorder(12, 15, 12, 15)));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    private void renderApiFields(JPanel panel) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 5, 6, 5);

        // AI Provider
        addLabel(panel, "AI 服务商:", 0);
        JPanel providerRow = new JPanel(new BorderLayout(10, 0));
        providerRow.setBackground(BG_WHITE);
        this.providerComboBox = createProviderComboBox();
        this.providerComboBox.addActionListener(e -> { if (!isUpdatingProvider) onProviderChanged(); });
        providerRow.add(this.providerComboBox, BorderLayout.CENTER);
        
        JPanel pBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        pBtns.setBackground(BG_WHITE);
        this.quickSaveButton = createSmallButton("💾 存为端点");
        this.quickSaveButton.addActionListener(e -> quickSaveEndpoint());
        pBtns.add(this.quickSaveButton);
        JButton mBtn = createSmallButton("⚙️ 管理列表");
        mBtn.addActionListener(e -> {
            new EndpointManagerDialog((Frame)SwingUtilities.getWindowAncestor(this)).setVisible(true);
            reloadProviders();
        });
        pBtns.add(mBtn);
        providerRow.add(pBtns, BorderLayout.EAST);
        addComponent(panel, providerRow, 0);

        // API Key
        addLabel(panel, "API Key:", 1);
        this.apiKeyField = createStyledTextField();
        addComponent(panel, this.apiKeyField, 1);

        // Auth Type
        addLabel(panel, "认证方式:", 2);
        this.authTypeComboBox = new JComboBox<>(new String[]{"bearer", "x-api-key", "api-key"});
        this.authTypeComboBox.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        this.authTypeComboBox.setBackground(BG_WHITE);
        addComponent(panel, this.authTypeComboBox, 2);

        // Max Tokens (新增自定义项)
        addLabel(panel, "最大生成长度:", 3);
        this.maxTokensField = createStyledTextField();
        this.maxTokensField.setToolTipText("单次分析允许生成的最大 Token 数 (如 4096, 8192, 16384)");
        addComponent(panel, this.maxTokensField, 3);

        // API Endpoint
        addLabel(panel, "API 端点:", 4);
        this.apiEndpointField = createStyledTextField();
        this.apiEndpointField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) { autoCompleteUrls(); }
        });
        addComponent(panel, this.apiEndpointField, 4);

        // Models Endpoint
        addLabel(panel, "模型列表端点:", 5);
        this.modelsEndpointField = createStyledTextField();
        addComponent(panel, this.modelsEndpointField, 5);

        // Current Model
        addLabel(panel, "当前模型:", 6);
        this.agentComboBox = new JComboBox<>();
        this.agentComboBox.setBackground(BG_WHITE);
        this.agentComboBox.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        addComponent(panel, this.agentComboBox, 6);
    }

    private void renderPromptFields(JPanel panel) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        this.promptArea = createStyledTextArea();
        panel.add(new javax.swing.JScrollPane(this.promptArea), gbc);
    }

    private void addLabel(JPanel p, String text, int row) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = row;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(6, 5, 6, 15);
        JLabel label = new JLabel(text);
        label.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        label.setForeground(TEXT_DARK);
        p.add(label, gbc);
    }

    private void addComponent(JPanel p, java.awt.Component c, int row) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 1; gbc.gridy = row;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 5, 6, 5);
        p.add(c, gbc);
    }

    private void onProviderChanged() {
        AIProvider s = (AIProvider)providerComboBox.getSelectedItem();
        if (s == null) return;
        
        if (s.getName().equals(ADD_NEW_PROVIDER)) {
            apiKeyField.setText("");
            apiEndpointField.setText("");
            modelsEndpointField.setText("");
            maxTokensField.setText("8192");
            authTypeComboBox.setSelectedIndex(0);
            agentComboBox.removeAllItems();
            agentComboBox.addItem("等待填写...");
            quickSaveButton.setVisible(true);
            statusLabel.setText("状态: 请填写新的 API 配置信息");
        } else {
            apiKeyField.setText(ConfigManager.getInstance().getConfig().getApiKey());
            apiEndpointField.setText(s.getApiEndpoint());
            modelsEndpointField.setText(s.getModelsEndpoint());
            maxTokensField.setText(String.valueOf(ConfigManager.getInstance().getConfig().getMaxTokens()));
            authTypeComboBox.setSelectedItem(s.getAuthType().toLowerCase());
            quickSaveButton.setVisible(false);
            statusLabel.setText("状态: 已加载端点 " + s.getName());
        }
    }

    private void loadConfig() {
        ConfigManager.Config config = ConfigManager.getInstance().getConfig();
        isUpdatingProvider = true;
        apiKeyField.setText(config.getApiKey());
        promptArea.setText(config.getPrompt());
        apiEndpointField.setText(config.getApiEndpoint());
        modelsEndpointField.setText(config.getModelsEndpoint());
        maxTokensField.setText(String.valueOf(config.getMaxTokens()));
        authTypeComboBox.setSelectedItem(config.getAuthType().toLowerCase());
        
        agentComboBox.removeAllItems();
        if (config.getAvailableModels() != null && !config.getAvailableModels().isEmpty()) {
            for (String m : config.getAvailableModels()) agentComboBox.addItem(m);
            agentComboBox.setSelectedItem(config.getSelectedAgent());
        } else {
            agentComboBox.addItem("请获取模型列表");
        }
        
        for (int i = 0; i < providerComboBox.getItemCount(); i++) {
            if (providerComboBox.getItemAt(i).getName().equals(config.getSelectedProvider())) {
                providerComboBox.setSelectedIndex(i);
                break;
            }
        }
        isUpdatingProvider = false;
        quickSaveButton.setVisible(providerComboBox.getSelectedItem().toString().equals(ADD_NEW_PROVIDER));
    }

    private void saveConfig() {
        ConfigManager.Config c = ConfigManager.getInstance().getConfig();
        try {
            int mt = Integer.parseInt(maxTokensField.getText().trim());
            c.setMaxTokens(mt);
        } catch (Exception e) {
            c.setMaxTokens(8192);
        }
        
        c.setApiKey(apiKeyField.getText().trim());
        c.setApiEndpoint(apiEndpointField.getText().trim());
        c.setModelsEndpoint(modelsEndpointField.getText().trim());
        c.setPrompt(promptArea.getText().trim());
        c.setAuthType((String)authTypeComboBox.getSelectedItem());
        
        AIProvider p = (AIProvider)providerComboBox.getSelectedItem();
        if (p != null && !p.getName().equals(ADD_NEW_PROVIDER)) {
            c.setSelectedProvider(p.getName());
        }
        Object m = agentComboBox.getSelectedItem();
        if (m != null && !m.toString().contains("获取") && !m.toString().contains("等待")) {
            c.setSelectedAgent(m.toString());
        }
        ConfigManager.getInstance().saveConfig();
        statusLabel.setText("状态: 配置已成功保存");
    }

    private void reloadProviders() {
        isUpdatingProvider = true;
        AIProvider cur = (AIProvider)providerComboBox.getSelectedItem();
        String name = cur != null ? cur.getName() : "";
        providerComboBox.removeAllItems();
        for (AIProvider p : ConfigManager.getInstance().getConfig().getAllProviders()) providerComboBox.addItem(p);
        providerComboBox.addItem(new AIProvider(ADD_NEW_PROVIDER, "", "", "bearer", true));
        for (int i = 0; i < providerComboBox.getItemCount(); i++) {
            if (providerComboBox.getItemAt(i).getName().equals(name)) { providerComboBox.setSelectedIndex(i); break; }
        }
        isUpdatingProvider = false;
    }

    private void quickSaveEndpoint() {
        String name = JOptionPane.showInputDialog(this, "保存当前配置为新端点，请输入名称:", "快速保存", JOptionPane.QUESTION_MESSAGE);
        if (name != null && !name.trim().isEmpty()) {
            AIProvider newP = new AIProvider(
                name.trim(), 
                apiEndpointField.getText().trim(), 
                modelsEndpointField.getText().trim(), 
                (String)authTypeComboBox.getSelectedItem(), 
                true
            );
            ConfigManager.getInstance().getConfig().getCustomProviders().add(newP);
            ConfigManager.getInstance().saveConfig();
            reloadProviders();
            for (int i = 0; i < providerComboBox.getItemCount(); i++) {
                if (providerComboBox.getItemAt(i).getName().equals(name.trim())) {
                    providerComboBox.setSelectedIndex(i);
                    break;
                }
            }
            JOptionPane.showMessageDialog(this, "端点已存入管理列表。");
        }
    }

    private void autoCompleteUrls() {
        String current = apiEndpointField.getText().trim();
        if (current.endsWith("/v1") || current.endsWith("/v1/")) {
            apiEndpointField.setText(com.burpautoai.util.UrlUtil.fixChatEndpoint(current));
            if (modelsEndpointField.getText().trim().isEmpty() || modelsEndpointField.getText().contains("/v1")) {
                modelsEndpointField.setText(com.burpautoai.util.UrlUtil.fixModelsEndpoint(current));
            }
        }
    }

    private void verifyApiKey() {
        Object mod = agentComboBox.getSelectedItem();
        if (mod == null || mod.toString().contains("获取") || mod.toString().contains("等待")) {
            JOptionPane.showMessageDialog(this, "请先填写 API 信息并选择一个有效的模型。");
            return;
        }
        statusLabel.setText("状态: 正在验证...");
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
                        JOptionPane.showMessageDialog(this, ok ? "API Key 验证通过！" : "验证失败，请检查配置。");
                    });
                }
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> statusLabel.setText("状态: 网络连接异常"));
            }
        }).start();
    }

    private void refreshAgents() {
        if (apiEndpointField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "请先填写 API 端点。");
            return;
        }
        statusLabel.setText("状态: 正在获取模型列表...");
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
                            for (int i = 0; i < arr.size(); i++) list.add(arr.get(i).getAsJsonObject().get("id").getAsString());
                        }
                        SwingUtilities.invokeLater(() -> {
                            agentComboBox.removeAllItems();
                            for (String s : list) agentComboBox.addItem(s);
                            statusLabel.setText("状态: 已获取 " + list.size() + " 个模型");
                        });
                    }
                }
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> statusLabel.setText("状态: 获取模型失败"));
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

    private JTextField createStyledTextField() {
        JTextField f = new JTextField();
        f.setPreferredSize(new Dimension(0, 38));
        f.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER_COLOR), BorderFactory.createEmptyBorder(0, 10, 0, 10)));
        f.setFont(new Font("Consolas", Font.PLAIN, 14));
        return f;
    }

    private javax.swing.JTextArea createStyledTextArea() {
        javax.swing.JTextArea a = new javax.swing.JTextArea(6, 0);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setFont(new Font("Consolas", Font.PLAIN, 13));
        a.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        return a;
    }

    private JComboBox<AIProvider> createProviderComboBox() {
        JComboBox<AIProvider> cb = new JComboBox<>();
        cb.setBackground(BG_WHITE);
        cb.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        for (AIProvider p : ConfigManager.getInstance().getConfig().getAllProviders()) cb.addItem(p);
        cb.addItem(new AIProvider(ADD_NEW_PROVIDER, "", "", "bearer", true));
        return cb;
    }

    private JButton createStyledButton(String text, boolean primary) {
        JButton b = new JButton(text);
        b.setPreferredSize(new Dimension(120, 40));
        b.setFocusPainted(false);
        b.setFont(new Font("微软雅黑", Font.BOLD, 14));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setBackground(primary ? ACCENT_GREEN : PANEL_LIGHT);
        b.setForeground(primary ? Color.WHITE : TEXT_DARK);
        b.setBorder(BorderFactory.createLineBorder(primary ? ACCENT_GREEN : BORDER_COLOR));
        return b;
    }

    private JButton createSmallButton(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        b.setBackground(PANEL_LIGHT);
        b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER_COLOR), BorderFactory.createEmptyBorder(3, 8, 3, 8)));
        return b;
    }
}
