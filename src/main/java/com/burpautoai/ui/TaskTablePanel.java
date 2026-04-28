package com.burpautoai.ui;

import com.burpautoai.model.ScanTask;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

public class TaskTablePanel extends JPanel {
    private MainPanel mainPanel;
    private LogPanel logPanel;
    private JTable taskTable;
    private DefaultTableModel tableModel;
    private Map<Integer, Integer> taskRowMap;
    
    private static final Color BG_BLACK = Color.WHITE;
    private static final Color TEXT_GREEN = new Color(33, 37, 41);
    private static final Color PANEL_DARK = new Color(245, 247, 250);
    private static final Color BORDER_GREEN = new Color(210, 214, 220);
    private static final Color ROW_SELECTED = new Color(225, 239, 255);
    private static final Color RED = new Color(255, 0, 0);

    public TaskTablePanel(MainPanel mainPanel, LogPanel logPanel) {
        this.mainPanel = mainPanel;
        this.logPanel = logPanel;
        this.taskRowMap = new HashMap<Integer, Integer>();
        this.initUI();
    }

    private void initUI() {
        this.setLayout(new BorderLayout(10, 10));
        this.setBackground(BG_BLACK);
        this.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER_GREEN, 2), BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        
        JPanel topPanel = new JPanel(new BorderLayout(10, 5));
        topPanel.setBackground(BG_BLACK);
        JLabel titleLabel = new JLabel("\u626b\u63cf\u4efb\u52a1\u5217\u8868");
        titleLabel.setForeground(TEXT_GREEN);
        titleLabel.setFont(new Font("\u5fae\u8f6f\u96c5\u9ed1", 1, 16));
        topPanel.add((Component)titleLabel, "West");
        
        this.add((Component)topPanel, "North");
        
        Object[] columns = new String[]{"\u7f16\u53f7", "\u65b9\u6cd5", "URL", "AI\u72b6\u6001", "\u8be6\u7ec6\u4fe1\u606f"};
        this.tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        this.taskTable = new JTable(this.tableModel);
        this.taskTable.setBackground(BG_BLACK);
        this.taskTable.setForeground(TEXT_GREEN);
        this.taskTable.setGridColor(BORDER_GREEN);
        this.taskTable.setSelectionBackground(ROW_SELECTED);
        this.taskTable.setSelectionForeground(TEXT_GREEN);
        this.taskTable.setFont(new Font("\u5fae\u8f6f\u96c5\u9ed1", 0, 13));
        this.taskTable.setRowHeight(32);
        this.taskTable.setShowGrid(true);
        this.taskTable.setIntercellSpacing(new Dimension(2, 2));
        
        JTableHeader header = this.taskTable.getTableHeader();
        header.setBackground(PANEL_DARK);
        header.setForeground(TEXT_GREEN);
        header.setFont(new Font("\u5fae\u8f6f\u96c5\u9ed1", 1, 14));
        header.setBorder(BorderFactory.createLineBorder(BORDER_GREEN, 2));
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 35));
        
        this.taskTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        this.taskTable.getColumnModel().getColumn(1).setPreferredWidth(60);
        this.taskTable.getColumnModel().getColumn(2).setPreferredWidth(450);
        this.taskTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        this.taskTable.getColumnModel().getColumn(4).setPreferredWidth(400);
        
        this.taskTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(BG_BLACK);
                    c.setForeground(TEXT_GREEN);
                } else {
                    c.setBackground(ROW_SELECTED);
                    c.setForeground(TEXT_GREEN);
                }
                this.setHorizontalAlignment(0);
                return c;
            }
        });
        
        this.taskTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showSelectedTaskReport();
                }
            }
        });

        JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.setBackground(PANEL_DARK);
        popupMenu.setBorder(BorderFactory.createLineBorder(BORDER_GREEN, 2));
        
        JMenuItem viewReportItem = createMenuItem("\u67e5\u770b AI \u5ba1\u8ba1\u62a5\u544a");
        viewReportItem.setFont(new Font("\u5fae\u8f6f\u96c5\u9ed1", 1, 13));
        viewReportItem.addActionListener(e -> showSelectedTaskReport());
        popupMenu.add(viewReportItem);
        popupMenu.addSeparator();

        JMenuItem rescanItem = createMenuItem("\u91cd\u65b0\u626b\u63cf");
        rescanItem.addActionListener(e -> rescanSelectedTask());
        popupMenu.add(rescanItem);
        
        JMenuItem deleteItem = createMenuItem("\u5220\u9664\u4efb\u52a1");
        deleteItem.addActionListener(e -> deleteSelectedTask());
        popupMenu.add(deleteItem);
        
        popupMenu.addSeparator();
        
        JMenuItem copyUrlItem = createMenuItem("\u590d\u5236URL");
        copyUrlItem.addActionListener(e -> copySelectedUrl());
        popupMenu.add(copyUrlItem);
        
        this.taskTable.setComponentPopupMenu(popupMenu);
        
        JScrollPane scrollPane = new JScrollPane(this.taskTable);
        scrollPane.getViewport().setBackground(BG_BLACK);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_GREEN, 2));
        this.add((Component)scrollPane, "Center");
    }

    public void addTask(ScanTask task) {
        String detail = getTaskDetailSummary(task);
        Object[] row = new Object[]{task.getId(), task.getMethod(), task.getUrl(), task.getStatus().getDisplayName(), detail};
        this.tableModel.addRow(row);
        this.taskRowMap.put(task.getId(), this.tableModel.getRowCount() - 1);
    }

    public void refreshTask(ScanTask task) {
        Integer rowIndex = this.taskRowMap.get(task.getId());
        if (rowIndex != null && rowIndex >= 0 && rowIndex < this.tableModel.getRowCount()) {
            String detail = getTaskDetailSummary(task);
            this.tableModel.setValueAt(task.getStatus().getDisplayName(), rowIndex, 3);
            this.tableModel.setValueAt(detail, rowIndex, 4);
            this.tableModel.fireTableRowsUpdated(rowIndex, rowIndex);
        }
    }

    private ScanTask findTaskById(int taskId) {
        for (ScanTask task : this.mainPanel.getTasks()) {
            if (task.getId() != taskId) continue;
            return task;
        }
        return null;
    }

    private JMenuItem createMenuItem(String text) {
        JMenuItem item = new JMenuItem(text);
        item.setBackground(PANEL_DARK);
        item.setForeground(TEXT_GREEN);
        item.setFont(new Font("\u5fae\u8f6f\u96c5\u9ed1", 0, 13));
        return item;
    }

    public void removeTask(ScanTask task) {
        Integer rowIndex = this.taskRowMap.get(task.getId());
        if (rowIndex != null) {
            this.tableModel.removeRow(rowIndex);
            rebuildRowMap();
        }
    }

    private void rebuildRowMap() {
        this.taskRowMap.clear();
        for (int i = 0; i < this.tableModel.getRowCount(); ++i) {
            int taskId = (Integer)this.tableModel.getValueAt(i, 0);
            this.taskRowMap.put(taskId, i);
        }
    }

    private void rescanSelectedTask() {
        int selectedRow = this.taskTable.getSelectedRow();
        if (selectedRow >= 0) {
            int taskId = (Integer)this.tableModel.getValueAt(selectedRow, 0);
            ScanTask task = findTaskById(taskId);
            if (task != null) {
                this.mainPanel.rescanTask(task);
            }
        }
    }

    private void showSelectedTaskReport() {
        int selectedRow = this.taskTable.getSelectedRow();
        if (selectedRow >= 0) {
            int taskId = (Integer)this.tableModel.getValueAt(selectedRow, 0);
            ScanTask task = findTaskById(taskId);
            if (task != null) {
                showReportDialog(task);
            }
        }
    }

    private void showReportDialog(ScanTask task) {
        Frame owner = (Frame)SwingUtilities.getWindowAncestor(this);
        // 将第三个参数设置为 false，使其成为非模态对话框
        javax.swing.JDialog dialog = new javax.swing.JDialog(owner, "\u4efb\u52a1 #" + task.getId() + " - AI \u5ba1\u8ba1\u62a5\u544a", false);
        dialog.setLayout(new BorderLayout());
        
        javax.swing.JTextArea reportArea = new javax.swing.JTextArea();
        reportArea.setEditable(false);
        reportArea.setFont(new Font("Consolas", 0, 14));
        reportArea.setBackground(new Color(248, 249, 250));
        reportArea.setForeground(new Color(33, 37, 41));
        reportArea.setLineWrap(true);
        reportArea.setWrapStyleWord(true);
        reportArea.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        StringBuilder report = new StringBuilder();
        report.append("====================================================\n");
        report.append("  AI-Scanner \u6f0f\u6d1e\u5ba1\u8ba1\u62a5\u544a  \n");
        report.append("====================================================\n\n");
        report.append("\u76ee\u6807 URL: ").append(task.getUrl()).append("\n");
        report.append("\u8bf7\u6c42\u65b9\u6cd5: ").append(task.getMethod()).append("\n\n");
        
        report.append("====================================================\n");
        report.append("  AI \u667a\u80fd\u5ba1\u8ba1\u8be6\u60c5 / AI Full Response  \n");
        report.append("====================================================\n\n");

        String aiAnalysis = task.getAiAnalysis();
        if (aiAnalysis != null && !aiAnalysis.isEmpty()) {
            report.append(aiAnalysis).append("\n");
        } else if (task.getStatus() != ScanTask.TaskStatus.FINISHED) {
            report.append("\uff08\u6b63\u5728\u7b49\u5f85 AI \u8f93\u51fa...\uff09\n");
        } else {
            report.append("\uff08\u672a\u83b7\u53d6\u5230 AI \u5206\u6790\u5185\u5bb9\uff09\n");
        }

        reportArea.setText(report.toString());
        reportArea.setCaretPosition(0);

        JScrollPane scrollPane = new JScrollPane(reportArea);
        scrollPane.setBorder(null);
        dialog.add(scrollPane, "Center");

        JButton closeButton = new JButton("\u5173\u95ed");
        closeButton.addActionListener(e -> dialog.dispose());
        JPanel bp = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bp.add(closeButton);
        dialog.add(bp, "South");

        dialog.setSize(800, 650);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    private String getTaskDetailSummary(ScanTask task) {
        if (task.getStatus() != ScanTask.TaskStatus.FINISHED) return "-";
        return "\u5206\u6790\u5df2\u5b8c\u6210";
    }

    private void deleteSelectedTask() {
        int selectedRow = this.taskTable.getSelectedRow();
        if (selectedRow >= 0) {
            int taskId = (Integer)this.tableModel.getValueAt(selectedRow, 0);
            ScanTask task = findTaskById(taskId);
            if (task != null && JOptionPane.showConfirmDialog(this, "\u786e\u5b9a\u8981\u5220\u9664\u4efb\u52a1 #" + taskId + " \u5417\uff1f", "\u786e\u8ba4\u5220\u9664", 0) == 0) {
                this.mainPanel.deleteTask(task);
            }
        }
    }

    private void copySelectedUrl() {
        int selectedRow = this.taskTable.getSelectedRow();
        if (selectedRow >= 0) {
            String url = (String)this.tableModel.getValueAt(selectedRow, 2);
            StringSelection selection = new StringSelection(url);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
            this.logPanel.logInfo("\u5df2\u590d\u5236URL\u5230\u526a\u8d34\u677f");
        }
    }
}
