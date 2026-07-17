package com.mediavault.ui.panels;

import com.mediavault.model.MediaFile;
import com.mediavault.util.Theme;
import net.coobird.thumbnailator.Thumbnails;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Panel central — muestra imágenes con zoom/pan, GIFs animados, y placeholder de video.
 *
 * ARQUITECTURA THREAD-SAFE:
 *  - La carga de imagen ocurre en un ExecutorService (hilo de fondo).
 *  - SwingUtilities.invokeLater() entrega el resultado al EDT.
 *  - paintComponent() solo lee campos volátiles.
 */
public class ViewerPanel extends JPanel {

    // ── Estado compartido (volatile = visibilidad entre hilos) ───────────────
    private volatile BufferedImage origImage  = null;
    private volatile BufferedImage scaledCache = null;
    private volatile double        zoom       = 1.0;
    private volatile int           offX       = 0;
    private volatile int           offY       = 0;
    private volatile MediaFile     current    = null;
    private volatile String        infoText   = "Selecciona una carpeta para empezar";
    private volatile boolean       loading    = false;

    // ── GIF animado ──────────────────────────────────────────────────────────
    private JLabel gifLabel = null;
    private Timer  gifTimer = null;

    // ── Carga asíncrona ──────────────────────────────────────────────────────
    private final ExecutorService loader =
        Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "ImageLoader");
            t.setDaemon(true);
            return t;
        });
    private Future<?>       currentLoad = null;
    private final AtomicInteger loadGen = new AtomicInteger(0);

    // ── Spinner ──────────────────────────────────────────────────────────────
    private final String[] SPIN = {"⣾","⣽","⣻","⢿","⡿","⣟","⣯","⣷"};
    private int spinIdx = 0;
    private Timer spinTimer;

    // ── Arrastre (pan) ───────────────────────────────────────────────────────
    private Point panStart = null;
    private int   panOffX0, panOffY0;

    // ── Callback ─────────────────────────────────────────────────────────────
    private JLabel statusLabel;   // inyectado desde fuera

    public ViewerPanel() {
        setBackground(Theme.BG);
        setLayout(new BorderLayout());
        setupMouse();
        setupSpinner();
    }

    public void setStatusLabel(JLabel lbl) { this.statusLabel = lbl; }
    public MediaFile getCurrent()          { return this.current; }

    // ── Carga pública ────────────────────────────────────────────────────────
    /**
     * @brief Carga el archivo pasado
     * @param mf El MediaFile que contiene la referencia al archivo
     */
    public void load(MediaFile mf) {
        // Cancelar carga anterior
        if (currentLoad != null && !currentLoad.isDone())
            currentLoad.cancel(true);
        stopGif();
        origImage   = null;
        scaledCache = null;
        zoom        = 1.0;
        offX = offY = 0;
        current     = mf;
        loadGen.incrementAndGet();
        int gen = loadGen.get();
        removeAll();

        if (mf == null) { repaint(); return; }

        switch (mf.getType()) {
            case VIDEO -> showVideoPlaceholder(mf);
            case GIF   -> loadGif(mf, gen);
            default    -> loadImage(mf, gen);
        }
    }

    // ── Imagen estática ──────────────────────────────────────────────────────
    /**
     * @brief Carga una imagen estática en el visor
     * @param mf la referencia a la imagen
     * @param gen el identificador de orden que debecoincidir con loadGen
     */
    private void loadImage(MediaFile mf, int gen) {
        loading = true;
        spinTimer.start();
        currentLoad = loader.submit(() -> {
            try {
                BufferedImage img = ImageIO.read(mf.getFile());
                if (img == null || loadGen.get() != gen) return;
                // Convertir a RGB si es necesario
                if (img.getType() != BufferedImage.TYPE_INT_RGB &&
                    img.getType() != BufferedImage.TYPE_INT_ARGB) {
                    BufferedImage rgb = new BufferedImage(
                        img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
                    Graphics2D g = rgb.createGraphics();
                    g.setColor(Color.decode("#12151e"));
                    g.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
                    g.drawImage(img, 0, 0, null);
                    g.dispose();
                    img = rgb;
                }
                final BufferedImage finalImg = img;
                SwingUtilities.invokeLater(() -> {
                    if (loadGen.get() != gen) return;
                    loading = false;
                    spinTimer.stop();
                    origImage   = finalImg;
                    scaledCache = null;
                    zoom        = 1.0;
                    offX = offY = 0;
                    infoText    = finalImg.getWidth() + " × " +
                                  finalImg.getHeight() + " px";
                    updateStatus(infoText);
                    repaint();
                });
            } catch (Exception e) {
                if (loadGen.get() != gen) return;
                SwingUtilities.invokeLater(() -> {
                    loading  = false;
                    spinTimer.stop();
                    infoText = "Error: " + e.getMessage();
                    updateStatus(infoText);
                    repaint();
                });
            }
        });
    }

    // ── GIF animado ──────────────────────────────────────────────────────────
    /**
     * @brief Carga una imagen animada (gif) en el visor
     * @param mf la referencia a la imagen
     * @param gen el identificador de orden que debecoincidir con loadGen
     */
    private void loadGif(MediaFile mf, int gen) {
        // Swing soporta GIF animado nativamente via ImageIcon — sin libs extra
        loading = true;
        spinTimer.start();
        loader.submit(() -> {
            try {
                // Verificar que el archivo existe antes de proceder
                if (!mf.getFile().exists() || loadGen.get() != gen) return;
                URL url = mf.getFile().toURI().toURL();
                ImageIcon icon = new ImageIcon(url);
                // Esperar a que el primer frame cargue
                MediaTracker mt = new MediaTracker(new JLabel());
                mt.addImage(icon.getImage(), 0);
                mt.waitForID(0);
                if (loadGen.get() != gen) return;

                SwingUtilities.invokeLater(() -> {
                    if (loadGen.get() != gen) return;
                    loading = false;
                    spinTimer.stop();
                    origImage = null; // no hay BufferedImage estática
                    // Mostrar con JLabel centrado
                    if (gifLabel != null) remove(gifLabel);
                    gifLabel = new JLabel(icon, SwingConstants.CENTER);
                    gifLabel.setBackground(Theme.BG);
                    gifLabel.setOpaque(true);
                    add(gifLabel, BorderLayout.CENTER);
                    revalidate();
                    repaint();
                    int w = icon.getIconWidth();
                    int h = icon.getIconHeight();
                    infoText = "GIF animado  " + w + " × " + h + " px";
                    updateStatus(infoText);
                });
            } catch (Exception e) {
                if (loadGen.get() != gen) return;
                // Fallback: cargar primer frame como imagen estática
                SwingUtilities.invokeLater(() -> {
                    loading  = false;
                    spinTimer.stop();
                    infoText = "GIF (error animación): " + e.getMessage();
                    updateStatus(infoText);
                    repaint();
                });
            }
        });
    }

    /**
     * @brief Elimina el gifLabel actual si existiera
     */
    private void stopGif() {
        if (gifLabel != null) {
            remove(gifLabel);
            gifLabel = null;
            revalidate();
        }
        if (gifTimer != null) { gifTimer.stop(); gifTimer = null; }
    }

    // ── Placeholder de video ──────────────────────────────────────────────────
    /**
     * @brief Carga un placeholder en el panel si el video falla
     * @param mf la referencia al archivo seleccionado para apertura externa
     */
    private void showVideoPlaceholder(MediaFile mf) {
        loading  = false;
        spinTimer.stop();
        origImage = null;
        stopGif();
        infoText = "Video — " + mf.getHumanSize();
        updateStatus(infoText);

        // Panel especial con botón abrir
        JPanel ph = new JPanel(new GridBagLayout());
        ph.setBackground(Theme.BG);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.insets = new Insets(6,0,6,0);

        c.gridy = 0;
        JLabel ico = new JLabel("🎬", SwingConstants.CENTER);
        ico.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 72));
        ico.setForeground(Theme.DIM);
        ph.add(ico, c);

        c.gridy = 1;
        JLabel nameLbl = new JLabel(mf.getName(), SwingConstants.CENTER);
        nameLbl.setFont(Theme.FONT_MED);
        nameLbl.setForeground(Theme.TEXT);
        ph.add(nameLbl, c);

        c.gridy = 2;
        JLabel hint = new JLabel("Doble clic o botón para abrir con el sistema",
                                  SwingConstants.CENTER);
        hint.setFont(Theme.FONT_SMALL);
        hint.setForeground(Theme.DIM);
        ph.add(hint, c);

        c.gridy = 3;
        JButton openBtn = styledButton("▶  Abrir video");
        openBtn.addActionListener(evt -> openExternally(mf.getFile()));
        ph.add(openBtn, c);

        removeAll();
        add(ph, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    // ── paintComponent ────────────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                            RenderingHints.VALUE_RENDER_QUALITY);

        if (loading) {
            drawCentered(g2, SPIN[spinIdx % 8], Theme.HL, 28);
            return;
        }

        BufferedImage img = origImage;
        if (img == null) return;

        int cw = getWidth(), ch = getHeight();
        double fit = Math.min((double)cw / img.getWidth(),
                              (double)ch / img.getHeight());
        double z   = fit * zoom;
        int nw = Math.max(1, (int)(img.getWidth()  * z));
        int nh = Math.max(1, (int)(img.getHeight() * z));

        // Reescalar solo si el tamaño cambió
        if (scaledCache == null ||
            scaledCache.getWidth()  != nw ||
            scaledCache.getHeight() != nh) {
            scaledCache = scaleImage(img, nw, nh);
        }

        int x = (cw - nw) / 2 + offX;
        int y = (ch - nh) / 2 + offY;
        g2.drawImage(scaledCache, x, y, null);
    }

    private BufferedImage scaleImage(BufferedImage src, int w, int h) {
        try {
            return Thumbnails.of(src).forceSize(w, h)
                             .outputQuality(1.0).asBufferedImage();
        } catch (Exception e) {
            // Fallback
            BufferedImage out = new BufferedImage(w, h, src.getType());
            Graphics2D g = out.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                               RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(src, 0, 0, w, h, null);
            g.dispose();
            return out;
        }
    }

    private void drawCentered(Graphics2D g, String text, Color color, int size) {
        g.setColor(color);
        g.setFont(new Font("Segoe UI", Font.PLAIN, size));
        FontMetrics fm = g.getFontMetrics();
        int x = (getWidth()  - fm.stringWidth(text)) / 2;
        int y = (getHeight() + fm.getAscent()) / 2;
        g.drawString(text, x, y);
    }

    // ── Zoom / Pan ────────────────────────────────────────────────────────────
    /**
     * @brief Crea y añade los listenners para permitir manipulación del archivo mediante ratón
     */
    private void setupMouse() {
        MouseAdapter ma = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e) ||
                    SwingUtilities.isMiddleMouseButton(e)) {
                    panStart = e.getPoint();
                    panOffX0 = offX; panOffY0 = offY;
                }
            }
            @Override public void mouseDragged(MouseEvent e) {
                if (panStart != null) {
                    offX = panOffX0 + e.getX() - panStart.x;
                    offY = panOffY0 + e.getY() - panStart.y;
                    repaint();
                }
            }
            @Override public void mouseReleased(MouseEvent e) { panStart = null; }
            @Override public void mouseWheelMoved(MouseWheelEvent e) {
                if (e.getWheelRotation() < 0) zoomIn(); else zoomOut();
            }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
        addMouseWheelListener(ma);
    }

    public void zoomIn()  { zoom = Math.min(zoom * 1.25, 16.0); scaledCache = null; repaint(); updateZoomStatus(); }
    public void zoomOut() { zoom = Math.max(zoom / 1.25, 0.04); scaledCache = null; repaint(); updateZoomStatus(); }
    public void resetView() { zoom = 1.0; offX = offY = 0; scaledCache = null; repaint(); updateZoomStatus(); }

    /**
     * @brief Actualiza el estado con el zoom actual
     */
    private void updateZoomStatus() {
        if (origImage != null)
            updateStatus(origImage.getWidth() + " × " + origImage.getHeight()
                         + " px  |  zoom " + Math.round(zoom * 100) + "%");
    }

    /**
     * @brief Actualiza la etiqueta de estado
     * @param text El texto para la etiqueta
     */
    private void updateStatus(String text) {
        if (statusLabel != null) statusLabel.setText(text);
    }

    // ── Spinner ───────────────────────────────────────────────────────────────
    /**
     * @brief Monta el timer para el spiner (frikada total)
     */
    private void setupSpinner() {
        spinTimer = new Timer(80, evt -> { spinIdx++; repaint(); });
        spinTimer.setRepeats(true);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    /**
     * @brief Abre externamente el archivo seleccionado
     * @param file el archivo en cuestión
     */
    private static void openExternally(File file) {
        try {
            Desktop.getDesktop().open(file);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null,
                "No se pudo abrir: " + ex.getMessage(), "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * @brief Genera un botón con estilo concreto
     * @param text La etiqueta del botón
     * @return el botón
     */
    private static JButton styledButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(Theme.HL);
        b.setForeground(Color.WHITE);
        b.setFont(new Font(Theme.FONT_SYMBOL, Font.BOLD, 11));
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setOpaque(true);
        return b;
    }

    /** 
     * @brief Limpia todo para mostrar estado vacío. 
     */
    public void clear() {
        if (currentLoad != null) currentLoad.cancel(true);
        loadGen.incrementAndGet();
        stopGif();
        origImage = null; scaledCache = null;
        loading = false;
        spinTimer.stop();
        removeAll();
        revalidate();
        repaint();
    }

    public void shutdown() { loader.shutdownNow(); }
}