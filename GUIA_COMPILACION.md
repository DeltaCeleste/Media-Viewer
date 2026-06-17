# MediaVault v2 — Guía de instalación y compilación

## Resumen rápido

```
JDK 17+  +  Maven 3.8+  →  mvn clean package  →  java -jar target/MediaVault-2.0.jar
```

---

## 1. Dependencias del sistema

### Java Development Kit (JDK) 17 o superior

MediaVault usa records, switch expressions y `var` — características de Java 17.

| Sistema | Fuente recomendada | Comando |
|---|---|---|
| **Windows** | [Adoptium Temurin 21](https://adoptium.net) | Instalador .msi |
| **Ubuntu/Debian** | OpenJDK oficial | `sudo apt install openjdk-21-jdk` |
| **Fedora/RHEL** | OpenJDK oficial | `sudo dnf install java-21-openjdk-devel` |
| **macOS** | Homebrew | `brew install openjdk@21` |

Verifica la instalación:
```bash
java -version
# Debe mostrar: openjdk version "21..." o similar
javac -version
# Debe mostrar: javac 21...
```

---

### Apache Maven 3.8+

Maven descarga automáticamente todas las dependencias Java desde Maven Central.

| Sistema | Comando |
|---|---|
| **Windows** | [Descarga ZIP](https://maven.apache.org/download.cgi) → descomprime → añade `bin/` al PATH |
| **Ubuntu/Debian** | `sudo apt install maven` |
| **Fedora/RHEL** | `sudo dnf install maven` |
| **macOS** | `brew install maven` |

Verifica:
```bash
mvn -version
# Debe mostrar: Apache Maven 3.x.x
```

**En Windows:** añade las variables de entorno:
- `JAVA_HOME` → ruta al JDK (p. ej. `C:\Program Files\Eclipse Adoptium\jdk-21.0.3.9-hotspot`)
- `PATH` → añade `%JAVA_HOME%\bin` y `C:\ruta\a\maven\bin`

---

## 2. Dependencias Java (Maven las descarga automáticamente)

Declaradas en `pom.xml` — **no hace falta descargar nada manualmente**:

| Librería | Versión | Para qué sirve |
|---|---|---|
| `metadata-extractor` | 2.19.0 | Lee EXIF, IPTC, XMP, GPS de imágenes y vídeos MP4/MOV |
| `animated-gif-lib` | 1.4 | Soporte GIF animado (Swing no lo maneja bien por defecto) |
| `thumbnailator` | 0.4.20 | Miniaturas de alta calidad de forma rápida y sencilla |

Maven los descarga en `~/.m2/repository/` la primera vez; las siguientes compilaciones usan la caché local.

---

## 3. Compilar

### Windows

```cmd
cd MediaVaultJava
build_and_run.bat
```

O manualmente:

```cmd
cd MediaVaultJava
mvn clean package
java -jar target\MediaVault-2.0.jar
```

### Linux / macOS

```bash
cd MediaVaultJava
chmod +x build_and_run.sh
./build_and_run.sh
```

O manualmente:

```bash
cd MediaVaultJava
mvn clean package
java -jar target/MediaVault-2.0.jar
```

### Solo compilar sin ejecutar

```bash
mvn clean package -DskipTests
# El JAR queda en target/MediaVault-2.0.jar
```

---

## 4. Estructura del proyecto

```
MediaVaultJava/
├── pom.xml                          ← Dependencias y configuración Maven
├── build_and_run.bat                ← Script Windows
├── build_and_run.sh                 ← Script Linux/macOS
└── src/main/java/com/mediavault/
    ├── Main.java                    ← Punto de entrada
    ├── model/
    │   ├── MediaFile.java           ← Modelo de archivo multimedia
    │   └── FilterOptions.java       ← Estado de filtros (record)
    ├── engine/
    │   ├── FileScanner.java         ← Escaneo de directorio (SwingWorker)
    │   └── MetadataEngine.java      ← Lectura/escritura de metadatos
    ├── ui/
    │   └── MainWindow.java          ← Ventana principal y orquestación
    ├── ui/panels/
    │   ├── ViewerPanel.java         ← Visor de imagen/GIF/vídeo con zoom
    │   ├── ThumbnailStrip.java      ← Tira horizontal de miniaturas
    │   ├── FileListPanel.java       ← Lista lateral de archivos
    │   ├── MetadataPanel.java       ← Panel de metadatos y edición
    │   └── FilterBar.java           ← Barra de búsqueda y filtros
    └── util/
        └── Theme.java               ← Paleta de colores y fuentes
```

---

## 5. Funciones de la aplicación

| Función | Detalle |
|---|---|
| **Abrir carpeta** | Ctrl+O o botón. Recuerda la última carpeta entre sesiones. |
| **Navegación** | ←/→ entre imágenes, Inicio/Fin para primero/último |
| **Zoom/Pan** | Rueda del ratón para zoom. Botón derecho/central + arrastrar para mover |
| **GIF animados** | Se reproducen automáticamente en el visor |
| **Vídeos** | Muestra información y botón para abrir con el reproductor del sistema |
| **Filtro por tipo** | Todo / Imágenes / GIFs / Vídeos |
| **Búsqueda** | Filtro por nombre en tiempo real |
| **Ordenación** | Nombre, fecha, tamaño (ascendente/descendente) |
| **Subcarpetas** | Checkbox para escaneo recursivo |
| **Metadatos** | EXIF, IPTC, XMP, GPS, dimensiones — árbol expandible. Doble clic para copiar |
| **Editar metadatos** | Descripción, Artista, Copyright, Software (JPEG) |
| **Renombrar** | Campo nombre en el panel de metadatos + botón guardar |
| **Eliminar** | Tecla Delete o botón. Pide confirmación |
| **Miniaturas** | Tira horizontal cargada en paralelo (4 hilos) |

---

## 6. Requisitos mínimos de hardware

- RAM: 256 MB libres para imágenes normales; 512 MB+ para carpetas de miles de archivos
- Disco: ~15 MB para el JAR con dependencias
- Pantalla: mínimo 960×660 px

---

## 7. Problemas comunes

### "java: command not found" / "'java' is not recognized"
→ JDK no instalado o no en el PATH. Revisa la sección 1.

### "JAVA_HOME not set" durante Maven
→ En Windows, crea la variable de entorno `JAVA_HOME` apuntando a tu JDK.

### La ventana aparece en blanco / sin colores
→ Algunos sistemas Linux requieren `export _JAVA_OPTIONS="-Dawt.useSystemAAFontSettings=on"` antes de ejecutar.

### "Could not resolve dependencies" durante mvn
→ Sin acceso a Internet. Maven necesita conexión la primera vez para descargar las dependencias a `~/.m2/`.

### GIFs no se animan
→ Asegúrate de que el archivo es un GIF animado válido. Los GIFs estáticos se mostrarán como imagen normal.

### Metadatos EXIF no se guardan
→ La escritura EXIF solo está disponible para archivos **JPEG**. PNG y otros formatos no soportan escritura directa de EXIF desde esta app.

---

## 8. Compilar en IDE (IntelliJ IDEA / Eclipse)

### IntelliJ IDEA
1. File → Open → selecciona la carpeta `MediaVaultJava`
2. IntelliJ detecta el `pom.xml` automáticamente → "Trust Project"
3. Espera a que descargue dependencias (barra de progreso inferior)
4. Run → `Main` (clic derecho sobre `Main.java` → Run)

### Eclipse
1. File → Import → Maven → Existing Maven Projects
2. Selecciona la carpeta `MediaVaultJava`
3. Finish → espera descarga
4. Clic derecho en `Main.java` → Run As → Java Application

### VS Code
1. Instala extensión "Extension Pack for Java"
2. Abre la carpeta `MediaVaultJava`
3. Clic en el botón ▶ que aparece sobre el método `main()`
