package com.mediavault;

import com.mediavault.ui.MainWindow;

import javax.swing.*;

/**
 * MediaVault — punto de entrada.
 *
 * Lanza la ventana principal en el Event Dispatch Thread (EDT),
 * que es el único hilo desde el que Swing permite crear/modificar componentes.
 */
public class Main {
    public static void main(String[] args) {
        // Activar aceleración por GPU en Java2D (útil en Windows)
        System.setProperty("sun.java2d.opengl", "true");
        // Mejor renderizado de fuentes en macOS
        System.setProperty("apple.awt.antialiasing", "true");

        SwingUtilities.invokeLater(() -> {
            MainWindow window = new MainWindow();
            window.setVisible(true);
        });
    }
}
