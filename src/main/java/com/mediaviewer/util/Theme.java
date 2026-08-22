package com.mediaviewer.util;

import java.awt.Color;
import java.awt.Font;

public enum Theme {
    LIGHT {
        @Override public Color getBackground() { return hex("#DFEFFF"); }
        @Override public Color getPanel()      { return hex("#C3E0FA"); }
        @Override public Color getAccent()     { return hex("#6B7C93"); }
        @Override public Color getHighLight()  { return hex("#0284C7"); }
        @Override public Color getHighLight2() { return hex("#06B6D4"); }
        @Override public Color getText1()       { return hex("#0F172A"); }
        @Override public Color getText2()      { return hex("#64748B"); }
        @Override public Color getText3()      { return hex("#FFFFFF"); }
        @Override public Color getBorder()     { return hex("#A0A8C0"); }
        @Override public Color getInput()      { return hex("#EAF0F6"); }
    },
    
    DARK {
        @Override public Color getBackground() { return hex("#0B131F"); }
        @Override public Color getPanel()      { return hex("#142232"); }
        @Override public Color getAccent()     { return hex("#607B96"); }
        @Override public Color getHighLight()  { return hex("#0EA5E9"); }
        @Override public Color getHighLight2() { return hex("#38BDF8"); }
        @Override public Color getText1()       { return hex("#F0F6FC"); }
        @Override public Color getText2()      { return hex("#8B9DAE"); }
        @Override public Color getText3()      { return hex("#060D17"); }
        @Override public Color getBorder()     { return hex("#213448"); }
        @Override public Color getInput()      { return hex("#1B2B3E"); }
    };
    
    // Métodos abstractos que cada enum debe implementar
    public abstract Color getBackground();
    public abstract Color getPanel();
    public abstract Color getAccent();
    public abstract Color getHighLight();
    public abstract Color getHighLight2();
    public abstract Color getText1();
    public abstract Color getText2();
    public abstract Color getText3();
    public abstract Color getBorder();
    public abstract Color getInput();

    public Color getBG(ThemeUtils.PanelType type){
        Color c;
        switch(type){
            case PANEL:
                c = getPanel();
                break;
            case BACKGROUND:
                c = getBackground();
                break;
            case HIGHLIGHT:
                c = getHighLight();
                break;
            default:
                c = getPanel();
                break;
        }
        return c;
    }

    public Color getText(ThemeUtils.TextType type){
        Color c;
        switch(type){
            case PRIMARY:
                c = getText1();
                break;
            case SECONDARY:
                c = getText2();
                break;
            case TERTIARY:
                c = getText3();
                break;
            case SUCCESS:
                c = getSuccess();
                break;
            case ERROR:
                c = getError();
                break;
            case WARNING:
                c = getWarning();
                break;
            default:
                c = getText1();
                break;
        }
        return c;
    }

    public Color getButtonColor(ThemeUtils.ButtonType type){
        Color c;
        switch(type){
            case ACCENT:
                c = getAccent();
                break;
            case HIGHLIGHT:
                c = getHighLight();
                break;
            case HIGHLIGHT2:
                c = getHighLight2();
                break;
            case PANEL:
                c = getPanel();
                break;
            case BACKGROUND:
                c = getBackground();
                break;
            default:
                c = getAccent();
                break;
        }
        return c;
    }
    
    // Métodos concretos con lógica compartida
    public Color getSuccess() { return hex("#35DD39"); }
    public Color getError()   { return hex("#E11D18"); }
    public Color getWarning() { return hex("#E1AD18"); }

    public String getFontNameDefault() { return "Segoe UI ";       }
    public String getFontNameSymbol()  { return "Segoe UI Symbol"; }
    public String getFontNameEmoji()   { return "Segoe UI Emoji";  }
    public String getFontNameMono()    { return "Consolas";        }

    public Font getFontBigBold()    { return new Font(getFontNameSymbol(), Font.BOLD,  18); }
    public Font getFontBig()        { return new Font(getFontNameSymbol(), Font.PLAIN, 18); }
    public Font getFontMedBold()    { return new Font(getFontNameSymbol(), Font.BOLD,  14); }
    public Font getFontMed()        { return new Font(getFontNameSymbol(), Font.PLAIN, 14); }
    public Font getFontSmallBold()  { return new Font(getFontNameSymbol(), Font.BOLD,  11); }
    public Font getFontSmall()      { return new Font(getFontNameSymbol(), Font.PLAIN, 11); }
    public Font getFontMono()       { return new Font(getFontNameMono(),   Font.PLAIN, 12); }

    public String getFontName(ThemeUtils.FontType type){
        String s;
        switch(type){
            case BASIC:
                s = getFontNameDefault();
                break;
            case SYMBOL:
                s = getFontNameSymbol();
                break;
            case EMOJI:
                s = getFontNameEmoji();
                break; 
            case MONO:
                s = getFontNameMono();
                break; 
            default:
                s = getFontNameDefault();
                break;
        }
        return s;
    }
    
    public Font getFont(ThemeUtils.FontSize size, int style, ThemeUtils.FontType type){
        int s;
        switch(size){
            case ENORMOUS:
                s = 72;
                break;
            case BIG:
                s = 18;
                break;
            case MED:
                s = 14;
                break; 
            case SMALL:
                s = 11;
                break; 
            default:
                s = 14;
                break;  
        }

        return new Font(getFontName(type), style, s);
    }

    // Para debugging
    public String getThemeName() {
        return this.name();
    }

    // Utilidades
    public static Color hex(String h) {
        return Color.decode(h);
    }

    public static String colorToHex(Color color) {
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }
}