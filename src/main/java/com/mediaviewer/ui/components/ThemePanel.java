package com.mediaviewer.ui.components;

import com.mediaviewer.util.Theme;
import javax.swing.*;
import java.awt.*;

public class ThemedPanel extends ThemedComponent {
    private JPanel panel;
    private Border borde = null;
    
    public ThemedPanel(LayoutManager layout, Border border) {
        super();
        this.borde = border;
        
        panel = new JPanel(layout);
        panel.setBorder(border);

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
        if(borde != null){
            if(borde isntanceof MatteBorder){
                MatteBorder mb = (MatteBorder) borde;
                panel.setBorder(new MatteBorder(mb.getBorderInsets(), currentTheme.getBorder()));
            }
        }
    }
    
    public void add(Component comp, Object constraints) {
        panel.add(comp, constraints);
    }

    public void add(Component comp) {
        panel.add(comp);
    }
}