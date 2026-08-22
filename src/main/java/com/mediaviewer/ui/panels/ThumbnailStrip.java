package com.mediaviewer.ui.panels;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;

import javax.swing.ImageIcon;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import com.mediaviewer.model.MediaFile;
import com.mediaviewer.ui.components.ThemedLabel;
import com.mediaviewer.ui.components.ThemedPanel;
import com.mediaviewer.ui.components.ThemedScroll;
import com.mediaviewer.util.ThemeUtils;

import net.coobird.thumbnailator.Thumbnails;

/**
 * Tira horizontal de miniaturas.
 *  - Se regenera completamente cuando cambia la lista de archivos.
 *  - Cada miniatura carga en un hilo del pool y se pinta via invokeLater.
 *  - El hilo principal solo añade JLabel ya cargados.
 */
public class ThumbnailStrip extends ThemedPanel {

    private static final int TW = 96;
    private static final int TH = 72;

    private final ThemedPanel     inner;
    private final ThemedScroll    scroll;
    private final ExecutorService pool =
        Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "ThumbPool");
            t.setDaemon(true); return t;
        });
    private final AtomicInteger  genCounter = new AtomicInteger(0);

    private List<MediaFile> items     = List.of();
    public int              current   = -1;
    private IntConsumer     onSelect;
    private ThemedPanel[]   cells;

    public ThumbnailStrip(IntConsumer onSelect) {
        super(new BorderLayout(), ThemeUtils.PanelType.BACKGROUND);
        this.onSelect = onSelect;

        inner = new ThemedPanel(new FlowLayout(FlowLayout.LEFT, 3, 4), ThemeUtils.PanelType.BACKGROUND);
        inner.setName("Thumb Inner");

        scroll = new ThemedScroll(inner,
            JScrollPane.VERTICAL_SCROLLBAR_NEVER,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.getHorizontalScrollBar().setUnitIncrement(24);
        scroll.setBorder(null);
        scroll.setBackground(ThemeUtils.PanelType.BACKGROUND);

        // Scroll horizontal con rueda del ratón
        scroll.addMouseWheelListener(e -> {
            JScrollBar bar = scroll.getHorizontalScrollBar();
            bar.setValue(bar.getValue() + e.getWheelRotation() * 30);
        });

        addToPanel(scroll, BorderLayout.CENTER);
        setPreferredSize(new Dimension(0, TH + 40));
    }

    /** 
     * @brief Puebla la tira con una nueva lista. Cancela cargas previas. 
     * @param newItems Lista de los nuevos elementos a cargar en la tira
     * @param initialIdx Indice que referencia a la imagen inicial
     */
    public void populate(List<MediaFile> newItems, int initialIdx) {
        int gen = genCounter.incrementAndGet(); // invalida todos los callbacks previos
        this.items   = newItems;
        this.current = initialIdx;
        this.cells   = new ThemedPanel[newItems.size()];

        inner.removeAll();
        for (int i = 0; i < newItems.size(); i++) {
            ThemedPanel cell = buildCell(i, gen);
            cells[i] = cell;
            inner.addToPanel(cell);
        }
        inner.revalidate();
        inner.repaint();
        highlightCell(initialIdx);
        scrollToCell(initialIdx);
    }

    /**
     * @brief Construye una celda para representar una thumbnail
     * @param idx indice de la file cuya thumbnail se va a representar
     * @param gen El contador que representa el número de la orden actual, si no coincide con el contador actual
     *            la generación se desestima por estar desactualizada
     * @return La celda construida
     */
    private ThemedPanel buildCell(int idx, int gen) {
        MediaFile mf = items.get(idx);

        ThemedPanel cell = new ThemedPanel(new BorderLayout(), ThemeUtils.PanelType.BACKGROUND);
        cell.setPreferredSize(new Dimension(TW + 4, TH + 22));
        cell.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cell.setName("Celda " + idx);

        // Label para imagen
        ThemedLabel imgLbl = new ThemedLabel("…", SwingConstants.CENTER, ThemeUtils.TextType.SECONDARY, ThemeUtils.FontSize.BIG, Font.PLAIN, ThemeUtils.FontType.BASIC);
        imgLbl.setPreferredSize(new Dimension(TW, TH));
        imgLbl.setHorizontalAlignment(SwingConstants.CENTER);

        // Label de nombre
        ThemedLabel nameLbl = new ThemedLabel(truncate(mf.getName(), 14), SwingConstants.CENTER, ThemeUtils.TextType.SECONDARY, ThemeUtils.FontSize.SMALL, Font.PLAIN, ThemeUtils.FontType.BASIC);

        cell.addToPanel(imgLbl, BorderLayout.CENTER);
        cell.addToPanel(nameLbl, BorderLayout.SOUTH);
        cell.setBorder(new EmptyBorder(2, 2, 2, 2));

        // Click
        MouseAdapter click = new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { onSelect.accept(idx); }
        };
        for (Component c : new Component[]{cell, imgLbl, nameLbl})
            c.addMouseListener(click);

        // Carga de miniatura en pool
        if (mf.getType() != MediaFile.MediaType.VIDEO) {
            pool.submit(() -> loadThumb(mf, imgLbl, gen));
        } else {
            pool.submit(() -> loadThumbVideo(mf, imgLbl, gen));
        }

        return cell;
    }

    /**
     * @brief Construye la thumbnail a partir de una imagen o gif
     * @param mf el MediaFile que referencia la imagen
     * @param lbl la etiqueta de texto donde irá la imagen
     * @param gen El contador que representa el número de la orden actual, si no coincide con el contador actual
     *            la generación se desestima por estar desactualizada
     */
    private void loadThumb(MediaFile mf, ThemedLabel lbl, int gen) {
        if (genCounter.get() != gen) return;
        try {
            BufferedImage thumb = Thumbnails.of(mf.getFile())
                .size(TW, TH).keepAspectRatio(true)
                .outputQuality(0.85).asBufferedImage();
            if (genCounter.get() != gen) return;
            ImageIcon icon = new ImageIcon(thumb);
            SwingUtilities.invokeLater(() -> {
                if (genCounter.get() != gen) return;
                lbl.setIcon(icon);
                lbl.setText("");
            });
        } catch (Exception e) {
            if (genCounter.get() != gen) return;
            SwingUtilities.invokeLater(() -> lbl.setText("?"));
        }
    }

    /**
     * @brief Construye la thumbnail a partir de un video
     * @param mf el MediaFile que referencia la imagen
     * @param lbl la etiqueta de texto donde irá la imagen
     * @param gen El contador que representa el número de la orden actual, si no coincide con el contador actual
     *            la generación se desestima por estar desactualizada
     */
    private void loadThumbVideo(MediaFile mf, ThemedLabel lbl, int gen) {
        if (genCounter.get() != gen) return;

        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(mf.getPath())) {
            avutil.av_log_set_level(avutil.AV_LOG_ERROR); //Silenciamos los mensajes en consola
            grabber.start();

            // Salta al segundo indicado (el parámetro está en microsegundos)
            grabber.setTimestamp((long) (1_000_000));

            // Captura un fotograma en ese instante
            Frame frame = grabber.grabImage();

            if (frame != null) {
                // Convierte el fotograma a una imagen Java
                Java2DFrameConverter converter = new Java2DFrameConverter();
                BufferedImage bufferedImage = converter.getBufferedImage(frame); 

                BufferedImage thumb = Thumbnails.of(bufferedImage)
                    .size(TW, TH).keepAspectRatio(true)
                    .outputQuality(0.85).asBufferedImage();
                if (genCounter.get() != gen) return;
                ImageIcon icon = new ImageIcon(thumb);
                SwingUtilities.invokeLater(() -> {
                    if (genCounter.get() != gen) return;
                    lbl.setIcon(icon);
                    lbl.setText("");
                });

            } else {
                throw new Exception();
            }
        } catch (Exception e) {
            if (genCounter.get() != gen) return;
            SwingUtilities.invokeLater(() -> {lbl.setText("🎬"); lbl.setFont(ThemeUtils.FontType.EMOJI, Font.PLAIN, ThemeUtils.FontSize.BIG);});
        }
    }

    /** 
     * @brief Resalta la celda activa y desresalta las demás. También ajusta el scroll a esta imagen 
     * @param idx el índice de la imagen a resaltar
     */
    public void highlight(int idx) {
        this.current = idx;
        highlightCell(idx);
        scrollToCell(idx);
    }

    /**
     * @brief Recorre el array de celdas quitando el resalto a todas y aplicandosela a la seleccionada
     * @param idx el índice de la imagen a resaltar
     */
    private void highlightCell(int idx) {
        if (cells == null) return;
        for (int i = 0; i < cells.length; i++) {
            if (cells[i] == null) continue;
            ThemeUtils.PanelType bg = (i == idx) ? ThemeUtils.PanelType.HIGHLIGHT : ThemeUtils.PanelType.BACKGROUND;
            ThemeUtils.TextType txt = (i == idx) ? ThemeUtils.TextType.TERTIARY   : ThemeUtils.TextType.SECONDARY;
            setAllBg(cells[i], bg, txt);
        }
    }

    /**
     * @brief Establece el color de fondo de un contenedor y sus componentes internas recursivamente
     * @param c El contenedor actual
     * @param bg El color de fondo al que se va a cambiar
     */
    private void setAllBg(ThemedPanel c, ThemeUtils.PanelType bg, ThemeUtils.TextType txt) {
        c.setBackground(bg);
        for (Component ch : c.getComponents()) {
            //ch.setBackground(bg);
            if (ch instanceof ThemedPanel) setAllBg((ThemedPanel)ch, bg, txt);
            if (ch instanceof ThemedLabel) ((ThemedLabel)ch).setForeground(txt);
        }
    }

    /**
     * @brief Mueve el scroll hacia la imagen seleccionada
     * @param idx el índice de la imagen
     */
    private void scrollToCell(int idx) {
        if (cells == null || idx < 0 || idx >= cells.length) return;
        SwingUtilities.invokeLater(() -> {
            ThemedPanel cell = cells[idx];
            if (cell != null) inner.scrollToRect(cell.getBounds());
        });
    }

    /**
     * @brief Trunca una cadena de texto a una cantidad de caracteres dada (acabandola en ...)
     * @param s La cadena a truncar
     * @param max La cantidad máxima de caracteres a dejar
     */
    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    /**
     * @brief Cierre de la pool y los hilos. No necesario por ser demonios, pero más seguro y controlado
     */
    public void shutdown() { pool.shutdownNow(); }
}
