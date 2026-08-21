package com.mediaviewer.ui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;

import javax.swing.JButton;
import javax.swing.border.Border;

import com.mediaviewer.util.ThemeUtils;

public class ThemedButton extends ThemedComponent {
    private JButton button;
    private ThemeUtils.TextType textType;
    private ThemeUtils.ButtonType buttonType;
    
    public ThemedButton(String text, ThemeUtils.TextType textType, ThemeUtils.FontSize size, int style, ThemeUtils.FontType ftype, ThemeUtils.ButtonType buttonType) {
        super();
        this.textType = textType;
        this.buttonType = buttonType;
        
        button = new JButton(text);
        button.setFocusPainted(false);
        button.setFont(currentTheme.getFont(size, style, ftype));

        applyTheme();
        setLayout(new BorderLayout());
        add(button, BorderLayout.CENTER);
    }
    
    @Override
    protected void applyTheme() {
        button.setBackground(currentTheme.getButtonColor(this.buttonType));
        button.setForeground(currentTheme.getText(this.textType));
        
        // Color de hover (usando un listener extra)
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                button.setBackground(darken(currentTheme.getButtonColor(buttonType), 0.1));
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                button.setBackground(currentTheme.getButtonColor(buttonType));
            }
        });
    }
    
    private Color darken(Color color, double factor) {
        int r = (int) (color.getRed() * (1 - factor));
        int g = (int) (color.getGreen() * (1 - factor));
        int b = (int) (color.getBlue() * (1 - factor));
        return new Color(Math.max(0, r), Math.max(0, g), Math.max(0, b));
    }
    
    public JButton getButton() {
        return button;
    }

    public void setBorder(Border b){
        button.setBorder(b);
    }

    public void setBorderPainted(boolean b){
        button.setBorderPainted(b);
    }

    public void setOpaque(boolean b){
        button.setOpaque(b);
    }

    public void setFocusPainted(boolean b){
        button.setFocusPainted(b);
    }

    public void setPreferredSize(Dimension d){
        button.setPreferredSize(d);
    }

    public void setCursor(Cursor c){
        button.setCursor(c);
    }

    public void setText(String t){
        button.setText(t);
    }
}