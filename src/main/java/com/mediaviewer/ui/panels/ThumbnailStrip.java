package com.mediavault.ui.panels;

import com.mediavault.model.MediaFile;
import com.mediavault.util.Theme;
import net.coobird.thumbnailator.Thumbnails;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;

/**
 * Tira horizontal de miniaturas.
 *  - Se regenera completamente cuando cambia la lista de archivos.
 *  - Cada miniatura carga en un hilo del pool y se pinta via invokeLater.
 *  - El hilo principal solo añade JLabel ya cargados.
 */
public class ThumbnailStrip extends JPanel {

    private static final int TW = 96;
    private static final int TH = 72;

    private final JPanel         inner;
    private final JScrollPane    scroll;
    private final ExecutorService pool =
        Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "ThumbPool");
            t.setDaemon(true); return t;
        });
    private final AtomicInteger  genCounter = new AtomicInteger(0);

    private List<MediaFile> items     = List.of();
    private int             current   = -1;
    private IntConsumer     onSelect;
    private JPanel[]        cells;

    public ThumbnailStrip(IntConsumer onSelect) {
        this.onSelect = onSelect;
        setBackground(Theme.BG);
        setLayout(new BorderLayout());

        inner = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 4));
        inner.setBackground(Theme.BG);

        scroll = new JScrollPane(inner,
            JScrollPane.VERTICAL_SCROLLBAR_NEVER,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.getHorizontalScrollBar().setUnitIncrement(24);
        scroll.setBorder(null);
        scroll.setBackground(Theme.BG);
        scroll.getViewport().setBackground(Theme.BG);

        // Scroll horizontal con rueda del ratón
        scroll.addMouseWheelListener(e -> {
            JScrollBar bar = scroll.getHorizontalScrollBar();
            bar.setValue(bar.getValue() + e.getWheelRotation() * 30);
        });

        add(scroll, BorderLayout.CENTER);
        setPreferredSize(new Dimension(0, TH + 40));
    }

    /** Puebla la tira con una nueva lista. Cancela cargas previas. */
    public void populate(List<MediaFile> newItems, int initialIdx) {
        int gen = genCounter.incrementAndGet(); // invalida todos los callbacks previos
        this.items   = newItems;
        this.current = initialIdx;
        this.cells   = new JPanel[newItems.size()];

        inner.removeAll();
        for (int i = 0; i < newItems.size(); i++) {
            JPanel cell = buildCell(i, gen);
            cells[i] = cell;
            inner.add(cell);
        }
        inner.revalidate();
        inner.repaint();
        highlightCell(initialIdx);
        scrollToCell(initialIdx);
    }

    private JPanel buildCell(int idx, int gen) {
        MediaFile mf = items.get(idx);

        JPanel cell = new JPanel(new BorderLayout());
        cell.setPreferredSize(new Dimension(TW + 4, TH + 22));
        cell.setBackground(Theme.BG);
        cell.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Label para imagen
        JLabel imgLbl = new JLabel("…", SwingConstants.CENTER);
        imgLbl.setForeground(Theme.DIM);
        imgLbl.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        imgLbl.setPreferredSize(new Dimension(TW, TH));
        imgLbl.setHorizontalAlignment(SwingConstants.CENTER);

        // Label de nombre
        JLabel nameLbl = new JLabel(truncate(mf.getName(), 14), SwingConstants.CENTER);
        nameLbl.setFont(new Font("Segoe UI", Font.PLAIN, 8));
        nameLbl.setForeground(Theme.DIM);

        cell.add(imgLbl, BorderLayout.CENTER);
        cell.add(nameLbl, BorderLayout.SOUTH);
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
            imgLbl.setText("🎬");
            imgLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));
        }

        return cell;
    }

    private void loadThumb(MediaFile mf, JLabel lbl, int gen) {
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

    /** Resalta la celda activa y desresalta las demás. */
    public void highlight(int idx) {
        this.current = idx;
        highlightCell(idx);
        scrollToCell(idx);
    }

    private void highlightCell(int idx) {
        if (cells == null) return;
        for (int i = 0; i < cells.length; i++) {
            if (cells[i] == null) continue;
            Color bg = (i == idx) ? Theme.HL : Theme.BG;
            setAllBg(cells[i], bg);
        }
    }

    private void setAllBg(Container c, Color bg) {
        c.setBackground(bg);
        for (Component ch : c.getComponents()) {
            ch.setBackground(bg);
            if (ch instanceof Container) setAllBg((Container)ch, bg);
        }
    }

    private void scrollToCell(int idx) {
        if (cells == null || idx < 0 || idx >= cells.length) return;
        SwingUtilities.invokeLater(() -> {
            JPanel cell = cells[idx];
            if (cell != null) inner.scrollRectToVisible(cell.getBounds());
        });
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    public void shutdown() { pool.shutdownNow(); }
}
