#!/bin/bash

# TCraft Client - Native Installer Build Script
# Creates platform-specific installers using jpackage

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

# Load version from version.properties
if [ ! -f "version.properties" ]; then
    echo -e "${RED}[ERROR]${RESET} version.properties not found"
    exit 1
fi

# Read properties from file
APP_VERSION=$(grep '^app.version=' version.properties | cut -d'=' -f2)
APP_VERSION_NUMERIC=$(grep '^app.version.numeric=' version.properties | cut -d'=' -f2)
APP_NAME=$(grep '^app.name=' version.properties | cut -d'=' -f2)
VENDOR=$(grep '^app.vendor=' version.properties | cut -d'=' -f2)

# Validate that version was loaded
if [ -z "$APP_VERSION" ]; then
    echo -e "${RED}[ERROR]${RESET} Could not read app.version from version.properties"
    exit 1
fi

# Use numeric version for jpackage, fallback to regular version if not set
if [ -z "$APP_VERSION_NUMERIC" ]; then
    APP_VERSION_NUMERIC="$APP_VERSION"
    echo -e "${YELLOW}[WARN]${RESET} app.version.numeric not set, using app.version"
fi

# Validate numeric version has max 3 components (jpackage requirement)
COMPONENT_COUNT=$(echo "$APP_VERSION_NUMERIC" | tr '.' '\n' | wc -l | tr -d ' ')
if [ "$COMPONENT_COUNT" -gt 3 ]; then
    echo -e "${RED}[ERROR]${RESET} app.version.numeric has ${COMPONENT_COUNT} components: ${APP_VERSION_NUMERIC}"
    echo -e "          jpackage requires max 3 components (e.g., 1.2.3)"
    echo ""
    echo -e "${BOLD}Fix by running:${RESET}"
    echo -e "  ${BLUE}./update-version.sh${RESET} ${CYAN}${APP_VERSION}${RESET}"
    echo ""
    echo "Or manually edit version.properties to use max 3 numbers"
    exit 1
fi

MAIN_CLASS="Main"
JAR_FILE="dist/TCraftClient.jar"

echo -e "${BOLD}${CYAN}================================================================${RESET}"
echo -e "${BOLD}${CYAN}  Building Native Installer - ${APP_NAME} v${APP_VERSION}${RESET}"
echo -e "${BOLD}${CYAN}================================================================${RESET}"
echo ""

# Check if JAR exists
if [ ! -f "$JAR_FILE" ]; then
    echo -e "${YELLOW}[WARN]${RESET} JAR file not found, building..."
    ./scripts/bash/build-jar.sh
    echo ""
fi

# Create installer directory
mkdir -p installer

# Detect platform and build appropriate installer
case "$(uname -s)" in
    Darwin*)
        echo -e "${BOLD}Platform:${RESET} macOS"
        echo -e "${BOLD}Package:${RESET}  .dmg installer"
        echo ""

        # Clean previous build artifacts
        rm -rf installer/*.dmg installer/*.app 2>/dev/null

        # Build jpackage command with macOS-specific options
        # Note: Quoting the main-class to prevent parsing issues with app name
        JPACKAGE_CMD="jpackage \
            --input dist \
            --name \"$APP_NAME\" \
            --main-jar TCraftClient.jar \
            --main-class \"$MAIN_CLASS\" \
            --type dmg \
            --app-version $APP_VERSION_NUMERIC \
            --vendor \"$VENDOR\" \
            --dest installer \
            --mac-package-name \"TCraftClient\" \
            --mac-package-identifier \"com.tcraft.client\" \
            --java-options '-Dapple.awt.application.name=TCraftClient' \
            --java-options '-Xmx1024m' \
            --java-options '-Dfile.encoding=UTF-8'"

        # Add icon if it exists
        if [ -f "assets/icon.icns" ]; then
            JPACKAGE_CMD="$JPACKAGE_CMD --icon assets/icon.icns"
            echo -e "${GREEN}[OK]${RESET} Using custom icon: ${BLUE}assets/icon.icns${RESET}"
        else
            echo -e "${YELLOW}[WARN]${RESET} icon.icns not found, building without custom icon"
        fi

        # Execute command
        echo ""
        echo -e "${BOLD}Running jpackage...${RESET}"
        eval $JPACKAGE_CMD

        if [ $? -eq 0 ]; then
            # Rename installer to use display version (with phase identifier like beta-, alpha-, etc)
            # jpackage creates file with numeric version, we rename to full version string
            if [ -f "installer/$APP_NAME-$APP_VERSION_NUMERIC.dmg" ]; then
                # Only rename if versions differ (e.g., beta-26.1.3 vs 26.1.3)
                if [ "$APP_VERSION" != "$APP_VERSION_NUMERIC" ]; then
                    mv "installer/$APP_NAME-$APP_VERSION_NUMERIC.dmg" "installer/$APP_NAME-$APP_VERSION.dmg"
                fi
                DMG_SIZE=$(ls -lh "installer/$APP_NAME-$APP_VERSION.dmg" | awk '{print $5}')
                echo -e "${GREEN}[OK]${RESET} macOS installer created: ${BLUE}installer/$APP_NAME-$APP_VERSION.dmg${RESET} (${CYAN}${DMG_SIZE}${RESET})"
            else
                echo -e "${GREEN}[OK]${RESET} macOS installer created successfully"
            fi
            echo ""
            echo -e "${BOLD}${YELLOW}Distribution Notes:${RESET}"
            echo -e "  Users may need to bypass Gatekeeper on first launch:"
            echo -e "    ${CYAN}1.${RESET} Right-click → Open (first time only)"
            echo -e "    ${CYAN}2.${RESET} Or: System Settings → Privacy & Security → Open Anyway"
            echo ""
            echo -e "${BOLD}Optional - Code signing:${RESET}"
            echo -e "  ${BLUE}codesign --force --deep --sign - \"installer/$APP_NAME.app\"${RESET}"
        else
            echo -e "${RED}[ERROR]${RESET} Failed to create macOS installer"
            exit 1
        fi
        ;;

    Linux*)
        echo -e "${BOLD}Platform:${RESET} Linux"
        echo -e "${BOLD}Package:${RESET}  .deb installer"
        echo ""

        # Build jpackage command
        JPACKAGE_CMD="jpackage \
            --input dist \
            --name \"$APP_NAME\" \
            --main-jar TCraftClient.jar \
            --main-class $MAIN_CLASS \
            --type deb \
            --app-version $APP_VERSION_NUMERIC \
            --vendor \"$VENDOR\" \
            --dest installer \
            --linux-package-name \"tcraft-client\""

        # Add icon if it exists
        if [ -f "assets/icon.png" ]; then
            JPACKAGE_CMD="$JPACKAGE_CMD --icon assets/icon.png"
            echo -e "${GREEN}[OK]${RESET} Using custom icon: ${BLUE}assets/icon.png${RESET}"
        else
            echo -e "${YELLOW}[WARN]${RESET} icon.png not found, building without custom icon"
        fi

        # Execute command
        echo ""
        echo -e "${BOLD}Running jpackage...${RESET}"
        eval $JPACKAGE_CMD > /dev/null 2>&1

        if [ $? -eq 0 ]; then
            # Rename installer to use display version (with phase identifier)
            if [ -f "installer/tcraft-client_${APP_VERSION_NUMERIC}-1_amd64.deb" ]; then
                mv "installer/tcraft-client_${APP_VERSION_NUMERIC}-1_amd64.deb" "installer/tcraft-client_${APP_VERSION}-1_amd64.deb"
            fi
            DEB_SIZE=$(ls -lh "installer/tcraft-client_$APP_VERSION-1_amd64.deb" | awk '{print $5}')
            echo -e "${GREEN}[OK]${RESET} Linux installer created: ${BLUE}installer/tcraft-client_$APP_VERSION-1_amd64.deb${RESET} (${CYAN}${DEB_SIZE}${RESET})"
        else
            echo -e "${RED}[ERROR]${RESET} Failed to create Linux installer"
            exit 1
        fi
        ;;

    MINGW*|MSYS*|CYGWIN*)
        echo -e "${BOLD}Platform:${RESET} Windows"
        echo -e "${BOLD}Package:${RESET}  .exe installer"
        echo ""

        # Build jpackage command
        JPACKAGE_CMD="jpackage \
            --input dist \
            --name \"$APP_NAME\" \
            --main-jar TCraftClient.jar \
            --main-class $MAIN_CLASS \
            --type exe \
            --app-version $APP_VERSION_NUMERIC \
            --vendor \"$VENDOR\" \
            --dest installer \
            --win-dir-chooser \
            --win-menu \
            --win-shortcut"

        # Add icon if it exists
        if [ -f "assets/icon.ico" ]; then
            JPACKAGE_CMD="$JPACKAGE_CMD --icon assets/icon.ico"
            echo -e "${GREEN}[OK]${RESET} Using custom icon: ${BLUE}assets/icon.ico${RESET}"
        else
            echo -e "${YELLOW}[WARN]${RESET} icon.ico not found, building without custom icon"
        fi

        # Execute command
        echo ""
        echo -e "${BOLD}Running jpackage...${RESET}"
        eval $JPACKAGE_CMD

        if [ $? -eq 0 ]; then
            # Rename installer to use display version (with phase identifier)
            if [ -f "installer/$APP_NAME-$APP_VERSION_NUMERIC.exe" ]; then
                mv "installer/$APP_NAME-$APP_VERSION_NUMERIC.exe" "installer/$APP_NAME-$APP_VERSION.exe"
            fi
            EXE_SIZE=$(ls -lh "installer/$APP_NAME-$APP_VERSION.exe" | awk '{print $5}')
            echo -e "${GREEN}[OK]${RESET} Windows installer created: ${BLUE}installer/$APP_NAME-$APP_VERSION.exe${RESET} (${CYAN}${EXE_SIZE}${RESET})"
        else
            echo -e "${RED}[ERROR]${RESET} Failed to create Windows installer"
            exit 1
        fi
        ;;

    *)
        echo -e "${YELLOW}[WARN]${RESET} Unknown platform, creating generic app-image..."
        jpackage \
            --input dist \
            --name "$APP_NAME" \
            --main-jar TCraftClient.jar \
            --main-class $MAIN_CLASS \
            --type app-image \
            --app-version $APP_VERSION_NUMERIC \
            --vendor "$VENDOR" \
            --dest installer
        ;;
esac

echo ""
echo -e "${BOLD}${GREEN}================================================================${RESET}"
echo -e "${BOLD}${GREEN}  Installer Build Complete${RESET}"
echo -e "${BOLD}${GREEN}================================================================${RESET}"
echo ""
echo -e "${BOLD}Output:${RESET} ${BLUE}./installer/${RESET}"
echo ""

