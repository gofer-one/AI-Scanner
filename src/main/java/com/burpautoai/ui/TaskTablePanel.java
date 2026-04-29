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
            
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int row = taskTable.rowAtPoint(e.getPoint());
                    if (row >= 0 && row < taskTable.getRowCount()) {
                        taskTable.setRowSelectionInterval(row, row);
                    }
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

        JMenuItem stopItem = createMenuItem("停止分析");
        stopItem.addActionListener(e -> stopSelectedTask());
        popupMenu.add(stopItem);

        // 使用监听器在菜单弹出瞬间实时更新状态
        popupMenu.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {
                int selectedRow = taskTable.getSelectedRow();
                if (selectedRow >= 0) {
                    int taskId = (Integer)tableModel.getValueAt(selectedRow, 0);
                    ScanTask task = findTaskById(taskId);
                    stopItem.setVisible(task != null && task.getStatus() == ScanTask.TaskStatus.SCANNING);
                } else {
                    stopItem.setVisible(false);
                }
            }
            @Override public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {}
            @Override public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {}
        });

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

    private void stopSelectedTask() {
        int selectedRow = this.taskTable.getSelectedRow();
        if (selectedRow >= 0) {
            int taskId = (Integer)this.tableModel.getValueAt(selectedRow, 0);
            ScanTask task = findTaskById(taskId);
            if (task != null && task.getStatus() == ScanTask.TaskStatus.SCANNING) {
                task.setStopped(true);
                this.logPanel.logInfo("用户请求停止任务 #" + taskId);
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
        // 将 owner 设置为 null，使其成为一个独立的窗口，不再强制悬浮在 Burp 主界面上方
        javax.swing.JDialog dialog = new javax.swing.JDialog((java.awt.Window)null, "\u4efb\u52a1 #" + task.getId() + " - AI \u5ba1\u8ba1\u62a5\u544a", java.awt.Dialog.ModalityType.MODELESS);
        dialog.setLayout(new BorderLayout());
        
        // 显式确保它不是“始终置顶”
        dialog.setAlwaysOnTop(false);
        
        javax.swing.JEditorPane reportPane = new javax.swing.JEditorPane();
        reportPane.setEditable(false);
        reportPane.setContentType("text/html");
        reportPane.setBackground(new Color(255, 255, 255));
        reportPane.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        Runnable updateContent = () -> {
            StringBuilder html = new StringBuilder();
            html.append("<html><body style='font-family:微软雅黑,sans-serif; font-size:14pt; color:#212529; line-height:1.6;'>");
            html.append("<div style='background-color:#f8f9fa; padding:15px; border-left:5px solid #17a2b8; margin-bottom:20px;'>");
            html.append("<h2 style='margin:0; color:#0c5460; text-align:center;'>AI-Scanner \u6f0f\u6d1e\u5ba1\u8ba1\u62a5\u544a</h2>");
            html.append("<p style='margin:5px 0 0 0;'>\u76ee\u6807 URL: ").append(task.getUrl()).append("</p>");
            html.append("</div>");

            String aiAnalysis = task.getAiAnalysis();
            if (aiAnalysis != null && !aiAnalysis.isEmpty()) {
                html.append(markdownToHtml(aiAnalysis));
            } else if (task.getStatus() != ScanTask.TaskStatus.FINISHED) {
                html.append("<p style='color:#6c757d; font-style:italic;'>AI \u68c0\u67e5\u4e2d...</p>");
            } else {
                html.append("<p style='color:#dc3545;'>\uff08\u672a\u83b7\u53d6\u5230 AI \u5206\u6790\u5185\u5bb9\uff09</p>");
            }
            html.append("</body></html>");
            reportPane.setText(html.toString());
        };

        updateContent.run();
        // 如果已完成，确保光标在顶部
        if (task.getStatus() == ScanTask.TaskStatus.FINISHED) {
            SwingUtilities.invokeLater(() -> reportPane.setCaretPosition(0));
        }

        // 如果任务还在进行，开启定时器刷新
        final javax.swing.Timer[] timerContainer = new javax.swing.Timer[1];
        if (task.getStatus() != ScanTask.TaskStatus.FINISHED) {
            timerContainer[0] = new javax.swing.Timer(1000, e -> {
                updateContent.run();
                if (task.getStatus() == ScanTask.TaskStatus.FINISHED) {
                    timerContainer[0].stop();
                    reportPane.setCaretPosition(0); // 完成时回到顶部
                }
            });
            timerContainer[0].start();
        }

        JScrollPane scrollPane = new JScrollPane(reportPane);
        scrollPane.setBorder(null);
        dialog.add(scrollPane, "Center");

        JButton closeButton = new JButton("\u5173\u95ed");
        closeButton.addActionListener(e -> {
            if (timerContainer[0] != null) timerContainer[0].stop();
            dialog.dispose();
        });
        
        JPanel bp = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bp.add(closeButton);
        dialog.add(bp, "South");

        dialog.setSize(850, 700);
        // 获取主窗口以进行居中定位，但不建立父子级强制悬浮关系
        Frame mainFrame = (Frame)SwingUtilities.getWindowAncestor(this);
        dialog.setLocationRelativeTo(mainFrame);
        dialog.setVisible(true);
    }

    private String markdownToHtml(String md) {
        if (md == null) return "";
        
        // 预处理：
        // 1. 处理紧密连字符 (AAAAA...)
        String processedMd = md.replaceAll("(.)\\1{50,}", "$1... [重复字符已截断]");
        // 2. 处理带空格的重复模式 (📐 📐 📐 ...)
        processedMd = processedMd.replaceAll("(📐\\s*){2,}", "📐 [多余三角形已过滤]");
        // 3. 处理其他可能的重复词组
        processedMd = processedMd.replaceAll("(.{1,10})\\1{20,}", "$1... [重复模式已截断]");

        String html = processedMd
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replaceAll("(?m)^### (.*)$", "<h3 style='color:#0056b3; border-bottom:1px solid #dee2e6; padding-bottom:5px; margin-top:20px;'>$1</h3>")
            .replaceAll("(?m)^## (.*)$", "<h2 style='color:#0056b3; border-bottom:2px solid #dee2e6; padding-bottom:8px; margin-top:25px;'>$1</h2>")
            .replaceAll("(?m)^# (.*)$", "<h1 style='color:#0056b3; text-align:center;'>$1</h1>")
            .replaceAll("(?m)^\\*\\*(.*)\\*\\*$", "<b>$1</b>")
            .replaceAll("(?m)^-(.*)$", "<li>$1</li>")
            .replace("\n", "<br>");
        
        // 处理代码块
        if (html.contains("```")) {
            // 使用非贪婪匹配，并增加长度限制，防止正则回溯导致的卡顿
            html = html.replaceAll("(?s)```(.*?)```", "<pre style='background-color:#f4f4f4; padding:10px; border:1px solid #ccc; font-family:Consolas,monospace;'>$1</pre>");
        }
        return html;
    }

    private String getTaskDetailSummary(ScanTask task) {
        if (task.getStatus() != ScanTask.TaskStatus.FINISHED) return "-";
        if (task.getErrorMessage() != null && !task.getErrorMessage().isEmpty()) {
            return "AI分析错误";
        }
        return "分析完成";
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
