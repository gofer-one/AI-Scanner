package com.burpautoai.ui;

import burp.ITab;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public class LogPanel extends JPanel implements ITab {
    private JTextPane logPane;
    private StyledDocument doc;
    private JLabel totalTasksValue;
    private JLabel aiSuccessValue;
    private JLabel aiFailureValue;
    private JLabel currentScanningValue;

    private static final Color BG_BLACK = Color.WHITE;
    private static final Color TEXT_GREEN = new Color(33, 37, 41);
    private static final Color PANEL_DARK = new Color(245, 247, 250);
    private static final Color BORDER_GREEN = new Color(210, 214, 220);
    private static final Color INPUT_BG = Color.WHITE;

    public LogPanel() {
        this.initUI();
    }

    private void initUI() {
        this.setLayout(new BorderLayout(10, 10));
        this.setBackground(BG_BLACK);
        this.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        JPanel statsPanel = new JPanel(new GridLayout(1, 4, 15, 0));
        statsPanel.setBackground(PANEL_DARK);
        statsPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER_GREEN, 2), BorderFactory.createEmptyBorder(15, 20, 15, 20)));
        
        this.totalTasksValue = this.createStatValueLabel("0");
        this.aiSuccessValue = this.createStatValueLabel("0");
        this.aiFailureValue = this.createStatValueLabel("0");
        this.currentScanningValue = this.createStatValueLabel("0");
        
        statsPanel.add(this.createStatLabel("总分析任务", this.totalTasksValue));
        statsPanel.add(this.createStatLabel("AI 成功回显", this.aiSuccessValue));
        statsPanel.add(this.createStatLabel("AI 调用失败", this.aiFailureValue));
        statsPanel.add(this.createStatLabel("正在处理中", this.currentScanningValue));
        
        this.aiFailureValue.setForeground(new Color(220, 38, 38));
        this.aiSuccessValue.setForeground(new Color(22, 163, 74));

        this.add(statsPanel, "North");
        
        this.logPane = new JTextPane();
        this.logPane.setBackground(INPUT_BG);
        this.logPane.setForeground(TEXT_GREEN);
        this.logPane.setCaretColor(TEXT_GREEN);
        this.logPane.setFont(new Font("微软雅黑", 0, 13));
        this.logPane.setEditable(false);
        this.doc = this.logPane.getStyledDocument();
        
        JScrollPane scrollPane = new JScrollPane(this.logPane);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(BorderFactory.createLineBorder(BORDER_GREEN, 2), "实时日志统计", 1, 2, new Font("微软雅黑", 1, 14), TEXT_GREEN), 
            BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        scrollPane.getViewport().setBackground(INPUT_BG);
        
        this.add(scrollPane, "Center");
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        buttonPanel.setBackground(BG_BLACK);
        JButton clearButton = this.createStyledButton("清空日志");
        clearButton.addActionListener(e -> this.clearLog());
        buttonPanel.add(clearButton);
        
        this.add(buttonPanel, "South");
    }

    private JLabel createStatValueLabel(String value) {
        JLabel label = new JLabel(value, JLabel.CENTER);
        label.setForeground(TEXT_GREEN);
        label.setFont(new Font("微软雅黑", 1, 20));
        return label;
    }

    private JPanel createStatLabel(String title, JLabel valueLabel) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(PANEL_DARK);
        JLabel titleLabel = new JLabel(title, JLabel.CENTER);
        titleLabel.setForeground(new Color(100, 116, 139));
        titleLabel.setFont(new Font("微软雅黑", 0, 12));
        panel.add(titleLabel, "North");
        panel.add(valueLabel, "Center");
        return panel;
    }

    private JButton createStyledButton(String text) {
        final JButton button = new JButton(text);
        button.setBackground(PANEL_DARK);
        button.setForeground(TEXT_GREEN);
        button.setBorder(BorderFactory.createLineBorder(BORDER_GREEN, 2));
        button.setFocusPainted(false);
        button.setFont(new Font("微软雅黑", 1, 13));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(110, 35));
        button.addMouseListener(new MouseAdapter(){
            @Override
            public void mouseEntered(MouseEvent evt) { button.setBackground(new Color(233, 236, 239)); }
            @Override
            public void mouseExited(MouseEvent evt) { button.setBackground(PANEL_DARK); }
        });
        return button;
    }

    public void logInfo(String message) { this.log("INFO", message, new Color(37, 99, 235)); }
    public void logSuccess(String message) { this.log("SUCCESS", message, new Color(22, 163, 74)); }
    public void logWarning(String message) { this.log("WARNING", message, new Color(217, 119, 6)); }
    public void logError(String message) { this.log("ERROR", message, new Color(220, 38, 38)); }
    public void logAI(String message) { this.log("AI", message, new Color(124, 58, 237)); }
    public void logRequest(String message) { this.log("REQUEST", message, new Color(8, 145, 178)); }

    private void log(String level, String message, Color color) {
        SwingUtilities.invokeLater(() -> {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
                String timestamp = sdf.format(new Date());
                String logMessage = timestamp + " | [" + level + "] " + message + "\n";
                SimpleAttributeSet attrs = new SimpleAttributeSet();
                StyleConstants.setForeground(attrs, color);
                this.doc.insertString(this.doc.getLength(), logMessage, attrs);
                this.logPane.setCaretPosition(this.doc.getLength());
            } catch (BadLocationException e) { e.printStackTrace(); }
        });
    }

    public void updateStats(int total, int success, int failure, int scanning) {
        SwingUtilities.invokeLater(() -> {
            if (this.totalTasksValue != null) this.totalTasksValue.setText(String.valueOf(total));
            if (this.aiSuccessValue != null) this.aiSuccessValue.setText(String.valueOf(success));
            if (this.aiFailureValue != null) this.aiFailureValue.setText(String.valueOf(failure));
            if (this.currentScanningValue != null) this.currentScanningValue.setText(String.valueOf(scanning));
        });
    }

    private void clearLog() {
        try {
            this.doc.remove(0, this.doc.getLength());
            this.logInfo("日志已清空");
        } catch (BadLocationException e) { e.printStackTrace(); }
    }

    public String getTabCaption() { return "日志统计"; }
    public Component getUiComponent() { return this; }
}
