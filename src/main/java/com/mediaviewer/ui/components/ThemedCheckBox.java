package com.mediaviewer.ui.components;

import com.mediaviewer.util.Theme;
import com.mediaviewer.util.ThemeUtils;

import javax.swing.*;
import java.awt.*;

public class ThemedCheckBox extends ThemedComponent {
    private JCheckBox box;
    private ThemeUtils.TextType type;

    
    public ThemedCheckBox(String text, ThemeUtils.TextType type, ThemeUtils.FontSize size, int style, ThemeUtils.FontType ftype) {
        super();
        this.type = type;
        box = new JCheckBox(text);

        box.setFont(currentTheme.getFont(size, style, ftype));

        applyTheme();
        setLayout(new BorderLayout());
        add(cb, BorderLayout.CENTER);
    }
    
    @Override
    protected void applyTheme() {
        cb.setBackground(currentTheme.getPanel());
        cb.setForeground(currentTheme.getText(this.type));  
    }
}