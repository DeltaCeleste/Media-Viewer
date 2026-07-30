package com.mediaviewer.ui.components;

import com.mediaviewer.util.Theme;
import javax.swing.*;
import java.awt.*;

public class ThemedPanel extends ThemedComponent {
    private JPanel panel;
    
    public ThemedPanel(LayoutManager layout, boolean border) {
        super();
        panel = new JPanel(layout);
        //if(border == true) panel.setBorder();

        applyTheme();
        setLaoyut(new BorderLayout());
        add(panel, BorderLayout.CENTER);
    }

    public ThemedPanel(Layout layout) {
        super();
        panel.setLayout(layout);

        setLaoyut();
        add(panel);
    }
    
    @Override
    protected void applyTheme() {
        panel.setBackground(this.currentTheme.getPanel());
        /*panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(currentTheme.getDivider(), 1),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));*/
    }
    
    public void add(Component comp, Object constraints) {
        panel.add(comp, constraints);
    }

    public void add(Component comp) {
        panel.add(comp);
    }
}