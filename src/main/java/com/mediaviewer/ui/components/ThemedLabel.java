package com.mediaviewer.ui.components;

import com.mediaviewer.util.Theme;
import javax.swing.*;
import java.awt.*;

public class ThemedLabel extends ThemedComponent {
    private JLabel label;
    private TextType type;
    // En principio no hay necesidad de guardar estas, ya que no hay cambios
    //private FontSize size;
    //private int style;
    
    public ThemedLabel(String text, TextType type, FontSize size, int style) {
        label = new JLabel(text);
        label.setOpaque(false);

        this.type  = type;
        //this.size  = size;
        //this.style = style;

        label.setFont(currentTheme.getBodyFont(size, style));

        applyTheme();
        setLayout(new BorderLayout());
        add(label, BorderLayout.CENTER);
    }
    
    @Override
    protected void applyTheme() {
        label.setForeground(currentTheme.getText(this.type));        
    }
    
    public void setText(String text) {
        label.setText(text);
    }
}