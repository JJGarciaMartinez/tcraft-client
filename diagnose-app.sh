#!/bin/bash

# TCraft Client - Diagnostic Script for macOS App Bundle
# Diagnoses why the app won't open on other Macs

set -e

# ANSI Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
RESET='\033[0m'

echo -e "${BOLD}${CYAN}================================================================${RESET}"
echo -e "${BOLD}${CYAN}  TCraft Client - Diagnostic Tool${RESET}"
echo -e "${BOLD}${CYAN}================================================================${RESET}"
echo ""

# Find DMG file in installer directory
DMG_FILE=$(find installer -name "*.dmg" -type f | head -n 1)

if [ -z "$DMG_FILE" ]; then
    echo -e "${RED}[ERROR]${RESET} No DMG file found in ${BLUE}installer/${RESET}"
    echo -e "${CYAN}[INFO]${RESET} Run ${BLUE}./build-installer.sh${RESET} to create one"
    exit 1
fi

echo -e "${GREEN}[OK]${RESET} DMG file found: ${BLUE}$DMG_FILE${RESET}"
echo ""

# Mount the DMG
echo -e "${YELLOW}[INFO]${RESET} Mounting DMG..."
hdiutil attach "$DMG_FILE" -mountpoint /tmp/tcraft_mount 2>/dev/null

if [ $? -ne 0 ]; then
    echo -e "${RED}[ERROR]${RESET} Failed to mount DMG"
    exit 1
fi

echo -e "${GREEN}[OK]${RESET} DMG mounted at ${BLUE}/tmp/tcraft_mount${RESET}"
echo ""

# Find the app bundle
APP_PATH=$(find /tmp/tcraft_mount -name "*.app" -maxdepth 1 | head -n 1)

if [ -z "$APP_PATH" ]; then
    echo -e "${RED}[ERROR]${RESET} No .app bundle found in DMG"
    hdiutil detach /tmp/tcraft_mount 2>/dev/null
    exit 1
fi

echo -e "${GREEN}[OK]${RESET} App bundle found: ${BLUE}$APP_PATH${RESET}"
echo ""

# Check app structure
echo -e "${BOLD}${CYAN}================================================================${RESET}"
echo -e "${BOLD}  App Bundle Structure${RESET}"
echo -e "${BOLD}${CYAN}================================================================${RESET}"
ls -la "$APP_PATH/Contents/"
echo ""

# Check if runtime is included
echo -e "${BOLD}${CYAN}================================================================${RESET}"
echo -e "${BOLD}  Java Runtime Check${RESET}"
echo -e "${BOLD}${CYAN}================================================================${RESET}"
if [ -d "$APP_PATH/Contents/runtime" ]; then
    echo -e "${GREEN}[OK]${RESET} Java runtime is bundled"
    echo -e "        Location: ${BLUE}$APP_PATH/Contents/runtime${RESET}"

    # Check Java version
    if [ -f "$APP_PATH/Contents/runtime/Contents/Home/bin/java" ]; then
        JAVA_VERSION=$("$APP_PATH/Contents/runtime/Contents/Home/bin/java" -version 2>&1 | head -n 1)
        echo -e "        Runtime version: ${CYAN}$JAVA_VERSION${RESET}"
    fi
else
    echo -e "${RED}[ERROR]${RESET} NO Java runtime bundled - This is the problem!"
    echo -e "          The app requires Java to be installed on the target Mac"
    echo ""
    echo -e "${BOLD}Solution:${RESET}"
    echo -e "  Use ${BLUE}--runtime-image${RESET} option when building"
fi
echo ""

# Check Info.plist
echo -e "${BOLD}${CYAN}================================================================${RESET}"
echo -e "${BOLD}  Info.plist Configuration${RESET}"
echo -e "${BOLD}${CYAN}================================================================${RESET}"
if [ -f "$APP_PATH/Contents/Info.plist" ]; then
    echo -e "${BOLD}Main class:${RESET}"
    /usr/libexec/PlistBuddy -c "Print :JVMMainClassName" "$APP_PATH/Contents/Info.plist" 2>/dev/null || echo "  Not found"
    echo ""
    echo -e "${BOLD}Main JAR:${RESET}"
    /usr/libexec/PlistBuddy -c "Print :JVMAppClasspath" "$APP_PATH/Contents/Info.plist" 2>/dev/null || echo "  Not found"
    echo ""
    echo -e "${BOLD}Bundle identifier:${RESET}"
    /usr/libexec/PlistBuddy -c "Print :CFBundleIdentifier" "$APP_PATH/Contents/Info.plist" 2>/dev/null || echo "  Not found"
fi
echo ""

# Check for JAR file
echo -e "${BOLD}${CYAN}================================================================${RESET}"
echo -e "${BOLD}  JAR Files${RESET}"
echo -e "${BOLD}${CYAN}================================================================${RESET}"
find "$APP_PATH/Contents" -name "*.jar" -exec ls -lh {} \;
echo ""

# Check for signature/notarization
echo -e "${BOLD}${CYAN}================================================================${RESET}"
echo -e "${BOLD}  Code Signature Check${RESET}"
echo -e "${BOLD}${CYAN}================================================================${RESET}"
codesign -dv "$APP_PATH" 2>&1 | head -n 5
echo ""

# Check if app can be executed
echo -e "${BOLD}${CYAN}================================================================${RESET}"
echo -e "${BOLD}  Execution Test${RESET}"
echo -e "${BOLD}${CYAN}================================================================${RESET}"
echo -e "${YELLOW}[INFO]${RESET} Attempting to open app..."
open "$APP_PATH" &
OPEN_PID=$!
sleep 3

# Check if process is running
if ps -p $OPEN_PID > /dev/null 2>&1; then
    echo -e "${GREEN}[OK]${RESET} App appears to be running (PID: ${CYAN}$OPEN_PID${RESET})"
    echo -e "        Check if window appeared"
else
    echo -e "${RED}[ERROR]${RESET} App process terminated immediately"
    echo ""
    echo -e "${YELLOW}[INFO]${RESET} Checking Console logs for errors..."
    log show --predicate 'process == "TCraft Client"' --last 1m --info 2>/dev/null | tail -n 20
fi

# Cleanup
echo ""
echo -e "${YELLOW}[INFO]${RESET} Cleaning up..."
hdiutil detach /tmp/tcraft_mount 2>/dev/null
echo -e "${GREEN}[OK]${RESET} Done"

