package com.mediavault.model;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Representa un archivo multimedia escaneado del disco.
 * Inmutable salvo por el nombre (puede renombrarse).
 */
public class MediaFile {

    public enum MediaType { IMAGE, GIF, VIDEO }

    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd  HH:mm:ss");

    private File   file;
    private final MediaType type;
    private final String    ext;          // en minúsculas, con punto: ".jpg"

    public MediaFile(File file) {
        this.file = file;
        String name = file.getName().toLowerCase();
        int dot = name.lastIndexOf('.');
        this.ext = dot >= 0 ? name.substring(dot) : "";
        this.type = resolveType(this.ext);
    }

    private static MediaType resolveType(String ext) {
        return switch (ext) {
            case ".gif"                                  -> MediaType.GIF;
            case ".mp4", ".avi", ".mkv", ".mov",
                 ".webm", ".wmv", ".flv", ".m4v"        -> MediaType.VIDEO;
            default                                      -> MediaType.IMAGE;
        };
    }

    // ── Accesores ─────────────────────────────────────────────────────────────

    public File      getFile()     { return file; }
    public String    getPath()     { return file.getAbsolutePath(); }
    public String    getName()     { return file.getName(); }
    public String    getExt()      { return ext; }
    public MediaType getType()     { return type; }
    public long      getSize()     { return file.length(); }
    public long      getLastModified() { return file.lastModified(); }

    /** Devuelve tamaño legible: "1.2 MB". */
    public String getHumanSize() {
        long n = file.length();
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int i = 0;
        double d = n;
        while (d >= 1024 && i < units.length - 1) { d /= 1024; i++; }
        return String.format("%.1f %s", d, units[i]);
    }

    /** Devuelve fecha de modificación formateada. */
    public String getFormattedDate() {
        LocalDateTime dt = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(file.lastModified()), ZoneId.systemDefault());
        return dt.format(FMT);
    }

    /** Actualiza la referencia al archivo (tras renombrar). */
    public void updateFile(File newFile) {
        this.file = newFile;
    }

    @Override public String toString() { return file.getName(); }
}
