#!/bin/bash

# TCraft Client - JAR Verification Script
# Verifies that the JAR file is properly built and functional

echo "=========================================="
echo "  JAR VERIFICATION - TCraft Client"
echo "=========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

ERRORS=0
WARNINGS=0

# 1. Check JAR exists
echo "1. Checking JAR existence..."
if [ -f "dist/TCraftClient.jar" ]; then
    SIZE=$(ls -lh dist/TCraftClient.jar | awk '{print $5}')
    echo -e "   ${GREEN}✓${NC} JAR found: dist/TCraftClient.jar"
    echo -e "   ${GREEN}✓${NC} Size: $SIZE"
else
    echo -e "   ${RED}✗${NC} JAR not found"
    echo -e "   ${YELLOW}ℹ${NC}  Run: ./build-jar.sh"
    ERRORS=$((ERRORS + 1))
    exit 1
fi
echo ""

# 2. Check internal structure
echo "2. Checking internal structure..."
CLASS_COUNT=$(unzip -l dist/TCraftClient.jar 2>/dev/null | grep -c "\.class$")
echo -e "   ${GREEN}✓${NC} .class files found: $CLASS_COUNT"

if [ $CLASS_COUNT -lt 10 ]; then
    echo -e "   ${YELLOW}⚠${NC}  Warning: Few .class files found"
    WARNINGS=$((WARNINGS + 1))
fi
echo ""

# 3. Check MANIFEST
echo "3. Checking MANIFEST.MF..."
MANIFEST=$(unzip -p dist/TCraftClient.jar META-INF/MANIFEST.MF 2>/dev/null)
MAIN_CLASS=$(echo "$MANIFEST" | grep "Main-Class" | cut -d' ' -f2 | tr -d '\r\n ')

if [ "$MAIN_CLASS" = "Main" ]; then
    echo -e "   ${GREEN}✓${NC} Main-Class configured correctly: Main"
else
    echo -e "   ${RED}✗${NC} Main-Class incorrect or missing: '$MAIN_CLASS'"
    ERRORS=$((ERRORS + 1))
fi
echo ""

# 4. Check Main.class exists
echo "4. Checking main class..."
if unzip -l dist/TCraftClient.jar 2>/dev/null | grep -q "Main.class"; then
    echo -e "   ${GREEN}✓${NC} Main.class found in JAR"
else
    echo -e "   ${RED}✗${NC} Main.class not found"
    ERRORS=$((ERRORS + 1))
fi
echo ""

# 5. Check version.properties (NEW - from BUILD_GUIDE.md)
echo "5. Checking version.properties..."
if unzip -l dist/TCraftClient.jar 2>/dev/null | grep -q "version.properties"; then
    echo -e "   ${GREEN}✓${NC} version.properties found in JAR"

    # Extract and display version info
    VERSION_CONTENT=$(unzip -p dist/TCraftClient.jar version.properties 2>/dev/null)
    APP_VERSION=$(echo "$VERSION_CONTENT" | grep '^app.version=' | cut -d'=' -f2)
    APP_VERSION_NUMERIC=$(echo "$VERSION_CONTENT" | grep '^app.version.numeric=' | cut -d'=' -f2)

    if [ -n "$APP_VERSION" ]; then
        echo -e "   ${BLUE}ℹ${NC}  Display version: $APP_VERSION"
    fi

    if [ -n "$APP_VERSION_NUMERIC" ]; then
        echo -e "   ${BLUE}ℹ${NC}  Numeric version: $APP_VERSION_NUMERIC"
    else
        echo -e "   ${YELLOW}⚠${NC}  Numeric version not found (needed for installer)"
        WARNINGS=$((WARNINGS + 1))
    fi
else
    echo -e "   ${RED}✗${NC} version.properties not found in JAR"
    echo -e "   ${YELLOW}ℹ${NC}  This file is required for AppConfig.java"
    ERRORS=$((ERRORS + 1))
fi
echo ""

# 6. Check assets
echo "6. Checking assets..."
ASSETS_COUNT=$(unzip -l dist/TCraftClient.jar 2>/dev/null | grep -c "assets/")
echo -e "   ${BLUE}ℹ${NC}  Asset files: $ASSETS_COUNT"

# Check critical assets from BUILD_GUIDE.md
CRITICAL_ASSETS=(
    "minecraft-mojangles.ttf"
    "minecraft_title.png"
    "icon.png"
)

MISSING_ASSETS=0
for asset in "${CRITICAL_ASSETS[@]}"; do
    if unzip -l dist/TCraftClient.jar 2>/dev/null | grep -q "$asset"; then
        echo -e "   ${GREEN}✓${NC} $asset found"
    else
        echo -e "   ${YELLOW}⚠${NC}  $asset not found"
        MISSING_ASSETS=$((MISSING_ASSETS + 1))
        WARNINGS=$((WARNINGS + 1))
    fi
done

if [ $MISSING_ASSETS -eq 0 ]; then
    echo -e "   ${GREEN}✓${NC} All critical assets present"
fi
echo ""

# 7. Check dist/ directory structure (NEW - from BUILD_GUIDE.md)
echo "7. Checking dist/ directory structure..."
DIST_FILES=(
    "dist/TCraftClient.jar"
    "dist/TCraft Client.command"
    "dist/TCraft Client.bat"
    "dist/TCraft Client.sh"
    "dist/README.txt"
)

MISSING_DIST=0
for file in "${DIST_FILES[@]}"; do
    if [ -f "$file" ]; then
        echo -e "   ${GREEN}✓${NC} $(basename "$file") found"
    else
        echo -e "   ${YELLOW}⚠${NC}  $(basename "$file") not found"
        MISSING_DIST=$((MISSING_DIST + 1))
        WARNINGS=$((WARNINGS + 1))
    fi
done

if [ $MISSING_DIST -eq 0 ]; then
    echo -e "   ${GREEN}✓${NC} Complete dist/ structure"
fi
echo ""

# 8. Check distribution ZIP (NEW - from BUILD_GUIDE.md)
echo "8. Checking distribution ZIP..."
ZIP_FILES=(*.zip)
if [ -e "${ZIP_FILES[0]}" ]; then
    for zip in *.zip; do
        if [[ $zip == *"TCraft Client"* ]]; then
            echo -e "   ${GREEN}✓${NC} Distribution ZIP found: $zip"
            ZIP_SIZE=$(ls -lh "$zip" | awk '{print $5}')
            echo -e "   ${BLUE}ℹ${NC}  ZIP size: $ZIP_SIZE"
        fi
    done
else
    echo -e "   ${YELLOW}⚠${NC}  No distribution ZIP found"
    echo -e "   ${YELLOW}ℹ${NC}  build-jar.sh should create it automatically"
    WARNINGS=$((WARNINGS + 1))
fi
echo ""

# 9. Test execution
echo "9. Execution test..."
echo "   Starting application..."
java -jar dist/TCraftClient.jar > /tmp/tcraft-jar-test.log 2>&1 &
JAR_PID=$!
sleep 2

if ps -p $JAR_PID > /dev/null 2>&1; then
    echo -e "   ${GREEN}✓${NC} JAR running correctly (PID: $JAR_PID)"
    kill $JAR_PID 2>/dev/null
    wait $JAR_PID 2>/dev/null
    echo -e "   ${GREEN}✓${NC} Process terminated cleanly"
else
    echo -e "   ${RED}✗${NC} JAR failed to start or terminated immediately"
    if [ -f /tmp/tcraft-jar-test.log ]; then
        echo "   Error logs:"
        cat /tmp/tcraft-jar-test.log
    fi
    ERRORS=$((ERRORS + 1))
fi
echo ""

# 10. Check execution logs
echo "10. Checking execution logs..."
if [ -f /tmp/tcraft-jar-test.log ] && [ -s /tmp/tcraft-jar-test.log ]; then
    if grep -qi "error\|exception" /tmp/tcraft-jar-test.log; then
        echo -e "   ${YELLOW}⚠${NC}  Errors/exceptions found in logs:"
        grep -i "error\|exception" /tmp/tcraft-jar-test.log | head -5
        WARNINGS=$((WARNINGS + 1))
    else
        echo -e "   ${GREEN}✓${NC} No critical errors in logs"
    fi
else
    echo -e "   ${GREEN}✓${NC} No errors in execution (empty logs)"
fi
echo ""

# Final summary
echo "=========================================="
if [ $ERRORS -eq 0 ]; then
    echo -e "${GREEN}✓ VERIFICATION COMPLETED SUCCESSFULLY${NC}"
    echo "=========================================="
    echo ""
    if [ $WARNINGS -gt 0 ]; then
        echo -e "${YELLOW}ℹ${NC}  $WARNINGS warning(s) found (non-critical)"
        echo ""
    fi
    echo "The JAR is ready to:"
    echo "  • Execute:          java -jar dist/TCraftClient.jar"
    echo "  • Test launchers:   ./dist/TCraft Client.(command|bat|sh)"
    echo "  • Distribute ZIP:   Look for TCraft Client-*.zip"
    echo "  • Create installer: ./build-installer.sh"
    echo ""
    exit 0
else
    echo -e "${RED}✗ VERIFICATION FAILED${NC}"
    echo "=========================================="
    echo ""
    echo "$ERRORS error(s) found."
    if [ $WARNINGS -gt 0 ]; then
        echo "$WARNINGS warning(s) found."
    fi
    echo ""
    echo "Please review the messages above."
    echo "Run ./build-jar.sh to rebuild the JAR."
    echo ""
    exit 1
fi

