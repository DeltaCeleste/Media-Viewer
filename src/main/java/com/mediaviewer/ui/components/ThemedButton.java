package com.mediaviewer.ui.components;

import com.mediaviewer.util.Theme;
import javax.swing.*;
import java.awt.*;

public class ThemedButton extends ThemedComponent {
    private JButton button;
    private String text;
    
    public ThemedButton(String text) {
        this.text = text;

        button = new JButton(text);
        button.setFocusPainted(false);

        setLayout(new BorderLayout());
        add(button, BorderLayout.CENTER);
    }
    
    @Override
    protected void applyTheme() {
        button.setBackground(currentTheme.getSurface());
        button.setForeground(currentTheme.getTextPrimary());
        button.setFont(currentTheme.getBodyFont());
        
        // Bordes redondeados (en Swing puro es más complejo, pero así se indica)
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        
        // Color de hover (usando un listener extra)
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                button.setBackground(darken(currentTheme.getSurface(), 0.1));
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                button.setBackground(currentTheme.getSurface());
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
}