package com.mediaviewer.ui.components;

import java.awt.BorderLayout;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;

import com.mediaviewer.util.ThemeUtils;

public class ThemedComboBox extends ThemedComponent {
    private JComboBox<String> cb;
    private ThemeUtils.TextType type;

    
    public ThemedComboBox(ThemeUtils.TextType type, ThemeUtils.FontSize size, int style, ThemeUtils.FontType ftype, String... items) {
        super();
        this.type = type;
        cb = new JComboBox<>(items);

        cb.setFont(currentTheme.getFont(size, style, ftype));

        applyTheme();
        setLayout(new BorderLayout());
        add(cb, BorderLayout.CENTER);
    }
    
    @Override
    protected void applyTheme() {
        cb.setBackground(currentTheme.getInput());
        cb.setForeground(currentTheme.getText(this.type));  
    }

    public void addActionListener(ActionListener listener) {
        this.cb.addActionListener(listener);
    }

    public String getSelectedItem(){
        return (String)cb.getSelectedItem();
    }
}