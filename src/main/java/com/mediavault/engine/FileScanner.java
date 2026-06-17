package com.mediavault.engine;

import com.mediavault.model.MediaFile;

import javax.swing.*;
import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Escanea un directorio en un SwingWorker.
 *  - Nunca bloquea el EDT.
 *  - Puede cancelarse llamando a cancel().
 *  - Notifica al EDT via onDone/onProgress.
 */
public class FileScanner extends SwingWorker<List<MediaFile>, String> {

    private static final Set<String> IMAGE_EXTS = Set.of(
        ".jpg",".jpeg",".png",".gif",".webp",".bmp",".tiff",".tif");
    private static final Set<String> VIDEO_EXTS = Set.of(
        ".mp4",".avi",".mkv",".mov",".webm",".wmv",".flv",".m4v");
    public  static final Set<String> ALL_EXTS;
    static {
        Set<String> all = new HashSet<>(IMAGE_EXTS);
        all.addAll(VIDEO_EXTS);
        ALL_EXTS = Collections.unmodifiableSet(all);
    }

    private final File                     root;
    private final Set<String>              allowedExts;
    private final boolean                  recursive;
    private final Consumer<List<MediaFile>> onDone;
    private final Consumer<String>          onProgress;
    private final AtomicBoolean             cancelled = new AtomicBoolean(false);

    public FileScanner(File root,
                       String typeFilter,
                       boolean recursive,
                       Consumer<List<MediaFile>> onDone,
                       Consumer<String>          onProgress) {
        this.root        = root;
        this.recursive   = recursive;
        this.onDone      = onDone;
        this.onProgress  = onProgress;
        this.allowedExts = resolveExts(typeFilter);
    }

    private static Set<String> resolveExts(String typeFilter) {
        return switch (typeFilter) {
            case "Imágenes" -> {
                Set<String> s = new HashSet<>(IMAGE_EXTS);
                s.remove(".gif");
                yield Collections.unmodifiableSet(s);
            }
            case "GIFs"    -> Set.of(".gif");
            case "Videos"  -> VIDEO_EXTS;
            default        -> ALL_EXTS;
        };
    }

    @Override
    protected List<MediaFile> doInBackground() {
        List<MediaFile> result = new ArrayList<>();
        scan(root, result);
        return result;
    }

    private void scan(File dir, List<MediaFile> result) {
        if (cancelled.get() || isCancelled()) return;
        File[] entries = dir.listFiles();
        if (entries == null) return;
        Arrays.sort(entries, Comparator.comparing(File::getName,
                                                   String.CASE_INSENSITIVE_ORDER));
        for (File f : entries) {
            if (cancelled.get()) return;
            if (f.isDirectory() && recursive) {
                scan(f, result);
            } else if (f.isFile()) {
                String name = f.getName().toLowerCase();
                int dot = name.lastIndexOf('.');
                if (dot >= 0 && allowedExts.contains(name.substring(dot))) {
                    result.add(new MediaFile(f));
                    if (result.size() % 50 == 0)
                        publish("Encontrados: " + result.size());
                }
            }
        }
    }

    @Override
    protected void process(List<String> chunks) {
        // En EDT: actualiza progreso
        if (!chunks.isEmpty() && onProgress != null)
            onProgress.accept(chunks.getLast());
    }

    @Override
    protected void done() {
        // En EDT: entrega resultado
        if (cancelled.get() || isCancelled()) return;
        try {
            List<MediaFile> files = get();
            if (onDone != null) onDone.accept(files);
        } catch (Exception e) {
            if (onProgress != null) onProgress.accept("Error: " + e.getMessage());
        }
    }

    /** Cancela el escaneo de forma segura. */
    public void stop() {
        cancelled.set(true);
        cancel(false);
    }
}
