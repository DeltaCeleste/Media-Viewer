package com.mediaviewer.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;

import com.formdev.flatlaf.extras.FlatInspector;
import com.ibm.icu.text.Collator;
import com.ibm.icu.util.ULocale;
import com.mediaviewer.engine.FileScanner;
import com.mediaviewer.model.FilterOptions;
import com.mediaviewer.model.MediaFile;
import com.mediaviewer.ui.components.ThemeToggleButton;
import com.mediaviewer.ui.components.ThemedButton;
import com.mediaviewer.ui.components.ThemedLabel;
import com.mediaviewer.ui.components.ThemedPanel;
import com.mediaviewer.ui.panels.FilterBar;
import com.mediaviewer.ui.panels.ThumbnailStrip;
import com.mediaviewer.ui.panels.ViewerPanel;
import com.mediaviewer.util.Theme;
import com.mediaviewer.util.ThemeManager;
import com.mediaviewer.util.ThemeUtils;

/**
 * Ventana principal de mediaviewer.
 *
 * Reglas de threading:
 * ┌────────────────────────────────────────────────────────┐
 * │ EDT (Event Dispatch Thread) │
 * │ • Todo lo que actualiza widgets │
 * │ • Filtrado/ordenación (rápido, en memoria) │
 * │ • Recibe callbacks de workers vía invokeLater/done() │
 * ├────────────────────────────────────────────────────────┤
 * │ SwingWorker / ExecutorService (hilos de fondo) │
 * │ • Escaneo de disco (FileScanner) │
 * │ • Carga de imágenes (ViewerPanel) │
 * │ • Miniaturas (ThumbnailStrip) │
 * │ • Lectura de metadatos (MetadataPanel) │
 * └────────────────────────────────────────────────────────┘
 */
public class MainWindow extends JFrame {

    // —— Constantes
    private final static int SHOW_SCAN_TIME = 5000;

    // ── Estado ────────────────────────────────────────────────────────────────
    private List<MediaFile> allFiles = new ArrayList<>();
    private List<MediaFile> filtered = new ArrayList<>();
    private HashSet<MediaFile> selected = new HashSet<>();
    private int currentIdx = -1;
    private File currentDir = null;
    private FileScanner activeScanner = null;

    // ── Persistencia ─────────────────────────────────────────────────────────
    private final Preferences prefs = Preferences.userNodeForPackage(MainWindow.class);
    private final static String THEME_PREF_KEY = "theme";
    private final static String DIR_PREF_KEY   = "lastDir";

    // ── Paneles ───────────────────────────────────────────────────────────────
    private ViewerPanel viewer;
    private ThumbnailStrip thumbStrip;
    //private FileListPanel  fileList;
    //private MetadataPanel  metaPanel;
    private FilterBar      filterBar;
    private ThemedLabel    dirLabel;
    private ThemedLabel    scanLabel;
    private ThemedLabel    posLabel;
    private ThemedLabel    viewerStatus;
    private ThemedLabel    selectStatus;

    // ── Control ─────────────────────────────────────────────────────────────
    private final AtomicInteger scanLabelInteger = new AtomicInteger(0);


    public MainWindow() {
        super("Meδia Viewer");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(1420, 900);
        setMinimumSize(new Dimension(960, 660));
        setLocationRelativeTo(null);

        String themeName = prefs.get(THEME_PREF_KEY, Theme.LIGHT.name());
        try {
            ThemeManager.setTheme(Theme.valueOf(themeName));
        } catch (IllegalArgumentException e) {
            ThemeManager.setTheme(Theme.LIGHT);
        }

        FlatInspector.install("ctrl shift alt F");
        applyLookAndFeel(ThemeManager.getInstance().getCurrentTheme());
        buildUI();

        bindKeys();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                shutdown();
            }
        });

        // Restaurar última carpeta
        String lastDir = prefs.get(DIR_PREF_KEY, "");
        if (!lastDir.isEmpty()) {
            File f = new File(lastDir);
            if (f.isDirectory())
                SwingUtilities.invokeLater(() -> loadDirectory(f));
        }

        // Registrar frame como listener
        ThemeManager.getInstance().addListener(this::applyThemeToFrame);
    }

    // ── utilidades ────────────────────────────────────────────────────────────
    /**
     * @brief Devuelve el archivo actualmente seleccionado por currentIdx
     * @return el archivo (MediaFile)
     */
    private MediaFile getCurrentMediaFile() {
        return getMediaFileByIdx(currentIdx);
    }

    /**
     * @brief Devuelve el file (ruta) al archivo actualmente seleccionado
     */
    private File getCurrentFile() {
        return getCurrentMediaFile().getFile();
    }

    /**
     * @brief Devuelve el archivo referenciado en los filtrados por el índice
     *        provisto
     * @param idx el índice
     * @return el archivo referenciado
     */
    private MediaFile getMediaFileByIdx(int idx) {
        return filtered.get(idx);
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    /**
     * @brief Construye la Interfaz gráfica a partir de los paneles
     */
    private void buildUI() {  
        // ── Barra superior ──
        ThemedPanel topBar = new ThemedPanel(new BorderLayout());

        ThemedPanel topLeft = new ThemedPanel(new FlowLayout(FlowLayout.LEFT, 10, 7));

        ThemedLabel logo = new ThemedLabel("Meδia Viewer", ThemeUtils.TextType.PRIMARY, ThemeUtils.FontSize.MED, Font.BOLD, ThemeUtils.FontType.SYMBOL);
        topLeft.addToPanel(logo);

        ThemedButton openBtn = new ThemedButton("Abrir carpeta 📂", ThemeUtils.TextType.TERTIARY, ThemeUtils.FontSize.SMALL, Font.BOLD, ThemeUtils.FontType.EMOJI, ThemeUtils.ButtonType.HIGHLIGHT);
        openBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        openBtn.getButton().addActionListener(evt -> chooseDirectory());
        topLeft.addToPanel(openBtn);

        dirLabel = new ThemedLabel("Sin carpeta — Ctrl+O para abrir", ThemeUtils.TextType.SECONDARY, ThemeUtils.FontSize.SMALL, Font.PLAIN, ThemeUtils.FontType.BASIC);
        topLeft.addToPanel(dirLabel);

        scanLabel = new ThemedLabel("", ThemeUtils.TextType.SUCCESS, ThemeUtils.FontSize.SMALL, Font.PLAIN, ThemeUtils.FontType.BASIC);
        // empujar a la derecha
        topLeft.addToPanel(Box.createHorizontalStrut(30));
        topLeft.addToPanel(scanLabel);

        boolean dark = ThemeManager.getInstance().getCurrentTheme().equals(Theme.DARK);
        ThemeToggleButton themeChanger = new ThemeToggleButton(this::changeTheme, dark);

        topBar.addToPanel(topLeft, BorderLayout.WEST);
        topBar.addToPanel(themeChanger, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        // ── Barra de filtros ──
        filterBar = new FilterBar(this::applyFilters, this::startScan);
        add(filterBar, BorderLayout.AFTER_LAST_LINE); // provisional, se reordena

        // ── Panel principal (split) ──
        viewer = new ViewerPanel(getRootPane()::requestFocusInWindow);
        viewerStatus = new ThemedLabel("Selecciona una carpeta para empezar", ThemeUtils.TextType.SECONDARY, ThemeUtils.FontSize.SMALL, Font.PLAIN, ThemeUtils.FontType.BASIC);
        viewer.setStatusLabel(viewerStatus);

        /*
         * fileList = new FileListPanel(this::selectByIndex);
         * metaPanel = new MetadataPanel(this::onSaved);
         * 
         * JSplitPane leftSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
         * fileList, buildCenterPanel());
         * leftSplit.setDividerLocation(210);
         * leftSplit.setDividerSize(5);
         * leftSplit.setBorder(null);
         * leftSplit.setBackground(Theme.BG);
         * 
         * JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
         * leftSplit, metaPanel);
         * mainSplit.setDividerLocation(getWidth() - 300);
         * mainSplit.setDividerSize(5);
         * mainSplit.setBorder(null);
         * mainSplit.setBackground(Theme.BG);
         * mainSplit.setResizeWeight(1.0);
         */

        // Layout con filtros arriba y split en centro
        ThemedPanel body = new ThemedPanel(new BorderLayout(), ThemeUtils.PanelType.BACKGROUND);
        body.addToPanel(filterBar, BorderLayout.NORTH);
        //body.add(mainSplit, BorderLayout.CENTER);
        body.addToPanel(buildCenterPanel(), BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        // ── Barra inferior (navegación) ──
        add(buildNavBar(), BorderLayout.SOUTH);
    }

    /**
     * @brief Contruye el panel central compuesto por:
     *        - El vector de thumbnails
     *        - El campo de display para la imagen
     *        - La barra con los botones que permiten hacer zoom y el tamaño de la
     *        imagen
     * @return El panel construido
     */
    private ThemedPanel buildCenterPanel() {
        thumbStrip = new ThumbnailStrip(this::selectByIndex, getRootPane()::requestFocusInWindow);
        thumbStrip.setName("ThumbStrip");

        ThemedPanel center = new ThemedPanel(new BorderLayout(), ThemeUtils.PanelType.BACKGROUND, BorderFactory.createMatteBorder(1, 0, 0, 0, Color.WHITE));

        // Status bar del visor (debajo del canvas)
        ThemedPanel viewerBar = new ThemedPanel(new GridLayout());
        
        // Botones zoom
        ThemedPanel zoomButtons = new ThemedPanel(new FlowLayout(FlowLayout.CENTER, 8, 3));
        for (String[] b : new String[][]{{"−","zoom−"},{"⟳","reset"},{"+","zoom+"},{"↗","open"}}) {
            ThemedButton btn = new ThemedButton(b[0], ThemeUtils.TextType.PRIMARY, ThemeUtils.FontSize.MED, Font.BOLD, ThemeUtils.FontType.SYMBOL, ThemeUtils.ButtonType.ACCENT);
            btn.setBorderPainted(false);
            btn.setFocusPainted(false);
            btn.setOpaque(true);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.getButton().addActionListener(evt -> handleViewerAction(b[1]));
            zoomButtons.addToPanel(btn);
        }        

        selectStatus = new ThemedLabel(selected.size() + " archivos seleccionados", ThemeUtils.TextType.SECONDARY, ThemeUtils.FontSize.SMALL, Font.PLAIN, ThemeUtils.FontType.BASIC);

        viewerBar.addToPanel(viewerStatus);

        viewerBar.addToPanel(zoomButtons); 

        JPanel leftwrapper = new JPanel(new BorderLayout());
        leftwrapper.setOpaque(false);
        leftwrapper.add(selectStatus, BorderLayout.EAST);
        

        viewerBar.addToPanel(leftwrapper);
        viewerBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.WHITE));

        center.addToPanel(viewer,    BorderLayout.CENTER);
        center.addToPanel(viewerBar, BorderLayout.SOUTH);
        center.addToPanel(thumbStrip, BorderLayout.NORTH);

        return center;
    }

    /**
     * @brief Construye la barra de navegación con los botones pertinentes
     * @return El panel construido
     */
    private ThemedPanel buildNavBar() {
        ThemedPanel nav = new ThemedPanel(new GridLayout());
        nav.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.WHITE));

        // Botones centrales
        ThemedPanel center = new ThemedPanel(new FlowLayout(FlowLayout.CENTER, 6, 6));
        for (String[] b : new String[][]{{"⏮","first"},{"◀","prev"},{"▶","next"},{"⏭","last"}}) {
            ThemedButton btn = new ThemedButton(b[0], ThemeUtils.TextType.PRIMARY, ThemeUtils.FontSize.MED, Font.BOLD, ThemeUtils.FontType.SYMBOL, ThemeUtils.ButtonType.ACCENT);
            btn.setBorderPainted(false);
            btn.setFocusPainted(false);
            btn.setOpaque(true);
            btn.setPreferredSize(new Dimension(44, 32));
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.getButton().addActionListener(evt -> handleNav(b[1]));
            center.addToPanel(btn);
        }


        // Botones Izquierdos
        ThemedPanel left = new ThemedPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        left.addToPanel(Box.createHorizontalStrut(10));
        posLabel = new ThemedLabel("—", ThemeUtils.TextType.SECONDARY, ThemeUtils.FontSize.SMALL, Font.PLAIN, ThemeUtils.FontType.BASIC);
        left.addToPanel(posLabel);

        // Botones derechos
        ThemedPanel right = new ThemedPanel(new FlowLayout(FlowLayout.RIGHT, 6, 6));
        right.setOpaque(false);

        ThemedButton refresh = ghostButton("↺  Refrescar  F5", ThemeUtils.TextType.PRIMARY);
        refresh.getButton().addActionListener(evt -> startScan());
        right.addToPanel(refresh);

        ThemedButton delete = ghostButton("🗑  Eliminar  Del", ThemeUtils.TextType.ERROR);
        delete.getButton().addActionListener(evt -> deleteCurrentFile());
        right.addToPanel(delete);

        nav.addToPanel(left);
        nav.addToPanel(center);
        nav.addToPanel(right);
        return nav;
    }

    // ── Teclas ────────────────────────────────────────────────────────────────
    /**
     * @brief Establece la lógica de la aplicación a algunas teclas y eventos de
     *        teclado para navegación por teclado
     */
    private void bindKeys() {
        JRootPane rp = getRootPane();

        // Listener para recuperar el foco al clicar fuera de un botón
        rp.setFocusable(true);
        rp.addMouseListener(new MouseAdapter() { @Override public void mousePressed(MouseEvent e) { rp.requestFocusInWindow(); } });

        // Movimiento
        // =====================================================================================================
        KeyStroke left = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0); // Una a la izquierda
        KeyStroke right = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0); // Una a la derecha
        KeyStroke ctrlleft = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.CTRL_DOWN_MASK); // A la 0
        KeyStroke ctrlright = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.CTRL_DOWN_MASK); // A la última

        rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(left, "prev");
        rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(right, "next");
        rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ctrlleft, "first");
        rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ctrlright, "last");

        rp.getActionMap().put("prev",  new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { goTo(currentIdx - 1); } });
        rp.getActionMap().put("next",  new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { goTo(currentIdx + 1); } });
        rp.getActionMap().put("first", new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { goTo(0); } });
        rp.getActionMap().put("last",  new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { goTo(filtered.size() - 1); } });

        // Selección
        // =======================================================================================================
        KeyStroke up = KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0); // Añadir o eliminar el archivo actual a la selección
        KeyStroke ctrlA = KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK); // Seleccionar todo

        rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(up, "select");
        rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ctrlA, "allselect");

        rp.getActionMap().put("select",    new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { toogleSelect(currentIdx); } });
        rp.getActionMap().put("allselect", new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { selectAll(); } });

        // Borrado
        // =========================================================================================================
        KeyStroke del      = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);                          // Borrar el archivo o selección actual
        KeyStroke ctrldel  = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, InputEvent.CTRL_DOWN_MASK);  // Borrar todo salvo la selección actual
        KeyStroke shiftdel = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, InputEvent.SHIFT_DOWN_MASK); // Borrar todo salvo la selección actual hasta el current

        rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(del,      "delete");
        rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ctrldel,  "inversedelete");
        rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(shiftdel, "inversecappeddelete");

        rp.getActionMap().put("delete",              new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { deleteCurrentSelection(); } });
        rp.getActionMap().put("inversedelete",       new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { deleteButcurrentSelection(false); } });
        rp.getActionMap().put("inversecappeddelete", new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { deleteButcurrentSelection(true); } });        

        // Otras funciones
        // =================================================================================================
        KeyStroke f12 = KeyStroke.getKeyStroke(KeyEvent.VK_F12, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK); // DEBUG:
                                                                                                                         // funciones
                                                                                                                         // beta
                                                                                                                         // para
                                                                                                                         // pruebas
                                                                                                                         // seguras
        KeyStroke f5 = KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0); // Refrescar
        KeyStroke ctrlO = KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK); // Abrir carpeta
        KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0); // Abrir el archivo actual externamente

        rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(f12, "test");
        rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(f5, "refresh");
        rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ctrlO, "open");
        rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(enter, "external");

        rp.getActionMap().put("test",     new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { System.out.println("Nada que probar"); } });
        rp.getActionMap().put("refresh",  new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { startScan(); } });
        rp.getActionMap().put("open",     new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { chooseDirectory(); } });
        rp.getActionMap().put("external", new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { openExternally(getCurrentFile()); } });

    }

    // ── Selección ────────────────────────────────────────────────────────────
    /**
     * @brief Introduce el índice de un archivo al conjunto seleccionados o lo saca
     *        si ya estaba seleccionado
     * @param idx el índice del archivo en filtered
     */
    private void toogleSelect(int idx) {
        MediaFile mf = getMediaFileByIdx(idx);
        if (selected.contains(mf)) {
            removeSelected(mf);
            thumbStrip.deselect(idx);
        } else {
            addSelected(mf);
            thumbStrip.select(idx);
        }
        selectStatus.setText(selected.size() + " archivos seleccionados");
    }

    /**
     * @brief Introduce el índice de un archivo al grupo de seleccionados
     * @param mf el archivo a introducir
     */
    private void addSelected(MediaFile mf) {
        if (!selected.contains(mf)) {
            selected.add(mf);

            int scanGen = scanLabelInteger.incrementAndGet();
            scanLabel.setType(ThemeUtils.TextType.SUCCESS);
            scanLabel.setText(mf.getName() + " seleccionado");
            Timer t = new Timer(SHOW_SCAN_TIME, e -> {
                if (scanLabelInteger.get() == scanGen) {
                    scanLabel.setText("");
                }
            });
            t.setRepeats(false);
            t.start();
            //System.out.println(mf.getName() + " seleccionado");
        }
    }

    /**
     * @brief Retira el índice de un archivo al grupo de seleccionados
     * @param mf el archivo a eliminar
     */
    private void removeSelected(MediaFile mf) {
        if (selected.contains(mf)) {
            selected.remove(mf);

            int scanGen = scanLabelInteger.incrementAndGet();
            scanLabel.setType(ThemeUtils.TextType.WARNING);
            scanLabel.setText(mf.getName() + " deseleccionado");
            Timer t = new Timer(SHOW_SCAN_TIME, e -> {
                if (scanLabelInteger.get() == scanGen) {
                    scanLabel.setText("");
                }
            });
            t.setRepeats(false);
            t.start();
            //System.out.println(mf.getName() + " deseleccionado");
        }
    }

    /**
     * @brief vacía el conjunto de seleccionados
     */
    private void clearSelected() {
        thumbStrip.clearSelected();
        selected.clear();
    }

    /**
     * @brief encuentra el índice del archivo más cercano (hacia atrás en filtered)
     *        que no este seleccionado y ajusta este indice simulando un delete
     * @return dicho indice
     */
    private int findClosestIdx() {
        if (selected.size() == filtered.size())
            return -1; // Estamos ante una selección total
        int idx = currentIdx;
        int candidato = -1;
        int anteriores = 0;
        while (idx >= 0) {
            if (!selected.contains(getMediaFileByIdx(idx)) && candidato == -1) {
                candidato = idx;
            } else {
                if (candidato != -1 && selected.contains(getMediaFileByIdx(idx))) {
                    anteriores++;
                }
            }
            idx--;

        }
        System.err.println(candidato - anteriores);
        if (candidato == -1)
            return 0;
        else
            return candidato - anteriores;
    }

    /**
     * @brief selecciona todas las imagenes del filtered actual
     */
    private void selectAll() {
        selected.addAll(filtered);
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
        if (shortPath.length() > 65)
            shortPath = "…" + shortPath.substring(shortPath.length() - 62);
        dirLabel.setText(shortPath); // Etiqueta de la topBar
        startScan();
    }

    // ── Escaneo (en SwingWorker — nunca bloquea EDT) ──────────────────────────
    /**
     * @brief Comienza un escaneo en el directorio actualmente seleccionado y carga
     *        los componentes correspondientes con el resultado
     */
    private void startScan() {
        if (currentDir == null)
            return;
        if (activeScanner != null)
            activeScanner.stop();

        FilterOptions opts = filterBar.get();
        scanLabel.setType(ThemeUtils.TextType.WARNING);
        scanLabel.setText("Escaneando…");

        scanLabelInteger.incrementAndGet();
        int scanGen = scanLabelInteger.get();

        activeScanner = new FileScanner(
                currentDir,
                opts.typeFilter(),
                opts.recursive(),
                files -> { // onDone — ya en EDT via done()
                    allFiles = files;
                    scanLabel.setType(ThemeUtils.TextType.SUCCESS);
                    scanLabel.setText(files.size() + " archivos encontrados");
                    Timer t = new Timer(SHOW_SCAN_TIME, e -> {
                        if (scanLabelInteger.get() == scanGen) {
                            scanLabel.setText("");
                        }
                    });
                    t.setRepeats(false);
                    t.start();
                    applyFilters();
                },
                msg -> scanLabel.setText(msg) // onProgress — ya en EDT via process()
        );
        activeScanner.execute();
    }

    // ── Filtrado / Ordenación (rápido, en EDT) ────────────────────────────────

    /**
     * @brief Aplica los filtros y manda a un archivo concreto
     * @param idx el indice de dicho archivo
     */
    private void applyFilters(int idx) {
        clearSelected(); // limpiamos los seleccionados pues filtered puede cambiar.
        FilterOptions opts = filterBar.get();
        List<MediaFile> items = new ArrayList<>(allFiles);

        // Filtro de tipo
        items = switch (opts.typeFilter()) {
            case "Imágenes" -> items.stream()
                    .filter(m -> m.getType() == MediaFile.MediaType.IMAGE)
                    .collect(Collectors.toList());
            case "GIFs" -> items.stream()
                    .filter(m -> m.getType() == MediaFile.MediaType.GIF)
                    .collect(Collectors.toList());
            case "Videos" -> items.stream()
                    .filter(m -> m.getType() == MediaFile.MediaType.VIDEO)
                    .collect(Collectors.toList());
            default -> items;
        };

        // Filtro de búsqueda
        if (!opts.searchText().isEmpty()) {
            String q = opts.searchText();
            items = items.stream()
                    .filter(m -> m.getName().toLowerCase().contains(q))
                    .collect(Collectors.toList());
        }

        // Ordenación
        ULocale locale = new ULocale(ULocale.getDefault().getName() + "@colNumeric=yes");
        Collator collator = Collator.getInstance(locale);
        Comparator<String> naturalOrder = (s1, s2) -> collator.compare(s1, s2);
        Comparator<MediaFile> cmp = switch (opts.sortKey()) {
            case "Nombre ↓" -> Comparator.comparing(MediaFile::getName,
                    naturalOrder).reversed();
            case "Fecha ↑" -> Comparator.comparingLong(MediaFile::getLastModified);
            case "Fecha ↓" -> Comparator.comparingLong(MediaFile::getLastModified).reversed();
            case "Tamaño ↑" -> Comparator.comparingLong(MediaFile::getSize);
            case "Tamaño ↓" -> Comparator.comparingLong(MediaFile::getSize).reversed();
            default -> Comparator.comparing(MediaFile::getName,
                    naturalOrder);
        };
        items.sort(cmp);

        filtered = items;
        filterBar.setCount(filtered.size(), allFiles.size());
        // fileList.populate(filtered);
        thumbStrip.populate(filtered, idx);

        if (!filtered.isEmpty())
            selectByIndex(idx);
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

    // ── Selección (aplicación) / Navegación
    // ────────────────────────────────────────────────
    /**
     * @brief selecciona un archivo por su indice en el vector de filtrados y
     *        actualiza la vista con este elemento
     */
    public void selectByIndex(int idx) {
        if (filtered.isEmpty())
            return;

        idx = Math.max(0, Math.min(idx, filtered.size() - 1));
        int prevIdx = currentIdx; // Si venimos de currentIdx = -1, el viewer panel debería estar vacío
        currentIdx = idx;
        MediaFile mf = getCurrentMediaFile();

        if (mf == viewer.getCurrent() && prevIdx != -1)
            return;

        viewer.load(mf);
        // metaPanel.load(mf);
        // fileList.highlight(idx);
        thumbStrip.highlight(idx);
        posLabel.setText((idx + 1) + " / " + filtered.size() + "   " + mf.getName());
    }

    private void goTo(int idx) {
        selectByIndex(idx);
    }

    // ── Acciones ──────────────────────────────────────────────────────────────
    /**
     * @brief Lógica para los botones de navegación rápida
     * @param cmd La cadena que identifica el comando
     */
    private void handleNav(String cmd) {
        switch (cmd) {
            case "prev" -> goTo(currentIdx - 1);
            case "next" -> goTo(currentIdx + 1);
            case "first" -> goTo(0);
            case "last" -> goTo(filtered.size() - 1);
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
            case "open" -> {
                if (currentIdx >= 0)
                    openExternally(getCurrentFile());
            }
        }
    }

    private void onSaved(MediaFile mf) {
        // Recargar lista en caso de renombre
        //fileList.populate(filtered);
        //fileList.highlight(currentIdx);
        if (currentIdx >= 0 && currentIdx < filtered.size())
            posLabel.setText((currentIdx+1) + " / " + filtered.size() + "   " + mf.getName());
    }

    /**
     * @brief borra todo filtered menos la seleccion actual
     * @param upToCurrent si true, solo borra hasta el archivo marcado por currentIdx
     */
    private void deleteButcurrentSelection(boolean upToCurrent) {
        HashSet<MediaFile> save = new HashSet<>(selected);
        clearSelected();
        selected.addAll(filtered);
        if(upToCurrent) selected.removeAll(filtered.subList(currentIdx+1, filtered.size()));
        selected.removeAll(save);
        if (!deleteCurrentSelection()) {
            clearSelected();
            selected.addAll(save);
        }
    }

    /**
     * @brief borra la selección actual, o si no hay seleccionadas, la imagen actual
     */
    private boolean deleteCurrentSelection() {
        if (selected.isEmpty()) {
            return deleteCurrentFile();
        } else {
            int choice = JOptionPane.showConfirmDialog(this,
                    "¿Eliminar permanentemente los " + selected.size() + " elementos seleccionados?\n",
                    "Eliminar seleccion de archivos", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (choice != JOptionPane.YES_OPTION)
                return false;
            ;
            int errores = 0;
            int newidx = findClosestIdx();

            for (MediaFile mf : selected) {
                if (mf.getFile().delete()) {
                    allFiles.remove(mf);
                } else {
                    errores++;
                }
            }
            if (errores != 0) {
                JOptionPane.showMessageDialog(this, "Hubo un problema eliminando " + errores + " archivos",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
            applyFilters(newidx);
            return true;
        }
    }

    /**
     * @brief Pide confirmación para eliminar la imagen seleccionada actualmente
     *        (por índice) y de ser afirmativa
     *        la respuesta la elimina y actualiza la interfaz
     */
    private boolean deleteCurrentFile() {
        if (currentIdx < 0 || filtered.isEmpty())
            return false;
        MediaFile mf = getCurrentMediaFile();
        int choice = JOptionPane.showConfirmDialog(this,
                "¿Eliminar permanentemente?\n\n" + mf.getName(),
                "Eliminar archivo", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION)
            return false;
        if (mf.getFile().delete()) {
            allFiles.remove(mf);
            applyFilters(currentIdx - 1);
        } else {
            JOptionPane.showMessageDialog(this, "No se pudo eliminar el archivo.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    /**
     * @brief Abre externamente un archivo concreto
     * @param file La ruta al archivo
     */
    private void openExternally(File file) {
        try {
            Desktop.getDesktop().open(file);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al abrir: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Cierre ────────────────────────────────────────────────────────────────
    /**
     * @brief Cierra la aplicación cerrando los recursos necesarios.
     */
    private void shutdown() {
        if (activeScanner != null)
            activeScanner.stop();
        viewer.shutdown();
        thumbStrip.shutdown();
        dispose();
        System.exit(0);
    }

    // ── Helpers de estilo ─────────────────────────────────────────────────────
    /**
     * @brief Crea un botón transparente
     * @param text El texto del botón
     */
    private static ThemedButton ghostButton(String text, ThemeUtils.TextType type) {
        ThemedButton b = new ThemedButton(text, type, ThemeUtils.FontSize.SMALL, Font.PLAIN, ThemeUtils.FontType.SYMBOL, ThemeUtils.ButtonType.PANEL);
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
            } catch (Exception e) {
                // Si no está FlatLaf, intentamos Nimbus
                try {
                    for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                        if ("Nimbus".equals(info.getName())) {
                            UIManager.setLookAndFeel(info.getClassName());
                            loaded = true;
                            break;
                        }
                    }
                } catch (Exception ignored) {
                }
            }

            // Si fallaron los anteriores, cae al del sistema
            if (!loaded) {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
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

        } catch (Exception ignored) {
        }
    }

    private void applyThemeToFrame(Theme theme) {
        applyLookAndFeel(theme);
        setBackground(theme.getBackground());
    }

    private void applyThemeToFrame() {
        applyThemeToFrame(ThemeManager.getInstance().getCurrentTheme());
    }

    private void changeTheme(){
        ThemeManager.getInstance().toggleLightDark();
        applyThemeToFrame();
        prefs.put(THEME_PREF_KEY, ThemeManager.getInstance().getCurrentTheme().getThemeName());
    }
}