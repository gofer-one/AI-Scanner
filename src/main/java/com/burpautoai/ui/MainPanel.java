package com.burpautoai.ui;

import burp.IBurpExtenderCallbacks;
import burp.IExtensionHelpers;
import burp.IHttpRequestResponse;
import burp.ITab;
import com.burpautoai.core.AIEngine;
import com.burpautoai.core.ConfigManager;
import com.burpautoai.model.ScanTask;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.nio.charset.StandardCharsets;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainPanel
extends JPanel
implements ITab {
    private IBurpExtenderCallbacks callbacks;
    private IExtensionHelpers helpers;
    private AIEngine aiEngine;
    private LogPanel logPanel;
    private TaskTablePanel taskTablePanel;
    private boolean passiveScanEnabled = false;
    private List<ScanTask> tasks;
    private int taskIdCounter = 1;
    private ExecutorService executorService;
    private static final Color BG_BLACK = Color.WHITE;
    private static final Color TEXT_GREEN = new Color(33, 37, 41);
    private static final Color PANEL_DARK = new Color(245, 247, 250);
    private static final Color BORDER_GREEN = new Color(210, 214, 220);

    public MainPanel(IBurpExtenderCallbacks callbacks, IExtensionHelpers helpers, LogPanel logPanel) {
        this.callbacks = callbacks;
        this.helpers = helpers;
        this.logPanel = logPanel;
        this.aiEngine = new AIEngine(callbacks, helpers, logPanel);
        this.tasks = new ArrayList<ScanTask>();
        this.executorService = Executors.newFixedThreadPool(3);
        this.initUI();
    }

    private void initUI() {
        this.setLayout(new BorderLayout(10, 10));
        this.setBackground(BG_BLACK);
        this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel topPanel = this.createTopPanel();
        this.add((Component)topPanel, "North");
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(BG_BLACK);
        tabbedPane.setForeground(TEXT_GREEN);
        tabbedPane.setFont(new Font("\u5fae\u8f6f\u96c5\u9ed1", 1, 14));
        this.taskTablePanel = new TaskTablePanel(this, this.logPanel);
        tabbedPane.addTab("\u4efb\u52a1\u5217\u8868", this.taskTablePanel);
        tabbedPane.addTab("\u65e5\u5fd7\u7edf\u8ba1", this.logPanel.getUiComponent());
        this.add((Component)tabbedPane, "Center");
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 0));
        panel.setBackground(BG_BLACK);
        panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER_GREEN, 1), BorderFactory.createEmptyBorder(10, 15, 10, 15)));
        
        // 标题
        JLabel titleLabel = new JLabel("AI-Scanner v1.0");
        titleLabel.setForeground(TEXT_GREEN);
        titleLabel.setFont(new Font("\u5fae\u8f6f\u96c5\u9ed1", 1, 16));
        panel.add((Component)titleLabel, "West");

        JButton configButton = new JButton("\u914d\u7f6e\u4e2d\u5fc3");
        configButton.setBackground(PANEL_DARK);
        configButton.setForeground(TEXT_GREEN);
        configButton.setBorder(BorderFactory.createLineBorder(BORDER_GREEN, 1));
        configButton.setFocusPainted(false);
        configButton.setFont(new Font("\u5fae\u8f6f\u96c5\u9ed1", 1, 13));
        configButton.setCursor(new Cursor(12));
        configButton.setPreferredSize(new Dimension(80, 30));
        configButton.addActionListener(e -> {
            Frame parentFrame = (Frame)SwingUtilities.getWindowAncestor(this);
            ConfigDialog dialog = new ConfigDialog(parentFrame, this.callbacks, this.helpers);
            dialog.setVisible(true);
        });
        panel.add((Component)configButton, "East");
        return panel;
    }

    public boolean isPassiveScanEnabled() {
        return this.passiveScanEnabled;
    }

    public void addRequest(IHttpRequestResponse request) {
        this.addRequest(request, ScanTask.ScanMode.CUSTOM);
    }

    public void addRequest(IHttpRequestResponse request, ScanTask.ScanMode scanMode) {
        try {
            if (request == null || request.getRequest() == null || request.getHttpService() == null) {
                this.callbacks.printError("无效的请求：请求或请求内容为空");
                this.logPanel.logError("添加请求失败：无效的请求");
                return;
            }
            byte[] requestBytes = request.getRequest();
            if (requestBytes == null || requestBytes.length == 0) {
                this.logPanel.logError("添加请求失败：请求体为空");
                return;
            }
            String requestStr = new String(requestBytes, StandardCharsets.UTF_8);
            String[] firstLine = requestStr.split("\r?\n")[0].split(" ");
            if (firstLine.length < 2) {
                this.logPanel.logError("添加请求失败：HTTP请求格式无效");
                return;
            }
            String method = firstLine.length > 0 ? firstLine[0] : "UNKNOWN";
            String url = request.getHttpService().getProtocol() + "://" + request.getHttpService().getHost() + (firstLine.length > 1 ? firstLine[1] : "");
            ScanTask task = new ScanTask(this.taskIdCounter++, request, method, url, scanMode);
            this.tasks.add(task);
            this.logPanel.logInfo("[新增] 任务 #" + task.getId() + " | 模式: " + task.getScanMode().getDisplayName());
            SwingUtilities.invokeLater(() -> {
                this.taskTablePanel.addTask(task);
                this.updateStats();
            });
            this.executorService.submit(() -> {
                this.updateStats();
                this.aiEngine.scanRequest(task);
                SwingUtilities.invokeLater(() -> {
                    this.taskTablePanel.refreshTask(task);
                    this.updateStats();
                });
            });
        }
        catch (Exception e) {
            this.callbacks.printError("\u6dfb\u52a0\u8bf7\u6c42\u5931\u8d25: " + e.getMessage());
            this.logPanel.logError("\u6dfb\u52a0\u8bf7\u6c42\u5931\u8d25: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void deleteTask(ScanTask task) {
        this.tasks.remove(task);
        this.taskTablePanel.removeTask(task);
        this.updateStats();
        this.logPanel.logInfo("\u5220\u9664\u4efb\u52a1 #" + task.getId());
    }

    public void rescanTask(ScanTask task) {
        task.setStatus(ScanTask.TaskStatus.PENDING);
        task.setAiAnalysis("");
        task.clearProbeRecords();
        this.logPanel.logInfo("\u91cd\u65b0\u626b\u63cf\u4efb\u52a1 #" + task.getId());
        this.taskTablePanel.refreshTask(task);
        this.updateStats();
        this.executorService.submit(() -> {
            this.updateStats();
            this.aiEngine.scanRequest(task);
            SwingUtilities.invokeLater(() -> {
                this.taskTablePanel.refreshTask(task);
                this.updateStats();
            });
        });
    }

    private void updateStats() {
        int total = this.tasks.size();
        int completed = 0;
        int scanning = 0;
        
        for (ScanTask task : this.tasks) {
            if (task.getStatus() == ScanTask.TaskStatus.FINISHED) {
                ++completed;
            }
            if (task.getStatus() == ScanTask.TaskStatus.SCANNING) {
                ++scanning;
            }
        }
        
        this.logPanel.updateStats(total, completed, scanning);
    }

    public void showTaskDetail(ScanTask task) {
        // AI 报告现已改为双击列表行通过弹窗展示
    }

    public List<ScanTask> getTasks() {
        return this.tasks;
    }

    public String getTabCaption() {
        return "AI-Scanner";
    }

    public Component getUiComponent() {
        return this;
    }

    public void shutdown() {
        if (this.executorService != null && !this.executorService.isShutdown()) {
            this.executorService.shutdownNow();
            try {
                if (!this.executorService.awaitTermination(5L, TimeUnit.SECONDS)) {
                    this.callbacks.printError("ExecutorService did not terminate in time");
                }
            } catch (InterruptedException e) {
                this.executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
