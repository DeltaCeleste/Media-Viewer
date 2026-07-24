package com.mediaviewer.util;

import java.awt.Color;
import java.awt.Font;

/** Paleta y fuentes de MediaVault. */
public final class Theme {

    /**
     * @brief Constructor para la paleta de claros u oscuros
     * @param light true si es modo día y false si es modo noche
     */
    private Theme(boolean light) {}

    public static final Color BG      = hex("#DFEFFF");
    public static final Color PANEL   = hex("#C3E0FA");
    public static final Color ACCENT  = hex("#6B7C93");
    public static final Color HL      = hex("#0284C7"); // Highlight para imagenes
    public static final Color HL2     = hex("#06B6D4");
    public static final Color TEXT    = hex("#0F172A");
    public static final Color TEXT2   = hex("#64748B");
    public static final Color TEXT3   = hex("#FFFFFF");
    public static final Color BORDER  = hex("#A0A8C0");
    public static final Color INPUT   = hex("#EAF0F6");
    public static final Color SUCCESS = hex("#059669");
    public static final Color FAILURE = hex("#E11D48");
    public static final String VIDEO_PLAYER = "#DFEFFF";

    // Fuentes
    public static final String FONT_DEFAULT   = "Segoe UI";
    public static final String FONT_SYMBOL    = "Segoe UI Symbol";
    public static final String FONT_EMOJI     = "Segoe UI Emoji";
    public static final Font  FONT_BIG          = new Font(FONT_SYMBOL, Font.PLAIN,  18);
    public static final Font  FONT_MED_BOLD     = new Font(FONT_SYMBOL, Font.BOLD,  14);
    public static final Font  FONT_SMALL_BOLD   = new Font(FONT_SYMBOL, Font.PLAIN, 11);
    public static final Font  FONT_SMALL        = new Font(FONT_SYMBOL, Font.PLAIN, 11);

    public static final Font  FONT_MONO  = new Font("Consolas",  Font.PLAIN, 10);
    

    private static Color hex(String h) {
        return Color.decode(h);
    }

    private static String colorToHex(Color color) {
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }
}
