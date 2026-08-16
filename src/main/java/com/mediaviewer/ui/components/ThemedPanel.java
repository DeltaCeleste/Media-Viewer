package com.mediaviewer.ui.components;

import com.mediaviewer.util.Theme;
import com.mediaviewer.util.ThemeUtils;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

public class ThemedPanel extends ThemedComponent {
    private JPanel panel;
    private ThemeUtils.PanelType bgType;
    private Border border = null;
    
    public ThemedPanel(LayoutManager layout, ThemeUtils.PanelType bgType, Border border) {
        super();
        this.border = border;
        this.bgType = bgType;
        
        panel = new JPanel(layout);

        applyTheme();
        setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);
    }

    public ThemedPanel(LayoutManager layout, ThemeUtils.PanelType bgType) {
        super();
        this.bgType = bgType;
        panel.setLayout(layout);

        setLayout(new BorderLayout());
        add(panel);
    }

    public ThemedPanel(LayoutManager layout) {
        super();
        this.bgType = ThemeUtils.PanelType.PANEL;
        panel.setLayout(layout);

        setLayout(new BorderLayout());
        add(panel);
    }
    
    @Override
    protected void applyTheme() {
        panel.setBackground(currentTheme.getBG(bgType));

        setBorderColor(this.border);
    }

    private void setBorderColor(Border b){
        if(b != null){
            if(b instanceof MatteBorder){
                MatteBorder mb = (MatteBorder) b;
                panel.setBorder(new MatteBorder(mb.getBorderInsets(), currentTheme.getBorder()));
            }
        }
    }

    public void setBorder(Border b){
        setBorderColor(b);
    }

    public void setOpaque(boolean b){
        panel.setOpaque(b);
    }
    
    public void addToPanel(Component comp, Object constraints) {
        panel.add(comp, constraints);
    }

    public void addToPanel(Component comp) {
        panel.add(comp);
    }

    public void setPreferredSize(Dimension d){
        panel.setPreferredSize(d);
    }

    public void setCursor(Cursor c){
        panel.setCursor(c);
    }

    public void setBackgroundType(ThemeUtils.PanelType t){
        this.bgType = t;
        panel.setBackground(currentTheme.getBG(bgType));
    }

    public Rectangle getBounds(){
        return this.panel.getBounds();
    }
}