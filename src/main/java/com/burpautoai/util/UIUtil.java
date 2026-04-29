package com.burpautoai.util;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class UIUtil {
    public static final Color BG_WHITE = Color.WHITE;
    public static final Color TEXT_DARK = new Color(45, 55, 72);
    public static final Color BORDER_COLOR = new Color(226, 232, 240);
    public static final Color PANEL_LIGHT = new Color(248, 250, 252);
    public static final Color ACCENT_GREEN = new Color(56, 161, 105);
    public static final Font FONT_MAIN = new Font("微软雅黑", Font.PLAIN, 13);
    public static final Font FONT_BOLD = new Font("微软雅黑", Font.BOLD, 14);
    public static final Font FONT_MONO = new Font("Consolas", Font.PLAIN, 13);

    public static JTextField createTextField() {
        JTextField f = new JTextField();
        f.setPreferredSize(new Dimension(0, 35));
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR), 
            BorderFactory.createEmptyBorder(0, 10, 0, 10)));
        f.setFont(FONT_MONO);
        return f;
    }

    public static JTextArea createTextArea(int rows) {
        JTextArea a = new JTextArea(rows, 0);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setFont(FONT_MONO);
        a.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        return a;
    }

    public static JButton createButton(String text, boolean primary) {
        JButton b = new JButton(text);
        b.setPreferredSize(new Dimension(120, 38));
        b.setFocusPainted(false);
        b.setFont(FONT_BOLD);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setBackground(primary ? ACCENT_GREEN : PANEL_LIGHT);
        b.setForeground(primary ? Color.WHITE : TEXT_DARK);
        b.setBorder(BorderFactory.createLineBorder(primary ? ACCENT_GREEN : BORDER_COLOR));
        
        b.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!primary) b.setBackground(new Color(237, 242, 247));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                if (!primary) b.setBackground(PANEL_LIGHT);
            }
        });
        return b;
    }
}
