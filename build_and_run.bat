@echo off
REM ============================================================
REM  MediaVault — Script de compilación para Windows
REM ============================================================
echo.
echo  [MediaVault] Compilando...
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
mvn clean package -q

if %errorlevel% neq 0 (
    echo.
    echo  ERROR durante la compilacion. Revisa los mensajes de Maven arriba.
    pause
    exit /b 1
)

echo.
echo  Compilacion exitosa!
echo  JAR generado en: target\MediaVault-2.0.jar
echo.
echo  Ejecutando la aplicacion...
java -jar target\MediaVault-2.0.jar

pause
