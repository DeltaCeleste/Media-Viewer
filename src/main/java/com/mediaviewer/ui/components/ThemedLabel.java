package com.mediaviewer.ui.components;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.Icon;
import javax.swing.JLabel;

import com.mediaviewer.util.ThemeUtils;

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

        label.setFont(currentTheme.getFont(size, style, ftype));

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

    public void setType(ThemeUtils.TextType t){
        this.type = t;
        applyTheme();
    }

    @Override
    public void setPreferredSize(Dimension d){
        super.setPreferredSize(d);
        label.setPreferredSize(d);
    }

    public void setHorizontalAlignment(int c){
        label.setHorizontalAlignment(c);
    }

    public void setFont(ThemeUtils.FontType type, int style, ThemeUtils.FontSize size){
        label.setFont(currentTheme.getFont(size, style, type));
    }

    public void setForeground(ThemeUtils.TextType type){
        this.type = type;
        this.label.setForeground(currentTheme.getText(type));
    }

    public void setIcon(Icon i){
        this.label.setIcon(i);
    }
}