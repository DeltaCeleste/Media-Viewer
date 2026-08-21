package com.mediaviewer.util;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class ThemeManager {
    private static ThemeManager instance;
    private static Theme currentTheme = Theme.LIGHT;
    private static final List<ThemeListener> listeners = new ArrayList<>();
    
    private ThemeManager() {}
    
    public static ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }
    
    // Cambiar tema con validación
    public static void setTheme(Theme theme) {
        if (theme == null) {
            throw new IllegalArgumentException("Theme cannot be null");
        }
        
        if (currentTheme != theme) {
            currentTheme = theme;
            notifyListeners();
        }
    }
    
    // Alternar entre claro/oscuro (conveniencia)
    public void toggleLightDark() {
        if (currentTheme == Theme.LIGHT) {
            setTheme(Theme.DARK);
        } else if (currentTheme == Theme.DARK) {
            setTheme(Theme.LIGHT);
        } else {
            // Si está en otro tema, ir a oscuro por defecto
            setTheme(Theme.DARK);
        }
    }
    
    public Theme getCurrentTheme() {
        return currentTheme;
    }
    
    // Obtener un color directamente (atajo)
    public Color getColor(java.util.function.Function<Theme, Color> colorExtractor) {
        return colorExtractor.apply(currentTheme);
    }
    
    // Registro de listeners
    public void addListener(ThemeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    public void removeListener(ThemeListener listener) {
        listeners.remove(listener);
    }
    
    private static void notifyListeners() {
        for (ThemeListener listener : listeners) {
            listener.onThemeChanged(currentTheme);
        }
    }
    
    // Interfaz para observers
    public interface ThemeListener {
        void onThemeChanged(Theme newTheme);
    }
}