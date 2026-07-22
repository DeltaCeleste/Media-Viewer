package com.mediaviewer.ui.panels;

import com.mediaviewer.model.MediaFile;
import com.mediaviewer.util.Theme;
import com.mediaviewer.util.InitException;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;

public class VideoPanel extends JPanel {
    private JFXPanel jfxPanel;
    private MediaPlayer mediaPlayer;
    private MediaView mediaView;
    private StackPane root;

    // Botones de control
    private JButton playPauseButton;
    private JButton stopButton;
    private JButton fullScreenButton;
    private JSlider volumeSlider;
    private JSlider progressSlider;
    private JLabel timeLabel;
    
    // Estado de reproducción
    private boolean isPlaying = false;
    private boolean isDragging = false;

    // Control
    private CompletableFuture<Void> futuroInicializacion = new CompletableFuture<>();
    private volatile InitException excepcionInicializacion = null;

    // Callcack
    private JLabel statusLabel;   // inyectado desde fuera

    public void setStatusLabel(JLabel lbl) { this.statusLabel = lbl; }


    public VideoPanel(File videoPath, AtomicInteger gen, Consumer<MediaFile> onFallo) throws Exception {
        int preGen = gen.get();
        setLayout(new BorderLayout());
        setBackground(Theme.BG);

        // 1. Inicializar el panel de JavaFX
        jfxPanel = new JFXPanel();
        jfxPanel.setBackground(Theme.BG);
        add(jfxPanel, BorderLayout.CENTER);

        JPanel controlsPanel = createControlsPanel();
        add(controlsPanel, BorderLayout.SOUTH);

        if(gen.get() == preGen){
            jfxPanel.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    updateMediaViewSize();
                }
            });
            excepcionInicializacion = new InitException("Inicialización para comenzar pero no comenzada");
            Platform.setImplicitExit(false);
            Platform.runLater(() -> {
                try{
                    initFX(videoPath, gen, preGen, onFallo);
                } catch (Exception e) {
                    excepcionInicializacion = new InitException(e.getMessage(), -1);
                }
            });
        }  

    }

    /**
     * @brief Inicializa el video y el panel adaptable
     * @param videoFile el archivo a cargar
     */
    private void initFX(File videoFile, AtomicInteger gen, int preGen, Consumer<MediaFile> onFallo) throws Exception {
        excepcionInicializacion = new InitException("Inicialización correctamente en proceso pero no acabada", 1);
        try{
            if (!videoFile.exists()) {
                System.err.println("El archivo de video no existe: " + videoFile);
                throw new Exception("El archivo de video no existe");
            }

            if(gen.get() != preGen){ return; }

            Media media = new Media(videoFile.toURI().toString());
            mediaPlayer = new MediaPlayer(media);

            media.setOnError(() -> {
                System.err.println("Fallo en la creación de la media");
                excepcionInicializacion = new InitException("Fallo al crear media", -1);
                onFallo.accept(new MediaFile(videoFile));
            });

            mediaPlayer.setOnError(() -> {
                String msg = "Fallo en la creación del mediaPlayer: " + mediaPlayer.getError().getMessage();
                System.err.println(msg);
                excepcionInicializacion = new InitException(msg, -1);
                onFallo.accept(new MediaFile(videoFile));
            });

            mediaPlayer.setOnReady(() -> {
                Platform.runLater(() -> {
                    try{
                        mediaView = new MediaView(mediaPlayer);
                        mediaView.setPreserveRatio(true);

                        root = new StackPane();
                        root.setStyle("-fx-background-color: black;");
                        root.getChildren().add(mediaView);

                        Scene scene = new Scene(root);
                        jfxPanel.setScene(scene);

                        SwingUtilities.invokeLater(this::updateMediaViewSize);

                        setupMediaListeners();

                        futuroInicializacion.complete(null);
                        excepcionInicializacion = null;

                        // El Media está listo, su duración ya está disponible.
                        statusLabel.setText("Video - " + formatDuration(mediaPlayer.getTotalDuration()));
                    } catch (Exception e) {
                        onFallo.accept(new MediaFile(videoFile));
                    }
                    
                });
            });
        } catch (Exception e) {
            futuroInicializacion.completeExceptionally(e);
            throw e;
        }
        
    }

    /**
     * @brief función cerrojo para asegurar la inicialización antes de acceder a datos
     */
    public void esperarInicializacion(int time) throws InitException {
        try {
            futuroInicializacion.get(time, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw excepcionInicializacion;
        }
    }

    public boolean inicializado() {
        if(excepcionInicializacion == null){
            return true;
        }
        return false;
    }

    private JPanel createControlsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        panel.setBackground(new Color(50, 50, 50));
        
        // --- Botón Play/Pause ---
        playPauseButton = new JButton("▶");
        playPauseButton.setFont(new Font(Theme.FONT_SYMBOL, Font.BOLD, 16));
        playPauseButton.setForeground(Color.WHITE);
        playPauseButton.setBackground(new Color(70, 70, 70));
        playPauseButton.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        playPauseButton.addActionListener(e -> togglePlayPause());
        panel.add(playPauseButton);
        
        // --- Botón Stop ---
        stopButton = new JButton("⏹");
        stopButton.setFont(new Font(Theme.FONT_SYMBOL, Font.BOLD, 16));
        stopButton.setForeground(Color.WHITE);
        stopButton.setBackground(new Color(70, 70, 70));
        stopButton.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        stopButton.addActionListener(e -> stopVideo());
        panel.add(stopButton);
        
        // --- Barra de progreso ---
        progressSlider = new JSlider(0, 100, 0);
        progressSlider.setPreferredSize(new Dimension(250, 20));
        progressSlider.setBackground(new Color(50, 50, 50));
        progressSlider.addChangeListener(e -> {
            if (progressSlider.getValueIsAdjusting()) {
                isDragging = true;
            } else if (isDragging) {
                isDragging = false;
                seekToPosition(progressSlider.getValue() / 100.0);
            }
        });
        panel.add(progressSlider);
        
        // --- Etiqueta de tiempo ---
        timeLabel = new JLabel("00:00 / 00:00");
        timeLabel.setForeground(Color.WHITE);
        timeLabel.setFont(new Font("Monospaced", Font.PLAIN, 12));
        panel.add(timeLabel);
        
        // --- Control de volumen ---
        JLabel volumeIcon = new JLabel("🔊");
        volumeIcon.setForeground(Color.WHITE);
        panel.add(volumeIcon);
        
        volumeSlider = new JSlider(0, 100, 100);
        volumeSlider.setPreferredSize(new Dimension(80, 20));
        volumeSlider.setBackground(new Color(50, 50, 50));
        volumeSlider.addChangeListener(e -> {
            if (!volumeSlider.getValueIsAdjusting()) {
                setVolume(volumeSlider.getValue() / 100.0);
            }
        });
        panel.add(volumeSlider);
        
        // --- Botón Pantalla Completa (opcional) ---
        fullScreenButton = new JButton("⛶");
        fullScreenButton.setFont(new Font("Arial", Font.BOLD, 16));
        fullScreenButton.setForeground(Color.WHITE);
        fullScreenButton.setBackground(new Color(70, 70, 70));
        fullScreenButton.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        fullScreenButton.addActionListener(e -> toggleFullScreen());
        panel.add(fullScreenButton);
        
        return panel;
    }

    /**
     * @brief Establece los listenners para la barra de progreso y el autofinalizar el video
     */
    private void setupMediaListeners() {
        // Listener para actualizar el tiempo y la barra de progreso
        mediaPlayer.currentTimeProperty().addListener((obs, oldVal, newVal) -> {
            if (!isDragging) {
                Duration duration = mediaPlayer.getTotalDuration();
                if (duration != null && duration.greaterThan(Duration.ZERO)) {
                    double progress = newVal.toSeconds() / duration.toSeconds();
                    SwingUtilities.invokeLater(() -> {
                        progressSlider.setValue((int) (progress * 100));
                        updateTimeLabel(newVal, duration);
                    });
                }
            }
        });
        
        // Listener para cuando termina el video
        mediaPlayer.setOnEndOfMedia(() -> {
            SwingUtilities.invokeLater(() -> {
                isPlaying = false;
                playPauseButton.setText("▶");
                progressSlider.setValue(0);
            });
        });
    }

    // ── Adaptación de tamaño ──────────────────────────────────────────────────────
    /**
     * @brief actualiza el tamaño del video en función del del panel
     */
    private void updateMediaViewSize() {
        if (mediaView != null && jfxPanel != null) {
            Platform.runLater(() -> {
                mediaView.setFitWidth(jfxPanel.getWidth());
                mediaView.setFitHeight(jfxPanel.getHeight());
            });
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(640, 360); // Tamaño preferido 16:9
    }

    @Override
    public void doLayout() {
        super.doLayout();
        // Cuando Swing reorganicé el layout, actualizamos el tamaño del video
        updateMediaViewSize();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        // Cuando el panel se añade a un contenedor, también actualizamos
        SwingUtilities.invokeLater(this::updateMediaViewSize);
    }

    // ── Controles ────────────────────────────────────────────────────────────────
    /**
     * @brief Si el video esta empezado lo pausa y si está pausado lo reanuda, cambiando el botón en su proceso
     */
    private void togglePlayPause() {
        if (mediaPlayer == null) return;
        
        Platform.runLater(() -> {
            if (isPlaying) {
                mediaPlayer.pause();
                SwingUtilities.invokeLater(() -> playPauseButton.setText("▶"));
            } else {
                mediaPlayer.play();
                SwingUtilities.invokeLater(() -> playPauseButton.setText("⏸"));
            }
            isPlaying = !isPlaying;
        });
    }

    private void stopVideo() {
        if (mediaPlayer == null) return;
        
        Platform.runLater(() -> {
            mediaPlayer.stop();
            isPlaying = false;
            SwingUtilities.invokeLater(() -> {
                playPauseButton.setText("▶");
                progressSlider.setValue(0);
                timeLabel.setText("00:00:00 / 00:00:00");
            });
        });
    }

    private void setVolume(double volume) {
        if (mediaPlayer != null) {
            Platform.runLater(() -> mediaPlayer.setVolume(volume));
        }
    }

    private void seekToPosition(double progress) {
        if (mediaPlayer == null) return;
        
        Platform.runLater(() -> {
            Duration duration = mediaPlayer.getTotalDuration();
            if (duration != null && duration.greaterThan(Duration.ZERO)) {
                double seconds = progress * duration.toSeconds();
                mediaPlayer.seek(Duration.seconds(seconds));
            }
        });
    }

    private void toggleFullScreen() {
        // Obtener el JFrame contenedor
        Container parent = this.getParent();
        while (!(parent instanceof JFrame) && parent != null) {
            parent = parent.getParent();
        }
        
        if (parent instanceof JFrame) {
            JFrame frame = (JFrame) parent;
            GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice();
            
            if (device.isFullScreenSupported()) {
                device.setFullScreenWindow(
                    device.getFullScreenWindow() == null ? frame : null
                );
            }
        }
    }

    private void updateTimeLabel(Duration current, Duration total) {
        String currentStr = formatDuration(current);
        String totalStr = formatDuration(total);
        SwingUtilities.invokeLater(() -> 
            timeLabel.setText(currentStr + " / " + totalStr)
        );
    }

    /**
     * @brief formatea una duración a formato hh:mm:ss
     */
    private String formatDuration(Duration duration) {
        if (duration == null) return "00:00:00";
        long seconds = (long) duration.toSeconds();
        long hours = seconds / (60*60);
        long mins = seconds / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, mins, secs);
    }

    // ── Controles del mediaplayer ───────────────────────────────────────────────────
    /**
     * @brief Reproduce el video desde el mediaplayer
     */
    public void play() {
        Platform.runLater(() -> mediaPlayer.play());
    }

    /**
     * @brief Pausa el video desde el mediaplayer
     */
    public void pause() {
        Platform.runLater(() -> mediaPlayer.pause());
    }

    /**
     * @brief Para el video desde el mediaplayer
     */
    public void stop() {
        Platform.runLater(() -> mediaPlayer.stop());
    }

    // ── Otros ────────────────────────────────────────────────────────────────
    public String getCurrentVidDurationStr() throws Exception{ 
        try{
            esperarInicializacion(5); 
            return formatDuration(mediaPlayer.getTotalDuration()); 
        } catch (Exception e) {
            throw e;
        }
    }
    
    /**
     * @brief Para el video y elimina el mediaplayer
     */
    public void dispose() {
        Platform.runLater(() -> {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.dispose();
            }
        });
    }
}