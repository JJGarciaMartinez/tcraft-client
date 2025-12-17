#!/bin/bash
# Script para copiar assets al directorio de salida de IntelliJ
# Ejecutar después de compilar o usar como pre-build task

OUTPUT_DIR="out/production/tcraft-client"

echo "Copiando assets al directorio de salida..."

# Crear directorio de assets si no existe
mkdir -p "$OUTPUT_DIR/assets"

# Copiar todos los assets
cp -r assets/* "$OUTPUT_DIR/assets/"

echo "✓ Assets copiados correctamente a $OUTPUT_DIR/assets/"

