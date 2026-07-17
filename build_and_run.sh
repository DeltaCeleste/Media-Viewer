#!/bin/bash
# ============================================================
#  MediaVault — Script de compilación para Linux / macOS
# ============================================================
set -e

echo ""
echo " [MediaVault] Compilando..."
echo ""

# Verificar Maven
if ! command -v mvn &> /dev/null; then
    echo " ERROR: Maven no encontrado."
    echo " Linux:  sudo apt install maven  /  sudo dnf install maven"
    echo " macOS:  brew install maven"
    exit 1
fi

# Verificar Java 17+
if ! command -v java &> /dev/null; then
    echo " ERROR: Java no encontrado."
    echo " Linux:  sudo apt install openjdk-17-jdk"
    echo " macOS:  brew install openjdk@17"
    exit 1
fi

JAVA_VER=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d. -f1)
if [ "$JAVA_VER" -lt 17 ] 2>/dev/null; then
    echo " AVISO: Se recomienda Java 17 o superior (detectado: $JAVA_VER)"
fi

# Compilar
mvn clean package -q

echo ""
echo " Compilación exitosa!"
echo " JAR: target/MediaVault-2.0.jar"
echo ""
echo " Ejecutando..."
java --enable-native-access=ALL-UNNAMED -jar target/MediaVault-2.0.jar
