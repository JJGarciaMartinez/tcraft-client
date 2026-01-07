#!/bin/bash

# TCraft Client - JAR Verification Script
# Verifies that the JAR file is properly built and functional

set -e

# Ensure all scripts have execute permissions
source "$(dirname "$0")/ensure-permissions.sh"

# Change to project root directory
cd "$(dirname "$0")/../.."

# ANSI Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
RESET='\033[0m'

ERRORS=0
WARNINGS=0

echo -e "${BOLD}${CYAN}================================================================${RESET}"
echo -e "${BOLD}${CYAN}  JAR Verification - TCraft Client${RESET}"
echo -e "${BOLD}${CYAN}================================================================${RESET}"
echo ""

# 1. Check JAR exists
echo -e "${YELLOW}[1/10] Checking JAR existence...${RESET}"
if [ -f "dist/TCraftClient.jar" ]; then
    SIZE=$(ls -lh dist/TCraftClient.jar | awk '{print $5}')
    echo -e "        ${GREEN}[OK]${RESET} JAR found: ${BLUE}dist/TCraftClient.jar${RESET}"
    echo -e "        ${GREEN}[OK]${RESET} Size: ${CYAN}$SIZE${RESET}"
else
    echo -e "        ${RED}[ERROR]${RESET} JAR not found"
    echo -e "        ${CYAN}[INFO]${RESET} Run: ${BLUE}./build-jar.sh${RESET}"
    ERRORS=$((ERRORS + 1))
    exit 1
fi
echo ""

# 2. Check internal structure
echo -e "${YELLOW}[2/10] Checking internal structure...${RESET}"
CLASS_COUNT=$(unzip -l dist/TCraftClient.jar 2>/dev/null | grep -c "\.class$")
echo -e "        ${GREEN}[OK]${RESET} .class files found: ${CYAN}$CLASS_COUNT${RESET}"

if [ $CLASS_COUNT -lt 10 ]; then
    echo -e "        ${YELLOW}[WARN]${RESET} Few .class files found"
    WARNINGS=$((WARNINGS + 1))
fi
echo ""

# 3. Check MANIFEST
echo -e "${YELLOW}[3/10] Checking MANIFEST.MF...${RESET}"
MANIFEST=$(unzip -p dist/TCraftClient.jar META-INF/MANIFEST.MF 2>/dev/null)
MAIN_CLASS=$(echo "$MANIFEST" | grep "Main-Class" | cut -d' ' -f2 | tr -d '\r\n ')

if [ "$MAIN_CLASS" = "Main" ]; then
    echo -e "        ${GREEN}[OK]${RESET} Main-Class configured correctly: ${CYAN}Main${RESET}"
else
    echo -e "        ${RED}[ERROR]${RESET} Main-Class incorrect or missing: ${CYAN}'$MAIN_CLASS'${RESET}"
    ERRORS=$((ERRORS + 1))
fi
echo ""

# 4. Check Main.class exists
echo -e "${YELLOW}[4/10] Checking main class...${RESET}"
if unzip -l dist/TCraftClient.jar 2>/dev/null | grep -q "Main.class"; then
    echo -e "        ${GREEN}[OK]${RESET} Main.class found in JAR"
else
    echo -e "        ${RED}[ERROR]${RESET} Main.class not found"
    ERRORS=$((ERRORS + 1))
fi
echo ""

# 5. Check version.properties (NEW - from BUILD_GUIDE.md)
echo -e "${YELLOW}[5/10] Checking version.properties...${RESET}"
if unzip -l dist/TCraftClient.jar 2>/dev/null | grep -q "version.properties"; then
    echo -e "        ${GREEN}[OK]${RESET} version.properties found in JAR"

    # Extract and display version info
    VERSION_CONTENT=$(unzip -p dist/TCraftClient.jar version.properties 2>/dev/null)
    APP_VERSION=$(echo "$VERSION_CONTENT" | grep '^app.version=' | cut -d'=' -f2)
    APP_VERSION_NUMERIC=$(echo "$VERSION_CONTENT" | grep '^app.version.numeric=' | cut -d'=' -f2)

    if [ -n "$APP_VERSION" ]; then
        echo -e "        ${CYAN}[INFO]${RESET} Display version: ${BOLD}$APP_VERSION${RESET}"
    fi

    if [ -n "$APP_VERSION_NUMERIC" ]; then
        echo -e "        ${CYAN}[INFO]${RESET} Numeric version: ${BOLD}$APP_VERSION_NUMERIC${RESET}"
    else
        echo -e "        ${YELLOW}[WARN]${RESET} Numeric version not found (needed for installer)"
        WARNINGS=$((WARNINGS + 1))
    fi
else
    echo -e "        ${RED}[ERROR]${RESET} version.properties not found in JAR"
    echo -e "        ${CYAN}[INFO]${RESET} This file is required for AppConfig.java"
    ERRORS=$((ERRORS + 1))
fi
echo ""

# 6. Check assets
echo -e "${YELLOW}[6/10] Checking assets...${RESET}"
ASSETS_COUNT=$(unzip -l dist/TCraftClient.jar 2>/dev/null | grep -c "assets/")
echo -e "        ${CYAN}[INFO]${RESET} Asset files: ${CYAN}$ASSETS_COUNT${RESET}"

# Check critical assets from BUILD_GUIDE.md
CRITICAL_ASSETS=(
    "minecraft-mojangles.ttf"
    "minecraft_title.png"
    "icon.png"
)

MISSING_ASSETS=0
for asset in "${CRITICAL_ASSETS[@]}"; do
    if unzip -l dist/TCraftClient.jar 2>/dev/null | grep -q "$asset"; then
        echo -e "        ${GREEN}[OK]${RESET} $asset found"
    else
        echo -e "        ${YELLOW}[WARN]${RESET} $asset not found"
        MISSING_ASSETS=$((MISSING_ASSETS + 1))
        WARNINGS=$((WARNINGS + 1))
    fi
done

if [ $MISSING_ASSETS -eq 0 ]; then
    echo -e "        ${GREEN}[OK]${RESET} All critical assets present"
fi
echo ""

# 7. Check dist/ directory structure (NEW - from BUILD_GUIDE.md)
echo -e "${YELLOW}[7/10] Checking dist/ directory structure...${RESET}"
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
        echo -e "        ${GREEN}[OK]${RESET} $(basename "$file") found"
    else
        echo -e "        ${YELLOW}[WARN]${RESET} $(basename "$file") not found"
        MISSING_DIST=$((MISSING_DIST + 1))
        WARNINGS=$((WARNINGS + 1))
    fi
done

if [ $MISSING_DIST -eq 0 ]; then
    echo -e "        ${GREEN}[OK]${RESET} Complete dist/ structure"
fi
echo ""

# 8. Check distribution ZIP (NEW - from BUILD_GUIDE.md)
echo -e "${YELLOW}[8/10] Checking distribution ZIP...${RESET}"
ZIP_FILES=(*.zip)
if [ -e "${ZIP_FILES[0]}" ]; then
    for zip in *.zip; do
        if [[ $zip == *"TCraft Client"* ]]; then
            echo -e "        ${GREEN}[OK]${RESET} Distribution ZIP found: ${BLUE}$zip${RESET}"
            ZIP_SIZE=$(ls -lh "$zip" | awk '{print $5}')
            echo -e "        ${CYAN}[INFO]${RESET} ZIP size: ${CYAN}$ZIP_SIZE${RESET}"
        fi
    done
else
    echo -e "        ${YELLOW}[WARN]${RESET} No distribution ZIP found"
    echo -e "        ${CYAN}[INFO]${RESET} build-jar.sh should create it automatically"
    WARNINGS=$((WARNINGS + 1))
fi
echo ""

# 9. Test execution
echo -e "${YELLOW}[9/10] Execution test...${RESET}"
echo -e "        Starting application..."
java -jar dist/TCraftClient.jar > /tmp/tcraft-jar-test.log 2>&1 &
JAR_PID=$!
sleep 2

if ps -p $JAR_PID > /dev/null 2>&1; then
    echo -e "        ${GREEN}[OK]${RESET} JAR running correctly (PID: ${CYAN}$JAR_PID${RESET})"
    kill $JAR_PID 2>/dev/null
    wait $JAR_PID 2>/dev/null
    echo -e "        ${GREEN}[OK]${RESET} Process terminated cleanly"
else
    echo -e "        ${RED}[ERROR]${RESET} JAR failed to start or terminated immediately"
    if [ -f /tmp/tcraft-jar-test.log ]; then
        echo "        Error logs:"
        cat /tmp/tcraft-jar-test.log | sed 's/^/          /'
    fi
    ERRORS=$((ERRORS + 1))
fi
echo ""

# 10. Check execution logs
echo -e "${YELLOW}[10/10] Checking execution logs...${RESET}"
if [ -f /tmp/tcraft-jar-test.log ] && [ -s /tmp/tcraft-jar-test.log ]; then
    if grep -qi "error\|exception" /tmp/tcraft-jar-test.log; then
        echo -e "         ${YELLOW}[WARN]${RESET} Errors/exceptions found in logs:"
        grep -i "error\|exception" /tmp/tcraft-jar-test.log | head -5 | sed 's/^/           /'
        WARNINGS=$((WARNINGS + 1))
    else
        echo -e "         ${GREEN}[OK]${RESET} No critical errors in logs"
    fi
else
    echo -e "         ${GREEN}[OK]${RESET} No errors in execution (empty logs)"
fi
echo ""

# Final summary
echo -e "${BOLD}${CYAN}================================================================${RESET}"
if [ $ERRORS -eq 0 ]; then
    echo -e "${BOLD}${GREEN}  Verification Completed Successfully${RESET}"
    echo -e "${BOLD}${CYAN}================================================================${RESET}"
    echo ""
    if [ $WARNINGS -gt 0 ]; then
        echo -e "${YELLOW}[WARN]${RESET} $WARNINGS warning(s) found (non-critical)"
        echo ""
    fi
    echo -e "${BOLD}The JAR is ready to:${RESET}"
    echo -e "  • Execute:          ${BLUE}java -jar dist/TCraftClient.jar${RESET}"
    echo -e "  • Test launchers:   ${BLUE}./dist/TCraft Client.(command|bat|sh)${RESET}"
    echo -e "  • Distribute ZIP:   Look for ${BLUE}TCraft Client-*.zip${RESET}"
    echo -e "  • Create installer: ${BLUE}./build-installer.sh${RESET}"
    echo ""
    exit 0
else
    echo -e "${BOLD}${RED}  Verification Failed${RESET}"
    echo -e "${BOLD}${CYAN}================================================================${RESET}"
    echo ""
    echo -e "${RED}[ERROR]${RESET} $ERRORS error(s) found"
    if [ $WARNINGS -gt 0 ]; then
        echo -e "${YELLOW}[WARN]${RESET} $WARNINGS warning(s) found"
    fi
    echo ""
    echo "Please review the messages above."
    echo -e "Run ${BLUE}./scripts/bash/build-jar.sh${RESET} to rebuild the JAR."
    echo ""
    exit 1
fi


