package com.mediavault.ui.panels;

import com.mediavault.engine.MetadataEngine;
import com.mediavault.model.MediaFile;
import com.mediavault.util.Theme;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.*;
import java.util.function.Consumer;

/**
 * Panel derecho: árbol de metadatos + campos editables + botón guardar.
 *
 * La lectura de metadatos ocurre en SwingWorker para no bloquear el EDT.
 */
public class MetadataPanel extends JPanel {

    private final DefaultTreeModel treeModel;
    private final JTree            tree;
    private final JTextField       nameField;
    private final Map<String, JTextField> editFields = new LinkedHashMap<>();
    private final JLabel           statusLbl;
    private final Consumer<MediaFile> onSaved;

    private MediaFile current;

    private static final String[] EDITABLE_FIELDS = {
        "ImageDescription", "Artist", "Copyright", "Software"
    };
    private static final Map<String, String> FIELD_LABELS = Map.of(
        "ImageDescription", "Descripción",
        "Artist",           "Artista",
        "Copyright",        "Copyright",
        "Software",         "Software"
    );

    public MetadataPanel(Consumer<MediaFile> onSaved) {
        this.onSaved = onSaved;
        setLayout(new BorderLayout());
        setBackground(Theme.PANEL);
        setPreferredSize(new Dimension(290, 0));

        // ── Cabecera ─────────────────────────────────────────────────────────
        JPanel hdr = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        hdr.setBackground(Theme.HL2);
        JLabel title = new JLabel("📋  METADATOS");
        title.setForeground(Theme.TEXT);
        title.setFont(new Font("Segoe UI", Font.BOLD, 11));
        hdr.add(title);
        add(hdr, BorderLayout.NORTH);

        // ── Panel central (scroll) ───────────────────────────────────────────
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBackground(Theme.PANEL);

        // Nombre
        JPanel nameRow = row("Nombre:");
        nameField = darkField();
        nameRow.add(nameField);
        center.add(nameRow);
        center.add(vgap(2));

        // Árbol de metadatos
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        treeModel = new DefaultTreeModel(root);
        tree = new JTree(treeModel);
        styleTree();
        tree.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) copySelectedValue();
            }
        });
        JScrollPane sp = new JScrollPane(tree);
        sp.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        sp.setBackground(Theme.CARD);
        sp.getViewport().setBackground(Theme.CARD);
        sp.setPreferredSize(new Dimension(0, 260));
        center.add(sp);
        center.add(vgap(4));

        // Separador
        JSeparator sep = new JSeparator();
        sep.setForeground(Theme.BORDER);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        center.add(sep);
        center.add(vgap(4));

        // Campos editables
        JLabel editTitle = new JLabel("  Editar etiquetas");
        editTitle.setForeground(Theme.DIM);
        editTitle.setFont(new Font("Segoe UI", Font.ITALIC, 9));
        editTitle.setAlignmentX(0);
        center.add(editTitle);
        center.add(vgap(2));

        for (String key : EDITABLE_FIELDS) {
            JPanel r = row(FIELD_LABELS.get(key) + ":");
            JTextField tf = darkField();
            editFields.put(key, tf);
            r.add(tf);
            center.add(r);
            center.add(vgap(1));
        }

        JScrollPane centerScroll = new JScrollPane(center,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        centerScroll.setBorder(null);
        centerScroll.setBackground(Theme.PANEL);
        centerScroll.getViewport().setBackground(Theme.PANEL);
        add(centerScroll, BorderLayout.CENTER);

        // ── Footer ───────────────────────────────────────────────────────────
        JPanel footer = new JPanel();
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        footer.setBackground(Theme.PANEL);
        footer.setBorder(BorderFactory.createEmptyBorder(4, 6, 6, 6));

        JButton saveBtn = new JButton("💾  Guardar cambios");
        saveBtn.setBackground(Theme.HL);
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        saveBtn.setBorderPainted(false);
        saveBtn.setFocusPainted(false);
        saveBtn.setOpaque(true);
        saveBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        saveBtn.setAlignmentX(0.5f);
        saveBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        saveBtn.addActionListener(e -> save());
        footer.add(saveBtn);
        footer.add(vgap(4));

        statusLbl = new JLabel(" ");
        statusLbl.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        statusLbl.setForeground(Theme.SUCCESS);
        statusLbl.setAlignmentX(0.5f);
        footer.add(statusLbl);

        add(footer, BorderLayout.SOUTH);
    }

    // ── Carga asíncrona ──────────────────────────────────────────────────────

    public void load(MediaFile mf) {
        this.current = mf;
        nameField.setText(mf.getName());
        for (JTextField tf : editFields.values()) tf.setText("");
        clearTree("Cargando metadatos…");
        statusLbl.setText(" ");

        SwingWorker<Map<String, Map<String, String>>, Void> worker =
            new SwingWorker<>() {
                @Override protected Map<String, Map<String, String>> doInBackground() {
                    return MetadataEngine.read(mf);
                }
                @Override protected void done() {
                    try {
                        populateTree(get());
                        prefillEditFields(get());
                    } catch (Exception e) {
                        clearTree("Error: " + e.getMessage());
                    }
                }
            };
        worker.execute();
    }

    private void clearTree(String msg) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(msg);
        treeModel.setRoot(root);
    }

    private void populateTree(Map<String, Map<String, String>> meta) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Metadatos");
        for (var entry : meta.entrySet()) {
            DefaultMutableTreeNode section =
                new DefaultMutableTreeNode(entry.getKey());
            for (var field : entry.getValue().entrySet()) {
                section.add(new DefaultMutableTreeNode(
                    field.getKey() + ": " + field.getValue()));
            }
            root.add(section);
        }
        treeModel.setRoot(root);
        // Expandir primer nivel
        for (int i = 0; i < tree.getRowCount(); i++) tree.expandRow(i);
    }

    private void prefillEditFields(Map<String, Map<String, String>> meta) {
        for (var section : meta.values()) {
            for (var entry : section.entrySet()) {
                for (String key : EDITABLE_FIELDS) {
                    if (entry.getKey().equalsIgnoreCase(key) ||
                        entry.getKey().equalsIgnoreCase(
                            FIELD_LABELS.getOrDefault(key, key))) {
                        editFields.get(key).setText(entry.getValue());
                    }
                }
            }
        }
    }

    // ── Guardar ───────────────────────────────────────────────────────────────

    private void save() {
        if (current == null) return;
        String newName = nameField.getText().trim();
        StringBuilder msg = new StringBuilder();

        // Renombrar
        if (!newName.isEmpty() && !newName.equals(current.getName())) {
            java.io.File renamed = MetadataEngine.renameFile(current, newName);
            msg.append(renamed != null ? "Renombrado ✓  " : "Renombrar falló  ");
        }

        // EXIF
        Map<String, String> fields = new LinkedHashMap<>();
        for (var entry : editFields.entrySet()) {
            String val = entry.getValue().getText().trim();
            if (!val.isEmpty()) fields.put(entry.getKey(), val);
        }
        if (!fields.isEmpty()) {
            msg.append(MetadataEngine.writeExif(current, fields));
        }

        String result = msg.toString().trim();
        statusLbl.setText(result.isEmpty() ? "Sin cambios." : result);
        Timer t = new Timer(3000, evt -> statusLbl.setText(" "));
        t.setRepeats(false); t.start();

        if (onSaved != null) onSaved.accept(current);
    }

    // ── Copiar valor al portapapeles ──────────────────────────────────────────

    private void copySelectedValue() {
        TreePath path = tree.getSelectionPath();
        if (path == null) return;
        Object node = path.getLastPathComponent();
        String text = node.toString();
        int colon = text.indexOf(':');
        String value = colon >= 0 ? text.substring(colon + 2) : text;
        Toolkit.getDefaultToolkit().getSystemClipboard()
            .setContents(new StringSelection(value), null);
        statusLbl.setText("Copiado al portapapeles");
        Timer t = new Timer(2000, evt -> statusLbl.setText(" "));
        t.setRepeats(false); t.start();
    }

    // ── Estilo ────────────────────────────────────────────────────────────────

    private void styleTree() {
        tree.setBackground(Theme.CARD);
        tree.setForeground(Theme.TEXT);
        tree.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        tree.setRowHeight(20);
        tree.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
        renderer.setBackgroundNonSelectionColor(Theme.CARD);
        renderer.setBackgroundSelectionColor(Theme.HL2);
        renderer.setTextNonSelectionColor(Theme.TEXT);
        renderer.setTextSelectionColor(Color.WHITE);
        renderer.setBorderSelectionColor(Theme.HL2);
        tree.setCellRenderer(renderer);
    }

    private static JPanel row(String label) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        p.setBackground(Theme.PANEL);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        p.setAlignmentX(0);
        JLabel lbl = new JLabel(label);
        lbl.setForeground(Theme.DIM);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        lbl.setPreferredSize(new Dimension(72, 18));
        p.add(lbl);
        return p;
    }

    private static JTextField darkField() {
        JTextField tf = new JTextField(16);
        tf.setBackground(Theme.INPUT);
        tf.setForeground(Theme.TEXT);
        tf.setCaretColor(Theme.TEXT);
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.BORDER),
            BorderFactory.createEmptyBorder(2, 4, 2, 4)));
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        return tf;
    }

    private static Component vgap(int h) {
        return Box.createRigidArea(new Dimension(0, h));
    }
}