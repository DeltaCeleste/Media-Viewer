package com.mediaviewer.ui.components;

import javax.swing.*;
import java.awt.*;

public abstract class ThemedComponent extends JComponent implements ThemeManager.ThemeListener {
    protected Theme currentTheme;
    
    public ThemedComponent() {
        this.currentTheme = ThemeManager.getInstance().getCurrentTheme();
        ThemeManager.getInstance().addListener(this);
        applyTheme();
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