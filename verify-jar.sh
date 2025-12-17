#!/bin/bash

# TCraft Client - JAR Verification Script
# Verifies that the JAR file is properly built and functional

echo "=========================================="
echo "  VERIFICACIÓN DEL JAR - TCraft Client"
echo "=========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

ERRORS=0

# 1. Check JAR exists
echo "1. Verificando existencia del JAR..."
if [ -f "dist/TCraftClient.jar" ]; then
    SIZE=$(ls -lh dist/TCraftClient.jar | awk '{print $5}')
    echo -e "   ${GREEN}✓${NC} JAR encontrado: dist/TCraftClient.jar"
    echo -e "   ${GREEN}✓${NC} Tamaño: $SIZE"
else
    echo -e "   ${RED}✗${NC} JAR no encontrado"
    ERRORS=$((ERRORS + 1))
    exit 1
fi
echo ""

# 2. Check internal structure
echo "2. Verificando estructura interna..."
CLASS_COUNT=$(unzip -l dist/TCraftClient.jar 2>/dev/null | grep -c "\.class$")
echo -e "   ${GREEN}✓${NC} Archivos .class encontrados: $CLASS_COUNT"

if [ $CLASS_COUNT -lt 10 ]; then
    echo -e "   ${YELLOW}⚠${NC}  Advertencia: Pocos archivos .class encontrados"
    ERRORS=$((ERRORS + 1))
fi
echo ""

# 3. Check MANIFEST
echo "3. Verificando MANIFEST.MF..."
MANIFEST=$(unzip -p dist/TCraftClient.jar META-INF/MANIFEST.MF 2>/dev/null)
MAIN_CLASS=$(echo "$MANIFEST" | grep "Main-Class" | cut -d' ' -f2 | tr -d '\r\n ')

if [ "$MAIN_CLASS" = "Main" ]; then
    echo -e "   ${GREEN}✓${NC} Main-Class configurado correctamente: Main"
else
    echo -e "   ${RED}✗${NC} Main-Class incorrecto o faltante: '$MAIN_CLASS'"
    ERRORS=$((ERRORS + 1))
fi
echo ""

# 4. Check Main.class exists
echo "4. Verificando clase principal..."
if unzip -l dist/TCraftClient.jar 2>/dev/null | grep -q "Main.class"; then
    echo -e "   ${GREEN}✓${NC} Main.class encontrado en el JAR"
else
    echo -e "   ${RED}✗${NC} Main.class no encontrado"
    ERRORS=$((ERRORS + 1))
fi
echo ""

# 5. Check assets
echo "5. Verificando assets..."
ASSETS_COUNT=$(unzip -l dist/TCraftClient.jar 2>/dev/null | grep -c "assets/")
echo -e "   ${GREEN}✓${NC} Archivos de assets: $ASSETS_COUNT"

if unzip -l dist/TCraftClient.jar 2>/dev/null | grep -q "minecraft-mojangles.ttf"; then
    echo -e "   ${GREEN}✓${NC} Fuente Minecraft encontrada"
else
    echo -e "   ${YELLOW}⚠${NC}  Fuente Minecraft no encontrada"
fi

if unzip -l dist/TCraftClient.jar 2>/dev/null | grep -q "minecraft_title.png"; then
    echo -e "   ${GREEN}✓${NC} Logo Minecraft encontrado"
else
    echo -e "   ${YELLOW}⚠${NC}  Logo Minecraft no encontrado"
fi
echo ""

# 6. Test execution
echo "6. Prueba de ejecución..."
echo "   Iniciando aplicación..."
java -jar dist/TCraftClient.jar > /tmp/tcraft-jar-test.log 2>&1 &
JAR_PID=$!
sleep 2

if ps -p $JAR_PID > /dev/null 2>&1; then
    echo -e "   ${GREEN}✓${NC} JAR ejecutándose correctamente (PID: $JAR_PID)"
    kill $JAR_PID 2>/dev/null
    wait $JAR_PID 2>/dev/null
    echo -e "   ${GREEN}✓${NC} Proceso terminado limpiamente"
else
    echo -e "   ${RED}✗${NC} JAR no pudo iniciarse o terminó inmediatamente"
    if [ -f /tmp/tcraft-jar-test.log ]; then
        echo "   Logs de error:"
        cat /tmp/tcraft-jar-test.log
    fi
    ERRORS=$((ERRORS + 1))
fi
echo ""

# 7. Check execution logs
echo "7. Verificando logs de ejecución..."
if [ -f /tmp/tcraft-jar-test.log ] && [ -s /tmp/tcraft-jar-test.log ]; then
    if grep -qi "error\|exception" /tmp/tcraft-jar-test.log; then
        echo -e "   ${YELLOW}⚠${NC}  Se encontraron errores/excepciones en los logs:"
        grep -i "error\|exception" /tmp/tcraft-jar-test.log | head -5
    else
        echo -e "   ${GREEN}✓${NC} Sin errores críticos en los logs"
    fi
else
    echo -e "   ${GREEN}✓${NC} Sin errores en la ejecución (logs vacíos)"
fi
echo ""

# Final summary
echo "=========================================="
if [ $ERRORS -eq 0 ]; then
    echo -e "${GREEN}✓ VERIFICACIÓN COMPLETA EXITOSA${NC}"
    echo "=========================================="
    echo ""
    echo "El JAR está listo para:"
    echo "  • Ejecutar:         java -jar dist/TCraftClient.jar"
    echo "  • Distribuir:       dist/TCraftClient.jar"
    echo "  • Crear instalador: ./build-installer.sh"
    echo ""
    exit 0
else
    echo -e "${RED}✗ VERIFICACIÓN FALLIDA${NC}"
    echo "=========================================="
    echo ""
    echo "Se encontraron $ERRORS errores."
    echo "Por favor, revisa los mensajes anteriores."
    echo ""
    exit 1
fi

