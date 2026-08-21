package com.mediaviewer.ui.components;

import java.awt.Color;
import java.awt.Font;

import javax.swing.JComponent;

import com.mediaviewer.util.Theme;
import com.mediaviewer.util.ThemeManager;

public abstract class ThemedComponent extends JComponent implements ThemeManager.ThemeListener {
    protected Theme currentTheme;
    
    public ThemedComponent() {
        this.currentTheme = ThemeManager.getInstance().getCurrentTheme();
        ThemeManager.getInstance().addListener(this);
    }
    
    @Override
    public void onThemeChanged(Theme newTheme) {
        this.currentTheme = newTheme;
        applyTheme();
        revalidate();
        repaint();
    }
    
    // Cada subclase implementa su propia aplicación de tema
    protected abstract void applyTheme();
    
    // Métodos de conveniencia para subclases
    protected Color getColor(java.util.function.Function<Theme, Color> extractor) {
        return extractor.apply(currentTheme);
    }
    
    protected Font getFont(java.util.function.Function<Theme, Font> extractor) {
        return extractor.apply(currentTheme);
    }
}