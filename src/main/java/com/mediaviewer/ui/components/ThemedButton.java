package com.mediaviewer.ui.components;

import com.mediaviewer.util.Theme;
import javax.swing.*;
import java.awt.*;

public class ThemedButton extends ThemedComponent {
    private JButton button;
    private TextType textType;
    private ButtonType buttonType;
    
    public ThemedButton(String text, TextType textType, FontSize size, int style, ButtonType buttonType) {
        this.textType = textType;
        this.buttonType = buttonType;
        
        button = new JButton(text);
        button.setFocusPainted(false);
        button.setFont(currentTheme.getBodyFont(size, style));

        applyTheme()
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
                button.setBackground(darken(getButtonColor(this.buttonType), 0.1));
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                button.setBackground(currentTheme.getButtonColor(this.buttonType));
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