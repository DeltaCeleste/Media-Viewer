package com.mediaviewer.ui.components;

import com.mediaviewer.util.Theme;
import com.mediaviewer.util.ThemeUtils;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public class ThemedScroll extends ThemedComponent {
    private JScrollPane scroll;
    private ThemeUtils.PanelType type;
    
    public ThemedScroll(JComponent c, int n1, int n2) {
        super();
        scroll = new JScrollPane(c, n1, n2);
        label.setOpaque(false);

        this.type = ThemeUtils.PanelType.PANEL;

        label.setFont(currentTheme.getFont(size, style, ftype));

        applyTheme();
        setLayout(new BorderLayout());
        add(label, BorderLayout.CENTER);
    }
    
    @Override
    protected void applyTheme() {
        scroll.setBackground(currentTheme.getBG(this.type));
        scroll.getViewport().setBackground(this.type);
    }

    public void setBackground(ThemeUtils.PanelType type){
        this.type = type;
        applyTheme();
    }

    public void setBorder(Border b){
        scroll.setBorder(b);
    }

    public JScrollBar getHorizontalScrollBar(){
        return scroll.getHorizontalScrollBar();
    }
    
}