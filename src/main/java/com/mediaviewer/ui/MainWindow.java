package com.mediaviewer.ui;

import com.mediaviewer.engine.FileScanner;
import com.mediaviewer.model.FilterOptions;
import com.mediaviewer.model.MediaFile;
import com.mediaviewer.ui.panels.*;
import com.mediaviewer.ui.components.*;
import com.mediaviewer.util.Theme;

import com.formdev.flatlaf.extras.FlatInspector;
import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Ventana principal de mediaviewer.
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
    private final static int    SHOW_SCAN_TIME = 5000;

    // ── Estado ────────────────────────────────────────────────────────────────
    private List<MediaFile> allFiles      = new ArrayList<>();
    private List<MediaFile> filtered      = new ArrayList<>();
    private int             currentIdx    = -1;
    private File            currentDir    = null;
    private FileScanner     activeScanner = null;

    // ── Persistencia ─────────────────────────────────────────────────────────
    private final Preferences prefs = Preferences.userNodeForPackage(MainWindow.class);
    private final static String THEME_PREF_KEY = "theme";
    private final static String DIR_PREF_KEY   = "lastDir";

    // ── Paneles ───────────────────────────────────────────────────────────────
    private ViewerPanel    viewer;
    private ThumbnailStrip thumbStrip;
    private FileListPanel  fileList;
    //private MetadataPanel  metaPanel;
    //private FilterBar      filterBar;
    private ThemedLabel    dirLabel;
    private ThemedLabel    scanLabel;
    private JLabel         posLabel;
    private ThemedLabel    viewerStatus;

    // ── Control ─────────────────────────────────────────────────────────────
    private final AtomicInteger scanLabelInteger = new AtomicInteger(0);


    public MainWindow() {
        super("Meδia Viewer");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(1420, 900);
        setMinimumSize(new Dimension(960, 660));
        setLocationRelativeTo(null);

        ThemeManager themeManager = ThemeManager.getInstance();
        String themeName = prefs.get(THEME_PREF_KEY, Theme.LIGHT.name());
        try {
            ThemeManager.setTheme(Theme.valueOf(themeName));
        } catch (IllegalArgumentException e) {
            ThemeManager.setTheme(Theme.LIGHT);
        }
        System.out.println(ThemeManager.getInstance().getCurrentTheme().getThemeName());

        FlatInspector.install("ctrl shift alt F");
        applyLookAndFeel(ThemeManager.getInstance().getCurrentTheme());
        buildUI();

        bindKeys();
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { shutdown(); }
        });

        // Restaurar última carpeta
        String lastDir = prefs.get(DIR_PREF_KEY, "");
        if (!lastDir.isEmpty()) {
            File f = new File(lastDir);
            if (f.isDirectory()) SwingUtilities.invokeLater(() -> loadDirectory(f));
        }

        // Registrar frame como listener
        ThemeManager.getInstance().addListener(this::applyThemeToFrame);
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    /**
     * @brief Construye la Interfaz gráfica a partir de los paneles
     */
    private void buildUI() {  
        // ── Barra superior ──
        ThemedPanel topBar = new ThemedPanel(new FlowLayout(FlowLayout.LEFT, 10, 7));

        ThemedLabel logo = new ThemedLabel("Meδia Viewer", TextType.PRIMARY, FontSize.MED, Font.BOLD);
        topBar.add(logo);

        ThemedButton openBtn = highlightButton("Abrir carpeta");
        openBtn.addActionListener(evt -> chooseDirectory());
        topBar.add(openBtn);

        dirLabel = new ThemedLabel("Sin carpeta — Ctrl+O para abrir", TextType.SECONDARY, FontSize.SMALL, Font.PLAIN);
        topBar.add(dirLabel);

        scanLabel = new ThemedLabel("", TextType.SUCCESS, FontSize.SMALL, Font.PLAIN);
        // empujar a la derecha
        topBar.add(Box.createHorizontalStrut(30));
        topBar.add(scanLabel);

        add(topBar, BorderLayout.NORTH);

        // ── Barra de filtros ──
        filterBar = new FilterBar(this::applyFilters);
        add(filterBar, BorderLayout.AFTER_LAST_LINE); // provisional, se reordena

        // ── Panel principal (split) ──
        viewer = new ViewerPanel();
        viewerStatus = new ThemedLabel("Selecciona una carpeta para empezar", TextType.SECONDARY, FontSize.SMALL, Font.PLAIN);
        viewer.setStatusLabel(viewerStatus);

        /*fileList  = new FileListPanel(this::selectByIndex);
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
        mainSplit.setResizeWeight(1.0);*/

        // Layout con filtros arriba y split en centro
        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(Theme.BG);
        body.add(filterBar, BorderLayout.NORTH);
        //body.add(mainSplit, BorderLayout.CENTER);
        body.add(buildCenterPanel(), BorderLayout.CENTER);
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
        center.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER));

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
            btn.setFont(Theme.FONT_MED_BOLD);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.addActionListener(evt -> handleViewerAction(b[1]));
            zoomButtons.add(btn);
        }        

        viewerBar.add(viewerStatus);
        viewerBar.add(zoomButtons); 
        viewerBar.add(Box.createHorizontalStrut(viewerStatus.getWidth()));
        viewerBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER));

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
        JPanel nav = new JPanel(new GridLayout());
        nav.setBackground(Theme.PANEL);
        nav.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER));

        //nav.setLayout(new BorderLayout());

        // Botones centrales
        JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 6));
        center.setBackground(Theme.PANEL);
        for (String[] b : new String[][]{{"⏮","first"},{"◀","prev"},{"▶","next"},{"⏭","last"}}) {
            JButton btn = new JButton(b[0]);
            styleBtn(btn);
            btn.addActionListener(evt -> handleNav(b[1]));
            center.add(btn);
        }

        // Botones Izquierdos
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        left.setBackground(Theme.PANEL);
        left.add(Box.createHorizontalStrut(10));
        posLabel = new JLabel("—");
        posLabel.setForeground(Theme.TEXT2);
        posLabel.setFont(Theme.FONT_SMALL);
        left.add(posLabel);

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

        nav.add(left);
        nav.add(center);
        nav.add(right);
        return nav;
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
        prefs.put(DIR_PREF_KEY, dir.getAbsolutePath());
        String shortPath = dir.getAbsolutePath();
        if (shortPath.length() > 65) shortPath = "…" + shortPath.substring(shortPath.length() - 62);
        dirLabel.setText(shortPath); //Etiqueta de la topBar
        startScan();
    }

    // ── Escaneo (en SwingWorker — nunca bloquea EDT) ──────────────────────────
    /**
     * @brief Comienza un escaneo en el directorio actualmente seleccionado y carga los componentes correspondientes con el resultado
     */
    private void startScan() {
        if (currentDir == null) return;
        if (activeScanner != null) activeScanner.stop();

        FilterOptions opts = filterBar.get();
        scanLabel.setText("Escaneando…");

        scanLabelInteger.incrementAndGet();
        int scanGen = scanLabelInteger.get();

        activeScanner = new FileScanner(
            currentDir,
            opts.typeFilter(),
            opts.recursive(),
            files -> {                       // onDone — ya en EDT via done()
                allFiles = files;
                scanLabel.setText(files.size() + " archivos encontrados");
                Timer t = new Timer(SHOW_SCAN_TIME, e -> {
                    if(scanLabelInteger.get() == scanGen){
                        scanLabel.setText("");   
                    }
            });
                t.setRepeats(false); t.start();
                applyFilters();
            },
            msg -> scanLabel.setText(msg)    // onProgress — ya en EDT via process()
        );
        activeScanner.execute();
    }

    // ── Filtrado / Ordenación (rápido, en EDT) ────────────────────────────────

    /**
     * @brief Aplica los filtros y manda a un archivo concreto
     * @param idx el indice de dicho archivo
     */
    private void applyFilters(int idx) {
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
        //fileList.populate(filtered);
        thumbStrip.populate(filtered, idx);

        if (!filtered.isEmpty()) selectByIndex(idx);
        else {
            currentIdx = -1;
            viewer.clear();
            viewerStatus.setText("—");
            posLabel.setText("—");
        }
    }

    /**
     * @brief Aplica los filtros y manda al inicio del array
     */
    private void applyFilters() {
        applyFilters(0);
    }

    // ── Selección / Navegación ────────────────────────────────────────────────
    /**
     * @brief selecciona un archivo por su indice en el vector de filtrados y actualiza la vista con este elemento
     */
    public void selectByIndex(int idx) {
        if (filtered.isEmpty()) return;

        idx = Math.max(0, Math.min(idx, filtered.size() - 1));
        int prevIdx = currentIdx; // Si venimos de currentIdx = -1, el viewer panel debería estar vacío 
        currentIdx = idx;
        MediaFile mf = filtered.get(idx);

        if (mf == viewer.getCurrent() && prevIdx != -1) return;

        viewer.load(mf);
        //metaPanel.load(mf);
        //fileList.highlight(idx);
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

    /**
     * @brief Pide confirmación para eliminar la imagen seleccionada actualmente (por índice) y de ser afirmativa
     *        la respuesta la elimina y actualiza la interfaz
     */
    private void deleteCurrentFile() {
        if (currentIdx < 0 || filtered.isEmpty()) return;
        MediaFile mf = filtered.get(currentIdx);
        int choice = JOptionPane.showConfirmDialog(this,
            "¿Eliminar permanentemente?\n\n" + mf.getName(),
            "Eliminar archivo", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) return;
        if (mf.getFile().delete()) {
            allFiles.remove(mf);
            applyFilters(currentIdx-1);
        } else {
            JOptionPane.showMessageDialog(this, "No se pudo eliminar el archivo.",
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * @brief Abre externamente un archivo concreto
     * @param file La ruta al archivo
     */
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
     * @brief Aplica un estilo predefinido (Estilo de navegación) a un botón 
     * @param b el botón
     */
    private void styleBtn(JButton b) {
        b.setBackground(Theme.ACCENT);
        b.setForeground(Theme.TEXT);
        b.setFont(Theme.FONT_MED_BOLD);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setPreferredSize(new Dimension(44, 32));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    /**
     * @brief Crea un botón de acento
     * @param text El texto del botón
     */
    private static JButton highlightButton(String text) {
        ThemedButton b = new ThemedButton(text, TextType.TERTIARY, FontSize.SMALL, Font.BOLD, ButtonType.HIGHLIGHT);
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
        b.setForeground(Theme.TEXT2);
        b.setFont(new Font(Theme.FONT_SYMBOL, Font.PLAIN, 10));
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    // ── Look & Feel ───────────────────────────────────────────────────────────
    /**
     * @brief Aplica los estilos propios de Theme al UIManager
     * @param theme el tema del que coger los colores
     */
    private static void applyLookAndFeel(Theme theme) {
        try {
            // Intentar cargar FlatLaf (Dark o Light) por reflexión o directamente
            boolean loaded = false;
            try {
                // Si tienes el JAR de FlatLaf en tu classpath:
                UIManager.setLookAndFeel("com.formdev.flatlaf.FlatDarkLaf"); 
                loaded = true;
                System.out.println("Flatlaf loaded");
            } catch (Exception e) {
                // Si no está FlatLaf, intentamos Nimbus
                try {
                    for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                        if ("Nimbus".equals(info.getName())) {
                            UIManager.setLookAndFeel(info.getClassName());
                            loaded = true;
                            System.out.println("Flatlaf loaded");
                            break;
                        }
                    }
                } catch (Exception ignored) {}
            }

            // Si fallaron los anteriores, cae al del sistema
            if (!loaded) {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                System.out.println("System look loaded");
            }

            // Colores globales (SE APLICAN DESPUÉS DE ESTABLECER EL LOOK AND FEEL)
            UIManager.put("Panel.background",            theme.getBackground());
            UIManager.put("ScrollBar.background",        theme.getAccent());
            UIManager.put("ScrollBar.thumb",             theme.getHighLight());
            
            // Claves globales para ComboBox
            UIManager.put("ComboBox.background",          theme.getInput());
            UIManager.put("ComboBox.foreground",          theme.getText1());
            UIManager.put("ComboBox.popupBackground",     theme.getBackground());
            UIManager.put("ComboBox.focusedBackground",   theme.getHighLight2());
            UIManager.put("ComboBox.selectionBackground", theme.getHighLight2());
            UIManager.put("ComboBox.selectionForeground", theme.getText1());
            // Color de selección en la lista desplegable (por si usa el componente List)
            UIManager.put("List.selectionBackground",     theme.getHighLight2());
            UIManager.put("List.selectionForeground",     theme.getText1());
            UIManager.put("Component.focusColor",         theme.getAccent());

            // Claves para las checkboxes
            UIManager.put("CheckBox.icon.borderColor",        theme.getBorder());
            UIManager.put("CheckBox.icon.background",         theme.getInput());
            UIManager.put("CheckBox.icon.selectedBackground", theme.getInput());
            UIManager.put("CheckBox.icon.checkmarkColor",     theme.getHighLight2());

            // Claves globales para Botones
            UIManager.put("Button.arc", 0);
            UIManager.put("Button.background", theme.getAccent());
            UIManager.put("Button.foreground", theme.getText1());
            
            // Claves para paneles auxiliares
            UIManager.put("OptionPane.background",       theme.getBackground());
            UIManager.put("OptionPane.messageForeground",theme.getText1());

            //Claves para Sliders
            UIManager.put("Slider.trackColor",       theme.getAccent());
            UIManager.put("Slider.trackValueColor",  theme.getHighLight());
            UIManager.put("Slider.thumbColor",       theme.getHighLight());
            UIManager.put("Slider.hoverThumbColor",  theme.getHighLight2());

        } catch (Exception ignored) {}
    }

    private void applyThemeToFrame(Theme theme) {
        applyLookAndFeel(theme);
        setBackground(theme.getBackground());
    }

    private void applyThemeToFrame() {
        applyThemeToFrame(ThemeManager.getInstance().getCurrentTheme());
    }

    private void changeTheme(){
        prefs.put(THEME_PREF_KEY, ThemeManager.getInstance().getCurrentTheme().getName());
    }
}