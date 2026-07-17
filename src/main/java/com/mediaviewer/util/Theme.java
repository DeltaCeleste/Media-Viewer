package com.mediavault.util;

import java.awt.Color;
import java.awt.Font;

/** Paleta y fuentes de MediaVault. */
public final class Theme {

    private Theme() {}
    //#aff2ea
    public static final Color BG      = hex("#041e3b");
    public static final Color PANEL   = hex("#1a1f2e");
    public static final Color ACCENT  = hex("#1e2d4a");
    public static final Color HL      = hex("#e94560"); // Highlight para imagenes
    public static final Color HL2     = hex("#0f3460");
    public static final Color TEXT    = hex("#e8eaf0");
    public static final Color DIM     = hex("#6b7a99");
    public static final Color CARD    = hex("#1e2540");
    public static final Color BORDER  = hex("#2a3560");
    public static final Color INPUT   = hex("#0d1220");
    public static final Color SUCCESS = hex("#4ecca3");
    public static final Color WHITE   = Color.WHITE;

    public static final Font  FONT_BOLD  = new Font("Segoe UI", Font.BOLD,  13);
    public static final Font  FONT_MED   = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font  FONT_SMALL = new Font("Segoe UI", Font.PLAIN,  9);
    public static final Font  FONT_MONO  = new Font("Consolas",  Font.PLAIN, 10);
    public static final String FONT_SYMBOL = "Segoe UI Symbol";
    public static final String FONT_EMOJI  = "Segoe UI Emoji";

    private static Color hex(String h) {
        return Color.decode(h);
    }
}
