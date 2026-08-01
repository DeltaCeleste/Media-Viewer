package com.mediaviewer.util;

import java.awt.Color;
import java.awt.Font;

public enum TextType    { PRIMARY, SECONDARY, TERTIARY, SUCCESS }
public enum ButtonType  { ACCENT, HIGHLIGHT, HIGHLIGHT2 }
public enum FontSize    { BIG, MED, SMALL }

public enum Theme {
    LIGHT {
        @Override public abstract Color getBackground() { return hex("#DFEFFF"); }
        @Override public abstract Color getPanel()      { return hex("#C3E0FA"); }
        @Override public abstract Color getAccent()     { return hex("#6B7C93"); }
        @Override public abstract Color getHighLight()  { return hex("#0284C7"); }
        @Override public abstract Color getHighLight2() { return hex("#06B6D4"); }
        @Override public abstract Color getText()       { return hex("#0F172A"); }
        @Override public abstract Color getText2()      { return hex("#64748B"); }
        @Override public abstract Color getText3()      { return hex("#FFFFFF"); }
        @Override public abstract Color getBorder()     { return hex("#A0A8C0"); }
        @Override public abstract Color getInput()      { return hex("#EAF0F6"); }
    },
    
    DARK {
        @Override public abstract Color getBackground() { return hex("#0B131F"); }
        @Override public abstract Color getPanel()      { return hex("#142232"); }
        @Override public abstract Color getAccent()     { return hex("#607B96"); }
        @Override public abstract Color getHighLight()  { return hex("#0EA5E9"); }
        @Override public abstract Color getHighLight2() { return hex("#38BDF8"); }
        @Override public abstract Color getText()       { return hex("#F0F6FC"); }
        @Override public abstract Color getText2()      { return hex("#8B9DAE"); }
        @Override public abstract Color getText3()      { return hex("#060D17"); }
        @Override public abstract Color getBorder()     { return hex("#213448"); }
        @Override public abstract Color getInput()      { return hex("#1B2B3E"); }
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

    public Color getText(TextType type){
        Color c;
        switch(type){
            case TextType.PRIMARY:
                c = getText1();
                break;
            case TextType.SECONDARY:
                c = getText2();
                break;
            case TextType.TERTIARY:
                c = getText3();
                break;
            case TextType.SUCCESS:
                c = getSuccess();
                break;
        }
        return c;
    }

    public Color getButtonColor(ButtonType type){
        Color c;
        switch(type){
            case ButtonType.ACCENT:
                c = getAccent();
                break;
            case ButtonType.HIGHLIGHT:
                c = getHighLight();
                break;
            case ButtonType.HIGHLIGHT2:
                c = getHighLight2();
                break;
        }
        return c;
    }
    
    // Métodos concretos con lógica compartida
    public Color getSuccess() { return hex("#05FF09"); }
    public Color getError()   { return hex("#E11D18"); }
    public Color getWarning() { return hex("#E1AD18"); }

    public Font getFontNameDefault() { return "Segoe UI ";       }
    public Font getFontNameSymbol()  { return "Segoe UI Symbol"; }
    public Font getFontNameEmoji()   { return "Segoe UI Emoji";  }
    public Font getFontNameMono()    { return "Consolas";        }

    public Font getFontBigBold()    { return new Font(getFontNameSymbol(), Font.BOLD,  18); }
    public Font getFontBig()        { return new Font(getFontNameSymbol(), Font.PLAIN, 18); }
    public Font getFontMedBold()    { return new Font(getFontNameSymbol(), Font.BOLD,  14); }
    public Font getFontMed()        { return new Font(getFontNameSymbol(), Font.PLAIN, 14); }
    public Font getFontSmallBold()  { return new Font(getFontNameSymbol(), Font.BOLD,  11); }
    public Font getFontSmall()      { return new Font(getFontNameSymbol(), Font.PLAIN, 11); }
    public Font getFontMono()       { return new Font(getFontNameMono(),   Font.PLAIN, 12); }

    public Font getFont(FontSize size, FontStyle style){
        int s;
        switch(size){
            case FontSize.BIG:
                s = 18;
                break;
            case FontSize.MED:
                s = 14;
                break; 
            case FontSize.SMALL:
                s = 11;
                break;  
        }

        return new Font(getFontNameSymbol(), style, s);
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