package com.mediaviewer.ui.components;

import java.awt.BorderLayout;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;

import com.mediaviewer.util.ThemeUtils;

public class ThemedCheckBox extends ThemedComponent {
    private JCheckBox box;
    private ThemeUtils.TextType type;

    
    public ThemedCheckBox(String text, ThemeUtils.TextType type, ThemeUtils.FontSize size, int style, ThemeUtils.FontType ftype) {
        super();
        this.type = type;
        box = new JCheckBox(text);
        box.setFocusable(false);

        box.setFont(currentTheme.getFont(size, style, ftype));

        applyTheme();
        setLayout(new BorderLayout());
        add(box, BorderLayout.CENTER);
    }
    
    @Override
    protected void applyTheme() {
        box.setBackground(currentTheme.getPanel());
        box.setForeground(currentTheme.getText(this.type));  
    }

    public void addActionListener(ActionListener listener) {
        this.box.addActionListener(listener);
    }

    public boolean isSelected(){
        return box.isSelected();
    }
}