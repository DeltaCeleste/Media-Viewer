package com.mediaviewer.ui.panels;

import com.mediaviewer.model.MediaFile;
import com.mediaviewer.util.Theme;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Panel izquierdo: lista de archivos como tabla.
 * Selección → notifica al controlador principal via IntConsumer.
 */
public class FileListPanel extends JPanel {

    private final DefaultTableModel model;
    private final JTable            table;
    private List<MediaFile>         items = List.of();
    private final IntConsumer       onSelect;
    private boolean                 programmaticSelect = false;

    private static final String[] COLS = {"Tipo", "Nombre", "Tamaño"};

    public FileListPanel(IntConsumer onSelect) {
        this.onSelect = onSelect;
        setLayout(new BorderLayout());
        setBackground(Theme.PANEL);

        // ── Cabecera ─────────────────────────────────────────────────────────
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        header.setBackground(Theme.HL2);
        JLabel title = new JLabel("📂  ARCHIVOS");
        title.setForeground(Theme.TEXT);
        title.setFont(new Font("Segoe UI", Font.BOLD, 11));
        header.add(title);
        add(header, BorderLayout.NORTH);

        // ── Tabla ─────────────────────────────────────────────────────────────
        model = new DefaultTableModel(COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        styleTable();

        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || programmaticSelect) return;
            int row = table.getSelectedRow();
            if (row >= 0 && row < items.size()) onSelect.accept(row);
        });

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(Theme.CARD);
        sp.setBackground(Theme.CARD);
        add(sp, BorderLayout.CENTER);
    }

    private void styleTable() {
        table.setBackground(Theme.CARD);
        table.setForeground(Theme.TEXT);
        table.setSelectionBackground(Theme.HL);
        table.setSelectionForeground(Color.WHITE);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        table.setRowHeight(26);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);
        table.getTableHeader().setBackground(Theme.ACCENT);
        table.getTableHeader().setForeground(Theme.TEXT);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 10));
        table.getTableHeader().setBorder(BorderFactory.createEmptyBorder());

        // Anchos de columna
        TableColumnModel cm = table.getColumnModel();
        cm.getColumn(0).setPreferredWidth(42); cm.getColumn(0).setMaxWidth(50);
        cm.getColumn(1).setPreferredWidth(140);
        cm.getColumn(2).setPreferredWidth(68); cm.getColumn(2).setMaxWidth(80);

        // Renderer que aplica colores alternos
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, val, sel, foc, r, c);
                if (!sel) {
                    setBackground(r % 2 == 0 ? Theme.CARD : Theme.ACCENT);
                    setForeground(Theme.TEXT);
                }
                setBorder(new javax.swing.border.EmptyBorder(0, 4, 0, 4));
                return this;
            }
        };
        renderer.setHorizontalAlignment(SwingConstants.LEFT);
        for (int i = 0; i < COLS.length; i++)
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
    }

    /** Llena la tabla con una lista de archivos. Siempre en EDT. */
    public void populate(List<MediaFile> newItems) {
        this.items = newItems;
        model.setRowCount(0);
        for (MediaFile mf : newItems) {
            String icon = switch (mf.getType()) {
                case VIDEO -> "🎬";
                case GIF   -> "🎞";
                default    -> "🖼";
            };
            model.addRow(new Object[]{
                icon + " " + mf.getExt().toUpperCase().replace(".", ""),
                mf.getName(),
                mf.getHumanSize()
            });
        }
    }

    /** Selecciona una fila sin disparar el evento al controlador. */
    public void highlight(int idx) {
        if (idx < 0 || idx >= model.getRowCount()) return;
        programmaticSelect = true;
        table.setRowSelectionInterval(idx, idx);
        table.scrollRectToVisible(table.getCellRect(idx, 0, true));
        programmaticSelect = false;
    }
}
