#!/bin/bash

# TCraft Client - JAR Build Script
# Creates an executable JAR file from compiled classes

set -e

# ANSI Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
RESET='\033[0m'

# Load version from version.properties
if [ ! -f "version.properties" ]; then
    echo -e "${RED}[ERROR]${RESET} version.properties not found"
    exit 1
fi

# Read properties from file
APP_VERSION=$(grep '^app.version=' version.properties | cut -d'=' -f2)
APP_NAME=$(grep '^app.name=' version.properties | cut -d'=' -f2)

# Validate that version was loaded
if [ -z "$APP_VERSION" ] || [ -z "$APP_NAME" ]; then
    echo -e "${RED}[ERROR]${RESET} Could not read app.version or app.name from version.properties"
    exit 1
fi

echo -e "${BOLD}${CYAN}================================================================${RESET}"
echo -e "${BOLD}${CYAN}  Building ${APP_NAME} v${APP_VERSION}${RESET}"
echo -e "${BOLD}${CYAN}================================================================${RESET}"
echo ""

# Compile sources first
echo -e "${YELLOW}[1/5] Compiling sources...${RESET}"
./compile-sources.sh
if [ $? -ne 0 ]; then
    echo -e "${RED}[ERROR]${RESET} Compilation failed, cannot build JAR"
    exit 1
fi
echo ""

# Create dist directory if it doesn't exist
mkdir -p dist

# Clean old assets from production output
echo -e "${YELLOW}[2/5] Cleaning old artifacts...${RESET}"
rm -rf out/production/tcraft-client/assets
rm -f out/production/tcraft-client/version.properties
rm -f out/production/tcraft-client/*.png out/production/tcraft-client/*.icns out/production/tcraft-client/*.ico
rm -rf out/production/tcraft-client/font
echo -e "${GREEN}[OK]${RESET} Cleaned stale resources"
echo ""

# Copy assets to production output
echo -e "${YELLOW}[3/5] Packaging resources...${RESET}"
cp -r assets out/production/tcraft-client/
echo -e "${GREEN}[OK]${RESET} Assets copied"

# Copy version.properties to production output
cp version.properties out/production/tcraft-client/
echo -e "${GREEN}[OK]${RESET} version.properties copied"
echo ""

# Create JAR file
echo -e "${YELLOW}[4/5] Creating JAR archive...${RESET}"
jar cvfm dist/TCraftClient.jar manifest.txt -C out/production/tcraft-client . > /dev/null 2>&1

# Verify JAR was created
if [ -f "dist/TCraftClient.jar" ]; then
    JAR_SIZE=$(ls -lh dist/TCraftClient.jar | awk '{print $5}')
    echo -e "${GREEN}[OK]${RESET} JAR created: ${BLUE}dist/TCraftClient.jar${RESET} (${CYAN}${JAR_SIZE}${RESET})"
else
    echo -e "${RED}[ERROR]${RESET} Failed to create JAR"
    exit 1
fi
echo ""

# Create platform-specific launchers
echo -e "${YELLOW}[5/5] Creating platform launchers...${RESET}"

# macOS launcher (.command)
cat > "dist/TCraft Client.command" << 'EOF'
#!/bin/bash
# TCraft Client Launcher for macOS
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$DIR"
java -jar TCraftClient.jar
if [ $? -ne 0 ]; then
    echo ""
    echo "Press any key to exit..."
    read -n 1
fi
EOF
chmod +x "dist/TCraft Client.command"

# Windows launcher (.bat)
cat > "dist/TCraft Client.bat" << 'EOF'
@echo off
REM TCraft Client Launcher for Windows
cd /d "%~dp0"
java -jar TCraftClient.jar
if %errorlevel% neq 0 (
    echo.
    echo Press any key to exit...
    pause >nul
)
EOF

# Linux launcher (.sh)
cat > "dist/TCraft Client.sh" << 'EOF'
#!/bin/bash
# TCraft Client Launcher for Linux
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$DIR"
java -jar TCraftClient.jar
if [ $? -ne 0 ]; then
    echo ""
    echo "Press any key to exit..."
    read -n 1
fi
EOF
chmod +x "dist/TCraft Client.sh"

# Create README
cat > "dist/README.txt" << 'EOF'
# TCraft Client - How to Run

## Quick Start

macOS:    Double-click "TCraft Client.command"
Windows:  Double-click "TCraft Client.bat"
Linux:    Run "./TCraft Client.sh" in terminal
Any OS:   Run "java -jar TCraftClient.jar" in terminal

## Requirements
- Java 16 or higher

Check version: java -version
Download Java: https://adoptium.net/
EOF

echo -e "${GREEN}[OK]${RESET} Platform launchers created"
echo -e "      ${BLUE}TCraft Client.command${RESET} (macOS)"
echo -e "      ${BLUE}TCraft Client.bat${RESET} (Windows)"
echo -e "      ${BLUE}TCraft Client.sh${RESET} (Linux)"
echo -e "      ${BLUE}README.txt${RESET} (instructions)"
echo ""

# Create ZIP archive
ZIP_NAME="${APP_NAME}-${APP_VERSION}.zip"
echo -e "${BOLD}Creating distribution package...${RESET}"

# Remove old ZIP if exists
rm -f "dist/$ZIP_NAME"

# Create ZIP from dist directory contents
cd dist || exit 1
zip -r "$ZIP_NAME" * -x "*.zip" > /dev/null 2>&1
cd .. || exit 1

# Move ZIP to project root
mv "dist/$ZIP_NAME" "$ZIP_NAME"

if [ -f "$ZIP_NAME" ]; then
    ZIP_SIZE=$(ls -lh "$ZIP_NAME" | awk '{print $5}')
    echo -e "${GREEN}[OK]${RESET} Distribution ZIP created: ${BLUE}${ZIP_NAME}${RESET} (${CYAN}${ZIP_SIZE}${RESET})"
else
    echo -e "${RED}[ERROR]${RESET} Failed to create ZIP"
    exit 1
fi
echo ""

# Success summary
echo -e "${BOLD}${GREEN}================================================================${RESET}"
echo -e "${BOLD}${GREEN}  Build Complete${RESET}"
echo -e "${BOLD}${GREEN}================================================================${RESET}"
echo ""
echo -e "${BOLD}Artifacts:${RESET}"
echo -e "  • ${BLUE}dist/TCraftClient.jar${RESET}             Executable JAR"
echo -e "  • ${BLUE}dist/TCraft Client.command${RESET}        macOS launcher"
echo -e "  • ${BLUE}dist/TCraft Client.bat${RESET}            Windows launcher"
echo -e "  • ${BLUE}dist/TCraft Client.sh${RESET}             Linux launcher"
echo -e "  • ${BLUE}${ZIP_NAME}${RESET}    Distribution package"
echo ""
echo -e "${BOLD}Next steps:${RESET}"
echo -e "  ${CYAN}1.${RESET} Test locally:        ${BLUE}java -jar dist/TCraftClient.jar${RESET}"
echo -e "  ${CYAN}2.${RESET} Build installer:     ${BLUE}./build-installer.sh${RESET}"
echo -e "  ${CYAN}3.${RESET} Distribute package:  ${BLUE}${ZIP_NAME}${RESET}"
echo ""


