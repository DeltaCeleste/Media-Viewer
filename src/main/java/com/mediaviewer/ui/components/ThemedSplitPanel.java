package com.mediaviewer.ui.components;

import java.awt.BorderLayout;
import java.awt.Container;

import javax.swing.JSplitPane;
import javax.swing.border.Border;

import com.mediaviewer.util.ThemeUtils;

public class ThemedSplitPanel extends ThemedComponent {
    private final JSplitPane split;
    private ThemeUtils.PanelType bg;

    public ThemedSplitPanel(int i, Container c1, Container c2){
        super();
        split = new JSplitPane(i, c1, c2);

        bg = ThemeUtils.PanelType.PANEL;

        applyTheme();
        setLayout(new BorderLayout());
        add(split, BorderLayout.CENTER);
    }

    @Override
    protected void applyTheme(){
        split.setBackground(currentTheme.getBG(bg));
    }

    public void setDividerLocation(int i){
        split.setDividerLocation(i);
    }

    public void setDividerSize(int i){
        split.setDividerSize(i);
    }

    @Override
    public void setBorder(Border b){
        split.setBorder(b);
    }

    public void setBackground(ThemeUtils.PanelType type){
        bg = type;
        split.setBackground(currentTheme.getBG(type));
    }

    public void setResizeWeight(double d){
        split.setResizeWeight(d);
    }

    public void setOneTouchExpandable(boolean b){
        split.setOneTouchExpandable(b);
    }
}