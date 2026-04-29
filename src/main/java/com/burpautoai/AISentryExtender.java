package com.burpautoai;

import burp.IBurpExtender;
import burp.IBurpExtenderCallbacks;
import burp.IContextMenuFactory;
import burp.IContextMenuInvocation;
import burp.IExtensionHelpers;
import burp.IHttpRequestResponse;
import burp.ITab;
import com.burpautoai.core.ConfigManager;
import com.burpautoai.model.ScanTask;
import com.burpautoai.ui.LogPanel;
import com.burpautoai.ui.MainPanel;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

public class AISentryExtender
implements IBurpExtender,
IContextMenuFactory {
    private IBurpExtenderCallbacks callbacks;
    private IExtensionHelpers helpers;
    private MainPanel mainPanel;
    private LogPanel logPanel;

    public void registerExtenderCallbacks(IBurpExtenderCallbacks callbacks) {
        this.callbacks = callbacks;
        this.helpers = callbacks.getHelpers();
        callbacks.setExtensionName("AI-Scanner");
        try {
            System.setProperty("file.encoding", "UTF-8");
        }
        catch (Exception exception) {
            callbacks.printOutput("Warning: Failed to set file encoding: " + exception.getMessage());
        }
        ConfigManager.getInstance().init(callbacks);
        SwingUtilities.invokeLater(() -> {
            this.logPanel = new LogPanel();
            this.mainPanel = new MainPanel(callbacks, this.helpers, this.logPanel);
            callbacks.addSuiteTab((ITab)this.mainPanel);
        });
        callbacks.registerContextMenuFactory((IContextMenuFactory)this);
        try {
            String info = "================================\nAI-Scanner v1.0 已加载\n================================";
            callbacks.printOutput(info);
        }
        catch (Exception e) {
            callbacks.printOutput("AI-Scanner v1.0 loaded successfully");
        }
    }

    public List<JMenuItem> createMenuItems(IContextMenuInvocation invocation) {
        ArrayList<JMenuItem> menuItems = new ArrayList<JMenuItem>();
        if (invocation.getInvocationContext() == 0 || invocation.getInvocationContext() == 2 || invocation.getInvocationContext() == 6 || invocation.getInvocationContext() == 5) {
            IHttpRequestResponse[] messages = invocation.getSelectedMessages();
            if (messages == null || messages.length == 0) {
                return menuItems;
            }
            JMenuItem aiScanItem = new JMenuItem("发送到 AI-Scanner");
            aiScanItem.setFont(new Font("微软雅黑", 0, 12));
            aiScanItem.addActionListener(e -> {
                for (IHttpRequestResponse message : messages) {
                    if (this.mainPanel == null) {
                        continue;
                    }
                    this.mainPanel.addRequest(message, ScanTask.ScanMode.CUSTOM);
                }
                this.callbacks.printOutput("已成功发送 " + messages.length + " 个请求到 AI-Scanner 面板");
            });
            menuItems.add(aiScanItem);

            // Add custom prompts sub-menu
            List<com.burpautoai.model.CustomPrompt> customPrompts = ConfigManager.getInstance().getConfig().getCustomPrompts();
            if (customPrompts != null && !customPrompts.isEmpty()) {
                JMenu customMenu = new JMenu("发送到 AI-Scanner (自定义)");
                customMenu.setFont(new Font("微软雅黑", 0, 12));
                for (com.burpautoai.model.CustomPrompt prompt : customPrompts) {
                    JMenuItem customItem = new JMenuItem(prompt.getName());
                    customItem.setFont(new Font("微软雅黑", 0, 12));
                    customItem.addActionListener(e -> {
                        for (IHttpRequestResponse message : messages) {
                            if (this.mainPanel == null) {
                                continue;
                            }
                            this.mainPanel.addRequest(message, ScanTask.ScanMode.CUSTOM, prompt.getPrompt());
                        }
                        this.callbacks.printOutput("已成功使用自定义提示词 \"" + prompt.getName() + "\" 发送 " + messages.length + " 个请求到 AI-Scanner 面板");
                    });
                    customMenu.add(customItem);
                }
                menuItems.add(customMenu);
            }
        }
        return menuItems;
    }
}
