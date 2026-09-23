package com.mediaviewer.ui.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import com.mediaviewer.engine.MetadataEngine;
import com.mediaviewer.model.MediaFile;
import com.mediaviewer.ui.components.ThemedButton;
import com.mediaviewer.ui.components.ThemedLabel;
import com.mediaviewer.ui.components.ThemedPanel;
import com.mediaviewer.ui.components.ThemedScroll;
import com.mediaviewer.ui.components.ThemedSeparator;
import com.mediaviewer.ui.components.ThemedTextField;
import com.mediaviewer.ui.components.ThemedTree;
import com.mediaviewer.ui.components.ThemedTreeCellRenderer;
import com.mediaviewer.util.ThemeUtils;

/**
 * Panel derecho: árbol de metadatos + campos editables + botón guardar.
 *
 * La lectura de metadatos ocurre en SwingWorker para no bloquear el EDT.
 */
public class MetadataPanel extends ThemedPanel {
    private final static int SHOW_COPY_TIME = 2000;
    private final static int SCROLL_SPEED   = 25;

    private final AtomicInteger copyInteger = new AtomicInteger(0);

    private final DefaultTreeModel             treeModel;
    private final ThemedTree                   tree;
    private final ThemedTextField              nameField;
    private final Map<String, ThemedTextField> editFields = new LinkedHashMap<>();
    private final ThemedLabel                  statusLbl;
    private final Consumer<MediaFile>          onSaved;

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
        super();
        this.onSaved = onSaved;

        panel.setLayout(new BorderLayout());
        panel.setPreferredSize(new Dimension(290, 0));

        // ── Cabecera ─────────────────────────────────────────────────────────
        ThemedPanel hdr = new ThemedPanel(new FlowLayout(FlowLayout.LEFT, 8, 6), ThemeUtils.PanelType.HIGHLIGHT2);
        ThemedLabel title = new ThemedLabel("📋  METADATOS", ThemeUtils.TextType.PRIMARY, ThemeUtils.FontSize.SMALL, Font.BOLD, ThemeUtils.FontType.EMOJI);
        hdr.addToPanel(title);
        addToPanel(hdr, BorderLayout.NORTH);

        // ── Panel central (scroll) ───────────────────────────────────────────
        ThemedPanel center = new ThemedPanel();
        center.setPanelLayout(new BoxLayout(center.getPanel(), BoxLayout.Y_AXIS));

        // Nombre
        ThemedPanel nameRow = row("Nombre:");
        nameField = darkField("Nombre");
        nameRow.addToPanel(nameField);
        center.addToPanel(nameRow);
        center.addToPanel(vgap(2));

        // Árbol de metadatos
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        treeModel = new DefaultTreeModel(root);
        tree = new ThemedTree(treeModel);
        styleTree();
        tree.getTree().addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) copySelectedValue();
            }
        });
        ThemedScroll sp = new ThemedScroll(tree);
        sp.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        sp.setPreferredSize(new Dimension(0, 500));
        sp.getVerticalScrollBar().setUnitIncrement(SCROLL_SPEED);
        sp.getHorizontalScrollBar().setUnitIncrement(SCROLL_SPEED);
        center.addToPanel(sp);
        center.addToPanel(vgap(4));

        // Separador
        ThemedSeparator sep = new ThemedSeparator();
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        center.addToPanel(sep);
        center.addToPanel(vgap(4));

        // Campos editables
        ThemedPanel editPanel = new ThemedPanel();
        editPanel.setPanelLayout(new BoxLayout(editPanel.getPanel(), BoxLayout.Y_AXIS));
        ThemedLabel editTitle = new ThemedLabel("  Editar etiquetas", ThemeUtils.TextType.SECONDARY, ThemeUtils.FontSize.SMALL, Font.ITALIC, ThemeUtils.FontType.BASIC);
        editTitle.setAlignmentX(0);
        editPanel.addToPanel(editTitle);
        editPanel.addToPanel(vgap(2));

        for (String key : EDITABLE_FIELDS) {
            ThemedPanel r = row(FIELD_LABELS.get(key) + ":");
            ThemedTextField tf = darkField(FIELD_LABELS.get(key));
            editFields.put(key, tf);
            r.addToPanel(tf);
            editPanel.addToPanel(r);
            editPanel.addToPanel(vgap(1));
        }
        editPanel.setPreferredSize(new Dimension(0, 200));
        editPanel.setMaximumSize(new Dimension(0, 200));
        center.addToPanel(editPanel);

        /*ThemedScroll centerScroll = new ThemedScroll(center,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        centerScroll.setBorder(null);*/
        addToPanel(center, BorderLayout.CENTER);

        // ── Footer ───────────────────────────────────────────────────────────
        ThemedPanel footer = new ThemedPanel();
        footer.setPanelLayout(new BoxLayout(footer.getPanel(), BoxLayout.Y_AXIS));
        footer.setBorder(BorderFactory.createEmptyBorder(4, 6, 6, 6));

        ThemedButton saveBtn = new ThemedButton("💾  Guardar cambios", ThemeUtils.TextType.TERTIARY, ThemeUtils.FontSize.SMALL, Font.BOLD, ThemeUtils.FontType.EMOJI, ThemeUtils.ButtonType.HIGHLIGHT);
        saveBtn.setBorderPainted(false);
        saveBtn.setFocusPainted(false);
        saveBtn.setOpaque(true);
        saveBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        saveBtn.setAlignmentX(0.5f);
        saveBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        saveBtn.getButton().addActionListener(e -> save());
        footer.addToPanel(saveBtn);
        footer.addToPanel(vgap(4));

        statusLbl = new ThemedLabel(" ", ThemeUtils.TextType.SUCCESS, ThemeUtils.FontSize.SMALL, Font.PLAIN, ThemeUtils.FontType.BASIC);
        statusLbl.setAlignmentX(0.5f);
        footer.addToPanel(statusLbl);

        addToPanel(footer, BorderLayout.SOUTH);
    }

    // ── Carga asíncrona ──────────────────────────────────────────────────────

    public void load(MediaFile mf) {
        this.current = mf;
        nameField.setText(mf.getName());
        for (ThemedTextField tf : editFields.values()) tf.setText("");
        clearTree("Cargando metadatos…");
        copyInteger.incrementAndGet();
        statusLbl.setText(" ");

        SwingWorker<Map<String, Map<String, List<String>>>, Void> worker =
            new SwingWorker<>() {
                @Override protected Map<String, Map<String, List<String>>> doInBackground() {
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

    private void populateTree(Map<String, Map<String, List<String>>> meta) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Metadatos");
        for (var entry : meta.entrySet()) {
            DefaultMutableTreeNode section =
                new DefaultMutableTreeNode(entry.getKey());
            for (var field : entry.getValue().entrySet()) {
                //System.out.println(field.getValue());
                for (String value : field.getValue()) {
                    section.add(new DefaultMutableTreeNode(
                        field.getKey() + ": " + value));
                }
                //section.add(new DefaultMutableTreeNode(
                //    field.getKey() + ": " + field.getValue()));
            }
            root.add(section);
        }
        treeModel.setRoot(root);
        // Expandir primer nivel
        for (int i = 0; i < tree.getRowCount(); i++) tree.getTree().expandRow(i);
    }

    private void prefillEditFields(Map<String, Map<String, List<String>>> meta) {
        for (var section : meta.values()) {
            for (var entry : section.entrySet()) {
                for (String key : EDITABLE_FIELDS) {
                    if (entry.getKey().equalsIgnoreCase(key) ||
                        entry.getKey().equalsIgnoreCase(
                            FIELD_LABELS.getOrDefault(key, key))) {
                        editFields.get(key).setText(entry.getValue().getFirst());
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
        int copyGen = copyInteger.incrementAndGet();
        Timer t = new Timer(SHOW_COPY_TIME, evt -> {
            if(copyGen == copyInteger.get()) statusLbl.setText(" ");
        });
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
        int copyGen = copyInteger.incrementAndGet();
        Timer t = new Timer(SHOW_COPY_TIME, evt -> {
            if(copyGen == copyInteger.get())
                statusLbl.setText(" ");
        });
        t.setRepeats(false); t.start();
    }

    // ── Estilo ────────────────────────────────────────────────────────────────

    private void styleTree() {
        tree.setBackground(ThemeUtils.PanelType.PANEL);
        tree.setForeground(ThemeUtils.TextType.PRIMARY);
        tree.setFont(ThemeUtils.FontType.BASIC, Font.PLAIN, ThemeUtils.FontSize.SMALL);
        tree.setRowHeight(20);
        tree.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        ThemedTreeCellRenderer renderer = new ThemedTreeCellRenderer();
        renderer.setBackgroundNonSelectionColor(ThemeUtils.PanelType.PANEL);
        renderer.setBackgroundSelectionColor(ThemeUtils.PanelType.HIGHLIGHT2);
        renderer.setTextNonSelectionColor(ThemeUtils.TextType.PRIMARY);
        renderer.setTextSelectionColor(ThemeUtils.TextType.TERTIARY);
        tree.setCellRenderer(renderer.getRender());
    }

    private static ThemedPanel row(String label) {
        ThemedPanel p = new ThemedPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        p.setAlignmentX(0);

        ThemedLabel lbl = new ThemedLabel(label, ThemeUtils.TextType.SECONDARY, ThemeUtils.FontSize.SMALL, Font.PLAIN, ThemeUtils.FontType.BASIC);
        lbl.setPreferredSize(new Dimension(72, 18));
        p.addToPanel(lbl);
        return p;
    }

    private static ThemedTextField darkField(String toolTip) {
        Border b = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.WHITE),
            BorderFactory.createEmptyBorder(2, 4, 2, 4));
        ThemedTextField tf = new ThemedTextField(16, toolTip, b);
        return tf;
    }

    private static Component vgap(int h) {
        return Box.createRigidArea(new Dimension(0, h));
    }
}