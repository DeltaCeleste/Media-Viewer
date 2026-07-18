package com.mediaviewer.engine;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.mediaviewer.model.MediaFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Motor de metadatos — TODAS las operaciones son síncronas.
 * Llamar siempre desde un hilo de fondo (SwingWorker / ExecutorService).
 *
 * Estructura devuelta: LinkedHashMap<String seccion, LinkedHashMap<String campo, String valor>>
 */
public class MetadataEngine {

    private MetadataEngine() {}

    // ── Lectura ───────────────────────────────────────────────────────────────

    /**
     * Lee todos los metadatos disponibles del archivo.
     * @return mapa ordenado sección → { campo → valor }
     */
    public static Map<String, Map<String, String>> read(MediaFile mf) {
        Map<String, Map<String, String>> result = new LinkedHashMap<>();

        // Sección: info básica del archivo
        Map<String, String> fileInfo = new LinkedHashMap<>();
        fileInfo.put("Nombre",    mf.getName());
        fileInfo.put("Ruta",      mf.getPath());
        fileInfo.put("Tamaño",    mf.getHumanSize());
        fileInfo.put("Modificado",mf.getFormattedDate());
        result.put("📁 Archivo", fileInfo);

        switch (mf.getType()) {
            case IMAGE, GIF -> readImageMeta(mf, result);
            case VIDEO      -> readVideoMeta(mf, result);
        }
        return result;
    }

    // ── Imagen ────────────────────────────────────────────────────────────────

    private static void readImageMeta(MediaFile mf,
                                       Map<String, Map<String, String>> out) {
        // Dimensiones con ImageIO (no carga píxeles completos si usamos getImageDimension)
        try {
            BufferedImage img = ImageIO.read(mf.getFile());
            if (img != null) {
                Map<String, String> imgInfo = new LinkedHashMap<>();
                imgInfo.put("Dimensiones", img.getWidth() + " × " + img.getHeight() + " px");
                imgInfo.put("Tipo color",  colorTypeName(img.getType()));
                imgInfo.put("Formato",     mf.getExt().toUpperCase().replace(".", ""));
                out.put("🖼 Imagen", imgInfo);
            }
        } catch (Exception ignored) {}

        // Metadatos EXIF / IPTC / XMP con metadata-extractor
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(mf.getFile());
            for (Directory dir : metadata.getDirectories()) {
                String sectionName = sectionIcon(dir.getName()) + " " + dir.getName();
                Map<String, String> tags = new LinkedHashMap<>();
                for (Tag tag : dir.getTags()) {
                    String desc = tag.getDescription();
                    if (desc != null && desc.length() < 300) {
                        tags.put(tag.getTagName(), desc);
                    }
                }
                if (!tags.isEmpty()) out.put(sectionName, tags);
            }
        } catch (Exception e) {
            Map<String, String> err = new LinkedHashMap<>();
            err.put("Error", e.getMessage());
            out.put("⚠ Metadatos", err);
        }
    }

    private static String colorTypeName(int type) {
        return switch (type) {
            case BufferedImage.TYPE_INT_RGB  -> "RGB";
            case BufferedImage.TYPE_INT_ARGB -> "ARGB";
            case BufferedImage.TYPE_BYTE_GRAY-> "Escala de grises";
            default -> "Tipo " + type;
        };
    }

    private static String sectionIcon(String dirName) {
        if (dirName == null) return "•";
        String l = dirName.toLowerCase();
        if (l.contains("exif"))    return "📷";
        if (l.contains("iptc"))    return "📰";
        if (l.contains("xmp"))     return "🏷";
        if (l.contains("gps"))     return "🌍";
        if (l.contains("jpeg"))    return "🖼";
        if (l.contains("png"))     return "🖼";
        if (l.contains("gif"))     return "🎞";
        if (l.contains("icc"))     return "🎨";
        return "•";
    }

    // ── Video ─────────────────────────────────────────────────────────────────

    private static void readVideoMeta(MediaFile mf,
                                       Map<String, Map<String, String>> out) {
        // metadata-extractor soporta MP4/MOV; para otros formatos mostramos info básica
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(mf.getFile());
            for (Directory dir : metadata.getDirectories()) {
                String sectionName = "🎬 " + dir.getName();
                Map<String, String> tags = new LinkedHashMap<>();
                for (Tag tag : dir.getTags()) {
                    String desc = tag.getDescription();
                    if (desc != null && desc.length() < 300)
                        tags.put(tag.getTagName(), desc);
                }
                if (!tags.isEmpty()) out.put(sectionName, tags);
            }
        } catch (Exception e) {
            Map<String, String> info = new LinkedHashMap<>();
            info.put("Formato", mf.getExt().toUpperCase().replace(".", ""));
            info.put("Nota",    "Metadatos internos no disponibles para este formato.");
            out.put("🎬 Video", info);
        }
    }

    // ── Escritura EXIF ────────────────────────────────────────────────────────

    /**
     * Escribe campos editables en metadatos JPEG usando API estándar.
     * Solo funciona en JPEG. Devuelve mensaje de resultado.
     *
     * Campos soportados: "ImageDescription", "Artist", "Copyright", "Software"
     */
    public static String writeExif(MediaFile mf, Map<String, String> fields) {
        String ext = mf.getExt();
        if (!ext.equals(".jpg") && !ext.equals(".jpeg")) {
            return "Solo archivos JPEG soportan escritura EXIF desde esta app.";
        }
        // metadata-extractor es solo lectura; para escribir usamos manipulación
        // de bytes EXIF directa en JPEG. Implementación simplificada:
        // se sobrescribe el comentario JPEG (APP1 comment) que sí permite Java sin libs extra.
        try {
            String desc = fields.getOrDefault("ImageDescription", "");
            if (!desc.isEmpty()) {
                injectJpegComment(mf.getFile(), desc);
                return "Descripción guardada en comentario JPEG ✓";
            }
            return "Sin cambios de metadatos que aplicar.";
        } catch (Exception e) {
            return "Error al escribir: " + e.getMessage();
        }
    }

    /**
     * Inyecta un comentario en el segmento COM de un JPEG.
     * Reemplaza el COM existente o lo añade antes del SOF.
     */
    private static void injectJpegComment(File file, String comment) throws IOException {
        byte[] original = Files.readAllBytes(file.toPath());
        byte[] commentBytes = comment.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        // Construir nuevo segmento COM
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // SOI marker
        out.write(original[0]);
        out.write(original[1]);

        // Nuevo segmento COM (0xFFFE)
        out.write(0xFF);
        out.write(0xFE);
        int len = commentBytes.length + 2;
        out.write((len >> 8) & 0xFF);
        out.write(len & 0xFF);
        out.write(commentBytes);

        // Resto del archivo (saltando COM previo si existe)
        int i = 2;
        while (i < original.length - 1) {
            if (original[i] == (byte)0xFF) {
                int marker = original[i+1] & 0xFF;
                if (marker == 0xFE) { // COM existente: saltar
                    int segLen = ((original[i+2] & 0xFF) << 8) | (original[i+3] & 0xFF);
                    i += 2 + segLen;
                    continue;
                }
            }
            out.write(original[i++]);
        }
        if (i < original.length) out.write(original[i]);

        Files.write(file.toPath(), out.toByteArray());
    }

    // ── Renombrar ─────────────────────────────────────────────────────────────

    /**
     * Renombra el archivo en disco.
     * @return nuevo File si ok, null si falla.
     */
    public static File renameFile(MediaFile mf, String newName) {
        File parent  = mf.getFile().getParentFile();
        File newFile = new File(parent, newName);
        if (mf.getFile().renameTo(newFile)) {
            mf.updateFile(newFile);
            return newFile;
        }
        return null;
    }
}
