@echo off
REM ============================================================
REM  MediaViewer — Script de compilación para Windows
REM ============================================================
echo.
echo  [MediaViewer] Compilando...
echo.

REM Verificar que Maven esté en el PATH
where mvn >nul 2>&1
if %errorlevel% neq 0 (
    echo  ERROR: Maven no encontrado en el PATH.
    echo  Instala Maven desde https://maven.apache.org/download.cgi
    echo  y añade su carpeta bin al PATH del sistema.
    pause
    exit /b 1
)

REM Verificar que Java 17+ esté disponible
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo  ERROR: Java no encontrado. Instala JDK 17 desde https://adoptium.net
    pause
    exit /b 1
)

REM Compilar y empaquetar (fat JAR con todas las dependencias)
call mvn clean package
echo Maven termino con codigo: %errorlevel%
set MVN_ERROR=%errorlevel%

echo.
echo  ========================================
echo  Resultado de Maven: %MVN_ERROR%
echo  ========================================
echo.

if %MVN_ERROR% neq 0 (
    echo  [ERROR] Maven fallo con codigo: %MVN_ERROR%
    echo.
    echo  ERROR durante la compilacion. Revisa los mensajes de Maven arriba.
    pause
    exit /b 1
)

echo.
echo  Compilacion exitosa!
echo  JAR generado en: target\MediaViewer-2.0.jar
echo.
echo  Ejecutando la aplicacion...
java -jar target\MediaViewer-2.0.jar

pause
