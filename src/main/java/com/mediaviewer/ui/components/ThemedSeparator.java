package com.mediaviewer.ui.components;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JSeparator;

public class ThemedSeparator extends ThemedComponent {
    protected JSeparator separator;
    
    public ThemedSeparator() {
        super();
        separator = new JSeparator();

        applyTheme();
        setLayout(new BorderLayout());
        add(separator, BorderLayout.CENTER);
    }

    @Override
    protected void applyTheme(){
        separator.setForeground(currentTheme.getBorder());
    }

    @Override
    public void setMaximumSize(Dimension d){
        separator.setMaximumSize(d);
    }
}