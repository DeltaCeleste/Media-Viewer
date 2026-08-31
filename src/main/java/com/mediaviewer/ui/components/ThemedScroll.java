package com.mediaviewer.ui.components;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.border.Border;

import com.mediaviewer.util.ThemeUtils;

public class ThemedScroll extends ThemedComponent {
    private final JScrollPane scroll;
    private ThemeUtils.PanelType type;
    
    public ThemedScroll(JComponent c, int n1, int n2) {
        super();
        scroll = new JScrollPane(c, n1, n2);
        scroll.setOpaque(false);

        this.type = ThemeUtils.PanelType.PANEL;

        applyTheme();
        setLayout(new BorderLayout());
        add(scroll, BorderLayout.CENTER);
    }

    public ThemedScroll(JComponent c) {
        super();
        scroll = new JScrollPane(c);
        scroll.setOpaque(false);

        this.type = ThemeUtils.PanelType.PANEL;

        applyTheme();
        setLayout(new BorderLayout());
        add(scroll, BorderLayout.CENTER);
    }
    
    @Override
    protected void applyTheme() {
        scroll.setBackground(currentTheme.getBG(this.type));
        scroll.getViewport().setBackground(currentTheme.getBG(this.type));
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

    @Override
    public void setPreferredSize(Dimension d){
        scroll.setPreferredSize(d);
    }
}