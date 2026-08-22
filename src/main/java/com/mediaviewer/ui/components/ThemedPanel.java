package com.mediaviewer.ui.components;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.LayoutManager;
import java.awt.Rectangle;

import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.MatteBorder;

import com.mediaviewer.util.ThemeUtils;

public class ThemedPanel extends ThemedComponent {
    protected JPanel panel;
    private ThemeUtils.PanelType bgType;
    private Border border = null;
    
    public ThemedPanel(LayoutManager layout, ThemeUtils.PanelType bgType, Border border) {
        super();
        this.border = border;
        this.bgType = bgType;
        initPanel(layout);

        applyTheme();
        setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);
    }

    public ThemedPanel(LayoutManager layout, ThemeUtils.PanelType bgType) {
        super();
        this.bgType = bgType;
        initPanel(layout);

        applyTheme();
        setLayout(new BorderLayout());
        add(panel);
    }

    public ThemedPanel(LayoutManager layout) {
        super();
        this.bgType = ThemeUtils.PanelType.PANEL;
        initPanel(layout);

        applyTheme();
        setLayout(new BorderLayout());
        add(panel);
    }

    private void initPanel(LayoutManager layout){
        panel = new JPanel(layout) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // Delegamos el pintado a la clase hija
                onDraw(g); 
            }
        };
    }
    
    // Método opcional
    protected void onDraw(Graphics g){
        return;
    }

    public void repaint() {
        if (panel != null) {
            panel.repaint();
        }
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
        this.panel.add(comp, constraints);
    }

    public void addToPanel(Component comp) {
        this.panel.add(comp);
    }

    @Override
    public void setPreferredSize(Dimension d){
        panel.setPreferredSize(d);
    }

    @Override
    public void setCursor(Cursor c){
        panel.setCursor(c);
    }

    public void setBackground(ThemeUtils.PanelType t){
        this.bgType = t;
        this.panel.setBackground(currentTheme.getBG(t));
    }

    @Override
    public Component[] getComponents(){
        return this.panel.getComponents();
    }

    @Override
    public void setName(String s){
        super.setName(s);
        this.panel.setName(s);
    }

    @Override
    public void removeAll(){
        this.panel.removeAll();
    }

    public void scrollToRect(Rectangle r){
        this.panel.scrollRectToVisible(r);
    }
}