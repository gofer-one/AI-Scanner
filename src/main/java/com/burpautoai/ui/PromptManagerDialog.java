package com.burpautoai.ui;

import com.burpautoai.core.ConfigManager;
import com.burpautoai.model.CustomPrompt;
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
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

public class PromptManagerDialog extends JDialog {
    private JTable promptTable;
    private DefaultTableModel tableModel;
    private ConfigManager.Config config = ConfigManager.getInstance().getConfig();
    private static final Color BG_BLACK = Color.WHITE;
    private static final Color TEXT_GREEN = new Color(33, 37, 41);
    private static final Color PANEL_DARK = new Color(245, 247, 250);
    private static final Color BORDER_GREEN = new Color(210, 214, 220);
    private static final Color INPUT_BG = Color.WHITE;

    public PromptManagerDialog(Frame owner) {
        super(owner, "自定义提示词管理", true);
        this.initUI();
        this.loadPrompts();
        this.setSize(800, 500);
        this.setLocationRelativeTo(owner);
    }

    private void initUI() {
        this.getContentPane().setBackground(BG_BLACK);
        this.setLayout(new BorderLayout(15, 15));
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(BG_BLACK);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        JLabel titleLabel = new JLabel("自定义 Prompt 提示词管理", 0);
        titleLabel.setForeground(TEXT_GREEN);
        titleLabel.setFont(new Font("微软雅黑", 1, 20));
        mainPanel.add((Component)titleLabel, "North");

        Object[] columns = new String[]{"名称", "提示词内容"};
        this.tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        this.promptTable = new JTable(this.tableModel);
        this.promptTable.setBackground(BG_BLACK);
        this.promptTable.setForeground(TEXT_GREEN);
        this.promptTable.setFont(new Font("微软雅黑", 0, 12));
        this.promptTable.setRowHeight(28);
        this.promptTable.setGridColor(BORDER_GREEN);
        this.promptTable.setSelectionBackground(new Color(225, 239, 255));
        this.promptTable.setSelectionForeground(TEXT_GREEN);
        this.promptTable.getTableHeader().setBackground(PANEL_DARK);
        this.promptTable.getTableHeader().setForeground(TEXT_GREEN);
        this.promptTable.getTableHeader().setFont(new Font("微软雅黑", 1, 13));
        this.promptTable.getColumnModel().getColumn(0).setPreferredWidth(150);
        this.promptTable.getColumnModel().getColumn(1).setPreferredWidth(550);

        JScrollPane scrollPane = new JScrollPane(this.promptTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_GREEN, 2));
        mainPanel.add((Component)scrollPane, "Center");

        JPanel buttonPanel = new JPanel(new FlowLayout(1, 15, 10));
        buttonPanel.setBackground(BG_BLACK);
        JButton addButton = this.createButton("新增提示词");
        addButton.addActionListener(e -> this.addPrompt());
        buttonPanel.add(addButton);
        JButton editButton = this.createButton("编辑提示词");
        editButton.addActionListener(e -> this.editPrompt());
        buttonPanel.add(editButton);
        JButton deleteButton = this.createButton("删除提示词");
        deleteButton.addActionListener(e -> this.deletePrompt());
        buttonPanel.add(deleteButton);
        JButton closeButton = this.createButton("关闭");
        closeButton.addActionListener(e -> this.dispose());
        buttonPanel.add(closeButton);
        mainPanel.add((Component)buttonPanel, "South");
        this.add(mainPanel);
    }

    private JButton createButton(String text) {
        final JButton button = new JButton(text);
        button.setBackground(PANEL_DARK);
        button.setForeground(TEXT_GREEN);
        button.setBorder(BorderFactory.createLineBorder(BORDER_GREEN, 2));
        button.setFocusPainted(false);
        button.setFont(new Font("微软雅黑", 1, 13));
        button.setCursor(new Cursor(12));
        button.setPreferredSize(new Dimension(100, 35));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent evt) {
                button.setBackground(new Color(233, 236, 239));
            }
            @Override
            public void mouseExited(MouseEvent evt) {
                button.setBackground(PANEL_DARK);
            }
        });
        return button;
    }

    private void loadPrompts() {
        this.tableModel.setRowCount(0);
        List<CustomPrompt> prompts = this.config.getCustomPrompts();
        for (CustomPrompt prompt : prompts) {
            this.tableModel.addRow(new Object[]{prompt.getName(), prompt.getPrompt()});
        }
    }

    private void addPrompt() {
        CustomPrompt newPrompt = this.showPromptEditDialog(null);
        if (newPrompt != null) {
            this.config.getCustomPrompts().add(newPrompt);
            ConfigManager.getInstance().saveConfig();
            this.loadPrompts();
        }
    }

    private void editPrompt() {
        int selectedRow = this.promptTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "请先选择要编辑的提示词");
            return;
        }
        String name = (String)this.tableModel.getValueAt(selectedRow, 0);
        CustomPrompt existing = null;
        for (CustomPrompt p : this.config.getCustomPrompts()) {
            if (p.getName().equals(name)) {
                existing = p;
                break;
            }
        }
        if (existing != null) {
            CustomPrompt updated = this.showPromptEditDialog(existing);
            if (updated != null) {
                existing.setName(updated.getName());
                existing.setPrompt(updated.getPrompt());
                ConfigManager.getInstance().saveConfig();
                this.loadPrompts();
            }
        }
    }

    private void deletePrompt() {
        int selectedRow = this.promptTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "请先选择要删除的提示词");
            return;
        }
        String name = (String)this.tableModel.getValueAt(selectedRow, 0);
        if (JOptionPane.showConfirmDialog(this, "确定要删除提示词 \"" + name + "\" 吗？", "确认删除", 0) == 0) {
            this.config.getCustomPrompts().removeIf(p -> p.getName().equals(name));
            ConfigManager.getInstance().saveConfig();
            this.loadPrompts();
        }
    }

    private CustomPrompt showPromptEditDialog(CustomPrompt existing) {
        JDialog dialog = new JDialog(this, existing == null ? "新增提示词" : "编辑提示词", true);
        dialog.setLayout(new BorderLayout(15, 15));
        dialog.getContentPane().setBackground(BG_BLACK);
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(BG_BLACK);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);
        
        gbc.gridx = 0; gbc.gridy = 0;
        mainPanel.add(new JLabel("名称:"), gbc);
        
        gbc.gridx = 1; gbc.weightx = 1.0;
        JTextField nameField = new JTextField(existing != null ? existing.getName() : "", 30);
        nameField.setBorder(BorderFactory.createLineBorder(BORDER_GREEN));
        mainPanel.add(nameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        mainPanel.add(new JLabel("提示词内容:"), gbc);

        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1.0; gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        JTextArea promptArea = new JTextArea(existing != null ? existing.getPrompt() : "", 10, 30);
        promptArea.setLineWrap(true);
        promptArea.setWrapStyleWord(true);
        promptArea.setBorder(BorderFactory.createLineBorder(BORDER_GREEN));
        mainPanel.add(new JScrollPane(promptArea), gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout(1, 20, 10));
        buttonPanel.setBackground(BG_BLACK);
        boolean[] confirmed = new boolean[]{false};
        JButton okButton = this.createButton("确定");
        okButton.addActionListener(e -> {
            confirmed[0] = true;
            dialog.dispose();
        });
        buttonPanel.add(okButton);
        JButton cancelButton = this.createButton("取消");
        cancelButton.addActionListener(e -> dialog.dispose());
        buttonPanel.add(cancelButton);

        dialog.add(mainPanel, "Center");
        dialog.add(buttonPanel, "South");
        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);

        if (confirmed[0]) {
            String name = nameField.getText().trim();
            String prompt = promptArea.getText().trim();
            if (name.isEmpty() || prompt.isEmpty()) {
                JOptionPane.showMessageDialog(this, "名称和内容不能为空", "错误", 0);
                return null;
            }
            return new CustomPrompt(name, prompt);
        }
        return null;
    }
}
