package com.mediaviewer.ui.components;

import com.mediaviewer.util.Theme;
import com.mediaviewer.util.ThemeUtils;

import javax.swing.*;
import java.awt.*;

public class ThemedLabel extends ThemedComponent {
    private JLabel label;
    private ThemeUtils.TextType type;
    // En principio no hay necesidad de guardar estas, ya que no hay cambios
    //private FontSize size;
    //private int style;
    
    public ThemedLabel(String text, ThemeUtils.TextType type, ThemeUtils.FontSize size, int style, ThemeUtils.FontType ftype) {
        super();
        label = new JLabel(text);
        label.setOpaque(false);

        this.type  = type;
        //this.size  = size;
        //this.style = style;

        label.setFont(currentTheme.getFont(size, style, ftype));

        applyTheme();
        setLayout(new BorderLayout());
        add(label, BorderLayout.CENTER);
    }

    public ThemedLabel(String text, int aligment, ThemeUtils.TextType type, ThemeUtils.FontSize size, int style, ThemeUtils.FontType ftype) {
        super();
        label = new JLabel(text, aligment);
        label.setOpaque(false);

        this.type  = type;

        label.setFont(currentTheme.getBodyFont(size, style, ftype));

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

    public void setPreferredSize(Dimension d){
        label.setPreferredSize(d);
    }

    public void setHorizontalAlignment(int c){
        label.setHorizontalAlignment(c);
    }

    public void setFont(ThemeUtils.FontType type, int style, ThemeUtils.FontSize size){
        label.setFont(new Font(type, style, size));
    }

    public void setForeground(ThemeUtils.TextType type){
        this.type = type;
        label.setForeground(currentTheme.getText(type));
    }
}