
package com.mediaviewer.ui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JToggleButton;

public class ThemeToggleButton extends ThemedComponent {
    private JToggleButton btn;
    private boolean isDarkMode = false;

    // Colores del tema claro
    private final Color lightHandle = new Color(255, 200, 0); // Amarillo Sol
    private final Color darkHandle = new Color(200, 210, 230); // Blanco/Azulado Luna

    public ThemeToggleButton(Runnable onSelect) {
        super();
        btn = new JToggleButton();
        btn.setOpaque(false);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(60, 30));

        // Listener para cambiar el estado al hacer clic
        btn.addActionListener(e -> {
            isDarkMode = btn.isSelected();
            onSelect.run();
            repaint();
        });

        // Efecto hover sutil
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
            }
        });

        applyTheme();
        setLayout(new BorderLayout());
        add(btn, BorderLayout.CENTER);
    }

    @Override
    protected void applyTheme(){
        
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        
        // Activar suavizado (Antialiasing) para bordes limpios
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        int padding = 3;
        int diameter = height - (padding * 2);

        // 1. Dibujar el fondo del switch
        Color currentBg = currentTheme.getAccent();
        g2.setColor(currentBg);
        g2.fill(new RoundRectangle2D.Float(0, 0, width, height, height, height));

        // 2. Calcular posición del circulo (deslizador)
        int xPos = isDarkMode ? (width - diameter - padding) : padding;

        // 3. Dibujar el círculo (Sol o Luna)
        Color currentHandle = isDarkMode ? darkHandle : lightHandle;
        g2.setColor(currentHandle);
        g2.fill(new Ellipse2D.Float(xPos, padding, diameter, diameter));

        // 4. Dibujar detalles del icono (Sombra de luna en modo oscuro / Rayos en modo claro)
        if (isDarkMode) {
            // Recorte para formar el cóncavo de la luna
            g2.setColor(currentTheme.getAccent());
            g2.fill(new Ellipse2D.Float(xPos + 5, padding - 1, diameter - 3, diameter - 3));
        } else {
            // Centro del sol (más brillante)
            g2.setColor(new Color(255, 225, 100));
            g2.fill(new Ellipse2D.Float(xPos + 4, padding + 4, diameter - 8, diameter - 8));
        }

        g2.dispose();
    }

    public boolean isDarkMode() {
        return isDarkMode;
    }

    // --- Ejemplo de uso e integración ---
    /*public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Demo Theme Switch");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(400, 200);
            frame.setLayout(new GridBagLayout());

            JPanel panel = new JPanel();
            panel.setPreferredSize(new Dimension(400, 200));
            panel.setBackground(new Color(245, 245, 245));

            ThemeToggleButton toggleBtn = new ThemeToggleButton();
            
            // Evento para alternar los colores del resto de la interfaz
            toggleBtn.addActionListener(e -> {
                if (toggleBtn.isDarkMode()) {
                    panel.setBackground(new Color(30, 30, 30));
                } else {
                    panel.setBackground(new Color(245, 245, 245));
                }
            });

            panel.add(toggleBtn);
            frame.add(panel);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }*/
}