package com.mediavault.ui;

import com.mediavault.engine.FileScanner;
import com.mediavault.model.FilterOptions;
import com.mediavault.model.MediaFile;
import com.mediavault.ui.panels.*;
import com.mediavault.util.Theme;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

/**
 * Ventana principal de MediaVault.
 *
 * Reglas de threading:
 *  ┌────────────────────────────────────────────────────────┐
 *  │ EDT (Event Dispatch Thread)                            │
 *  │   • Todo lo que actualiza widgets                      │
 *  │   • Filtrado/ordenación (rápido, en memoria)           │
 *  │   • Recibe callbacks de workers vía invokeLater/done() │
 *  ├────────────────────────────────────────────────────────┤
 *  │ SwingWorker / ExecutorService (hilos de fondo)         │
 *  │   • Escaneo de disco (FileScanner)                     │
 *  │   • Carga de imágenes (ViewerPanel)                    │
 *  │   • Miniaturas (ThumbnailStrip)                        │
 *  │   • Lectura de metadatos (MetadataPanel)               │
 *  └────────────────────────────────────────────────────────┘
 */
public class MainWindow extends JFrame {

    // —— Constantes
    private final static String FUENTE_DEFAULT = "Segoe UI Symbol";
    private final static int    SHOW_SCAN_TIME = 10000;

    // ── Estado ────────────────────────────────────────────────────────────────
    private List<MediaFile> allFiles      = new ArrayList<>();
    private List<MediaFile> filtered      = new ArrayList<>();
    private int             currentIdx    = -1;
    private File            currentDir    = null;
    private FileScanner     activeScanner = null;

    // ── Persistencia ─────────────────────────────────────────────────────────
    private final Preferences prefs = Preferences.userNodeForPackage(MainWindow.class);

    // ── Paneles ───────────────────────────────────────────────────────────────
    private ViewerPanel    viewer;
    private ThumbnailStrip thumbStrip;
    private FileListPanel  fileList;
    private MetadataPanel  metaPanel;
    private FilterBar      filterBar;
    private JLabel         dirLabel;
    private JLabel         scanLabel;
    private JLabel         posLabel;
    private JLabel         viewerStatus;

    public MainWindow() {
        super("Meδia Viewer");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(1420, 900);
        setMinimumSize(new Dimension(960, 660));
        setLocationRelativeTo(null);
        setBackground(Theme.BG);

        applyLookAndFeel();
        buildUI();
        bindKeys();
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { shutdown(); }
        });

        // Restaurar última carpeta
        String lastDir = prefs.get("lastDir", "");
        if (!lastDir.isEmpty()) {
            File f = new File(lastDir);
            if (f.isDirectory()) SwingUtilities.invokeLater(() -> loadDirectory(f));
        }
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    /**
     * @brief Construye la Interfaz gráfica a partir de los paneles
     */
    private void buildUI() {
        // ── Barra superior ──
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 7));
        topBar.setBackground(Theme.HL2);

        JLabel logo = new JLabel("Meδia Viewer");
        logo.setForeground(Theme.TEXT);
        logo.setFont(new Font(FUENTE_DEFAULT, Font.BOLD, 14));
        topBar.add(logo);

        JButton openBtn = accentButton("Abrir carpeta");
        openBtn.addActionListener(evt -> chooseDirectory());
        topBar.add(openBtn);

        dirLabel = new JLabel("Sin carpeta — Ctrl+O para abrir");
        dirLabel.setForeground(Theme.DIM);
        dirLabel.setFont(new Font(FUENTE_DEFAULT, Font.PLAIN, 10));
        topBar.add(dirLabel);

        scanLabel = new JLabel("");
        scanLabel.setForeground(Theme.SUCCESS);
        scanLabel.setFont(new Font(FUENTE_DEFAULT, Font.PLAIN, 10));
        // empujar a la derecha
        topBar.add(Box.createHorizontalStrut(30));
        topBar.add(scanLabel);

        add(topBar, BorderLayout.NORTH);

        // ── Barra de filtros ──
        filterBar = new FilterBar(this::applyFilters);
        add(filterBar, BorderLayout.AFTER_LAST_LINE); // provisional, se reordena

        // ── Panel principal (split) ──
        viewer    = new ViewerPanel();
        viewerStatus = new JLabel("Selecciona una carpeta para empezar");
        viewerStatus.setForeground(Theme.DIM);
        viewerStatus.setFont(new Font(FUENTE_DEFAULT, Font.PLAIN, 10));
        viewer.setStatusLabel(viewerStatus);

        fileList  = new FileListPanel(this::selectByIndex);
        metaPanel = new MetadataPanel(this::onSaved);

        JSplitPane leftSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            fileList, buildCenterPanel());
        leftSplit.setDividerLocation(210);
        leftSplit.setDividerSize(5);
        leftSplit.setBorder(null);
        leftSplit.setBackground(Theme.BG);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            leftSplit, metaPanel);
        mainSplit.setDividerLocation(getWidth() - 300);
        mainSplit.setDividerSize(5);
        mainSplit.setBorder(null);
        mainSplit.setBackground(Theme.BG);
        mainSplit.setResizeWeight(1.0);

        // Layout con filtros arriba y split en centro
        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(Theme.BG);
        body.add(filterBar, BorderLayout.NORTH);
        body.add(mainSplit, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        // ── Barra inferior (navegación) ──
        add(buildNavBar(), BorderLayout.SOUTH);
    }

    /**
     * @brief Contruye el panel central compuesto por:
     * - El vector de thumbnails
     * - El campo de display para la imagen
     * - La barra con los botones que permiten hacer zoom y el tamaño de la imagen
     * @return El panel construido
     */
    private JPanel buildCenterPanel() {
        thumbStrip = new ThumbnailStrip(this::selectByIndex);

        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(Theme.BG);

        // Status bar del visor (debajo del canvas)
        JPanel viewerBar = new JPanel(new GridLayout());
        viewerBar.setBackground(Theme.PANEL);
        
        // Botones zoom
        JPanel zoomButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 3));
        for (String[] b : new String[][]{{"−","zoom−"},{"⟳","reset"},{"+","zoom+"},{"↗","open"}}) {
            JButton btn = new JButton(b[0]);
            btn.setBackground(Theme.ACCENT);
            btn.setForeground(Theme.TEXT);
            btn.setBorderPainted(false);
            btn.setFocusPainted(false);
            btn.setOpaque(true);
            btn.setFont(new Font(FUENTE_DEFAULT, Font.PLAIN, 12));
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.addActionListener(evt -> handleViewerAction(b[1]));
            zoomButtons.add(btn);
        }        

        viewerBar.add(viewerStatus);
        viewerBar.add(zoomButtons); 
        viewerBar.add(Box.createHorizontalStrut(viewerStatus.getWidth()));

        center.add(viewer,    BorderLayout.CENTER);
        center.add(viewerBar, BorderLayout.SOUTH);
        center.add(thumbStrip, BorderLayout.NORTH);
        return center;
    }

    /** 
     * @brief Construye la barra de navegación con los botones pertinentes
     * @return El panel construido
     */
    private JPanel buildNavBar() {
        JPanel nav = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        nav.setBackground(Theme.PANEL);
        nav.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER));

        nav.setLayout(new BorderLayout());

        // Botones Izquierdos
        JPanel leftNav = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        leftNav.setBackground(Theme.PANEL);
        for (String[] b : new String[][]{{"⏮","first"},{"◀","prev"},{"▶","next"},{"⏭","last"}}) {
            JButton btn = new JButton(b[0]);
            styleNavBtn(btn);
            btn.addActionListener(evt -> handleNav(b[1]));
            leftNav.add(btn);
        }
        leftNav.add(Box.createHorizontalStrut(10));
        posLabel = new JLabel("—");
        posLabel.setForeground(Theme.DIM);
        posLabel.setFont(new Font(FUENTE_DEFAULT, Font.PLAIN, 11));
        leftNav.add(posLabel);

        // Botones derechos
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 6));
        right.setBackground(Theme.PANEL);
        right.setOpaque(false);

        JButton refresh = ghostButton("↺  Refrescar  F5");
        refresh.addActionListener(evt -> startScan());
        right.add(refresh);

        JButton delete = ghostButton("🗑  Eliminar  Del");
        delete.setForeground(Theme.HL);
        delete.addActionListener(evt -> deleteCurrentFile());
        right.add(delete);

        nav.add(leftNav, BorderLayout.WEST);
        nav.add(right, BorderLayout.EAST);
        return nav;
    }

    /**
     * @brief Aplica un estilo predefinido (Estilo de navegación) a un botón 
     * @param b el botón
     */
    private void styleNavBtn(JButton b) {
        b.setBackground(Theme.ACCENT);
        b.setForeground(Theme.TEXT);
        b.setFont(new Font(FUENTE_DEFAULT, Font.BOLD, 14));
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setPreferredSize(new Dimension(44, 32));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    // ── Teclas ────────────────────────────────────────────────────────────────
    /**
     * @brief Establece la lógica de la aplicación a algunas teclas y eventos de teclado para navegación por teclado
     */
    private void bindKeys() {
        KeyStroke left   = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT,   0);
        KeyStroke right  = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT,  0);
        KeyStroke home   = KeyStroke.getKeyStroke(KeyEvent.VK_HOME,   0);
        KeyStroke end    = KeyStroke.getKeyStroke(KeyEvent.VK_END,    0);
        KeyStroke del    = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);
        KeyStroke f5     = KeyStroke.getKeyStroke(KeyEvent.VK_F5,     0);
        KeyStroke ctrlO  = KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK);

        JRootPane rp = getRootPane();
        rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(left,  "prev");
        rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(right, "next");
        rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(home,  "first");
        rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(end,   "last");
        rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(del,   "delete");
        rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(f5,    "refresh");
        rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ctrlO, "open");

        rp.getActionMap().put("prev",    new AbstractAction() { public void actionPerformed(ActionEvent e) { goTo(currentIdx - 1); } });
        rp.getActionMap().put("next",    new AbstractAction() { public void actionPerformed(ActionEvent e) { goTo(currentIdx + 1); } });
        rp.getActionMap().put("first",   new AbstractAction() { public void actionPerformed(ActionEvent e) { goTo(0); } });
        rp.getActionMap().put("last",    new AbstractAction() { public void actionPerformed(ActionEvent e) { goTo(filtered.size() - 1); } });
        rp.getActionMap().put("delete",  new AbstractAction() { public void actionPerformed(ActionEvent e) { deleteCurrentFile(); } });
        rp.getActionMap().put("refresh", new AbstractAction() { public void actionPerformed(ActionEvent e) { startScan(); } });
        rp.getActionMap().put("open",    new AbstractAction() { public void actionPerformed(ActionEvent e) { chooseDirectory(); } });
    }

    // ── Directorio ────────────────────────────────────────────────────────────
    /**
     * @brief Crea el diálogo default para seleccionar directorio en el que trabajar
     */
    private void chooseDirectory() {
        JFileChooser fc = new JFileChooser(
            currentDir != null ? currentDir : new File(System.getProperty("user.home")));
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setDialogTitle("Seleccionar carpeta de medios");
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
            loadDirectory(fc.getSelectedFile());
    }

    /**
     * @brief Carga un directorio en la aplicación para leer su contenido
     * @param dir El directorio que se debe cargar
     */
    private void loadDirectory(File dir) {
        currentDir = dir;
        prefs.put("lastDir", dir.getAbsolutePath());
        String shortPath = dir.getAbsolutePath();
        if (shortPath.length() > 65) shortPath = "…" + shortPath.substring(shortPath.length() - 62);
        dirLabel.setText(shortPath); //Etiqueta de la topBar
        startScan();
    }

    // ── Escaneo (en SwingWorker — nunca bloquea EDT) ──────────────────────────

    private void startScan() {
        if (currentDir == null) return;
        if (activeScanner != null) activeScanner.stop();

        FilterOptions opts = filterBar.get();
        scanLabel.setText("Escaneando…");

        activeScanner = new FileScanner(
            currentDir,
            opts.typeFilter(),
            opts.recursive(),
            files -> {                       // onDone — ya en EDT via done()
                allFiles = files;
                scanLabel.setText(files.size() + " archivos encontrados");
                Timer t = new Timer(SHOW_SCAN_TIME, e -> scanLabel.setText(""));
                t.setRepeats(false); t.start();
                applyFilters();
            },
            msg -> scanLabel.setText(msg)    // onProgress — ya en EDT via process()
        );
        activeScanner.execute();
    }

    // ── Filtrado / Ordenación (rápido, en EDT) ────────────────────────────────

    private void applyFilters() {
        FilterOptions opts = filterBar.get();
        List<MediaFile> items = new ArrayList<>(allFiles);

        // Filtro de tipo
        items = switch (opts.typeFilter()) {
            case "Imágenes" -> items.stream()
                .filter(m -> m.getType() == MediaFile.MediaType.IMAGE)
                .collect(Collectors.toList());
            case "GIFs"    -> items.stream()
                .filter(m -> m.getType() == MediaFile.MediaType.GIF)
                .collect(Collectors.toList());
            case "Videos"  -> items.stream()
                .filter(m -> m.getType() == MediaFile.MediaType.VIDEO)
                .collect(Collectors.toList());
            default        -> items;
        };

        // Filtro de búsqueda
        if (!opts.searchText().isEmpty()) {
            String q = opts.searchText();
            items = items.stream()
                .filter(m -> m.getName().toLowerCase().contains(q))
                .collect(Collectors.toList());
        }

        // Ordenación
        Comparator<MediaFile> cmp = switch (opts.sortKey()) {
            case "Nombre ↓"  -> Comparator.comparing(MediaFile::getName,
                                    String.CASE_INSENSITIVE_ORDER).reversed();
            case "Fecha ↑"   -> Comparator.comparingLong(MediaFile::getLastModified);
            case "Fecha ↓"   -> Comparator.comparingLong(MediaFile::getLastModified).reversed();
            case "Tamaño ↑"  -> Comparator.comparingLong(MediaFile::getSize);
            case "Tamaño ↓"  -> Comparator.comparingLong(MediaFile::getSize).reversed();
            default          -> Comparator.comparing(MediaFile::getName,
                                    String.CASE_INSENSITIVE_ORDER);
        };
        items.sort(cmp);

        filtered = items;
        filterBar.setCount(filtered.size(), allFiles.size());
        fileList.populate(filtered);
        thumbStrip.populate(filtered, 0);

        if (!filtered.isEmpty()) selectByIndex(0);
        else {
            currentIdx = -1;
            viewer.clear();
            posLabel.setText("—");
        }
    }

    // ── Selección / Navegación ────────────────────────────────────────────────
    /**
     * @brief selecciona un archivo por su indice en el vector de filtrados y actualiza la vista con este elemento
     */
    public void selectByIndex(int idx) {
        if (filtered.isEmpty()) return;
        idx = Math.max(0, Math.min(idx, filtered.size() - 1));
        if (idx == currentIdx) return;
        currentIdx = idx;
        MediaFile mf = filtered.get(idx);

        viewer.load(mf);
        metaPanel.load(mf);
        fileList.highlight(idx);
        thumbStrip.highlight(idx);
        posLabel.setText((idx + 1) + " / " + filtered.size() + "   " + mf.getName());
    }

    private void goTo(int idx) { selectByIndex(idx); }

    // ── Acciones ──────────────────────────────────────────────────────────────
    /**
     * @brief Lógica para los botones de navegación rápida
     * @param cmd La cadena que identifica el comando
     */
    private void handleNav(String cmd) {
        switch (cmd) {
            case "prev"  -> goTo(currentIdx - 1);
            case "next"  -> goTo(currentIdx + 1);
            case "first" -> goTo(0);
            case "last"  -> goTo(filtered.size() - 1);
        }
    }

    /**
     * @brief Lógica para los botones de zoom del panel central
     * @param cmd La cadena que identifica el comando
     */
    private void handleViewerAction(String cmd) {
        switch (cmd) {
            case "zoom−" -> viewer.zoomOut();
            case "zoom+" -> viewer.zoomIn();
            case "reset" -> viewer.resetView();
            case "open"  -> { if (currentIdx >= 0) openExternally(filtered.get(currentIdx).getFile()); }
        }
    }

    private void onSaved(MediaFile mf) {
        // Recargar lista en caso de renombre
        fileList.populate(filtered);
        fileList.highlight(currentIdx);
        if (currentIdx >= 0 && currentIdx < filtered.size())
            posLabel.setText((currentIdx+1) + " / " + filtered.size() + "   " + mf.getName());
    }

    private void deleteCurrentFile() {
        if (currentIdx < 0 || filtered.isEmpty()) return;
        MediaFile mf = filtered.get(currentIdx);
        int choice = JOptionPane.showConfirmDialog(this,
            "¿Eliminar permanentemente?\n\n" + mf.getName(),
            "Eliminar archivo", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) return;
        if (mf.getFile().delete()) {
            allFiles.remove(mf);
            applyFilters();
        } else {
            JOptionPane.showMessageDialog(this, "No se pudo eliminar el archivo.",
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openExternally(File file) {
        try { Desktop.getDesktop().open(file); }
        catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al abrir: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Cierre ────────────────────────────────────────────────────────────────
    /**
     * @brief Cierra la aplicación cerrando los recursos necesarios.
     */
    private void shutdown() {
        if (activeScanner != null) activeScanner.stop();
        viewer.shutdown();
        thumbStrip.shutdown();
        dispose();
        System.exit(0);
    }

    // ── Helpers de estilo ─────────────────────────────────────────────────────
    /**
     * @brief Crea un botón de acento
     * @param text El texto del botón
     */
    private static JButton accentButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(Theme.HL);
        b.setForeground(Color.WHITE);
        b.setFont(new Font(FUENTE_DEFAULT, Font.BOLD, 11));
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    /**
     * @brief Crea un botón transparente
     * @param text El texto del botón
     */
    private static JButton ghostButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(Theme.PANEL);
        b.setForeground(Theme.DIM);
        b.setFont(new Font(FUENTE_DEFAULT, Font.PLAIN, 10));
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    // ── Look & Feel ───────────────────────────────────────────────────────────
    /**
     * @brief Aplica los estilos propios de Theme al UIManager
     */
    private static void applyLookAndFeel() {
        try {
            // FlatLaf si está disponible, si no Nimbus, si no el del sistema
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            // Colores globales de Swing
            UIManager.put("Panel.background",           Theme.PANEL);
            UIManager.put("ScrollBar.background",       Theme.ACCENT);
            UIManager.put("ScrollBar.thumb",            Theme.HL2);
            UIManager.put("ComboBox.background",        Theme.INPUT);
            UIManager.put("ComboBox.foreground",        Theme.TEXT);
            UIManager.put("OptionPane.background",      Theme.PANEL);
            UIManager.put("OptionPane.messageForeground",Theme.TEXT);
        } catch (Exception ignored) {}
    }
}