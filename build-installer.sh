#!/bin/bash

# TCraft Client - Native Installer Build Script
# Creates platform-specific installers using jpackage

APP_NAME="TCraft Client"
APP_VERSION="1.0.0"
VENDOR="TCraft Team"
MAIN_CLASS="Main"
JAR_FILE="dist/TCraftClient.jar"

echo "Building TCraft Client Installer..."

# Check if JAR exists
if [ ! -f "$JAR_FILE" ]; then
    echo "JAR file not found. Running build-jar.sh first..."
    ./build-jar.sh
fi

# Create installer directory
mkdir -p installer

# Detect platform and build appropriate installer
case "$(uname -s)" in
    Darwin*)
        echo "Building macOS installer (.dmg)..."

        # Build jpackage command
        JPACKAGE_CMD="jpackage \
            --input dist \
            --name \"$APP_NAME\" \
            --main-jar TCraftClient.jar \
            --main-class $MAIN_CLASS \
            --type dmg \
            --app-version $APP_VERSION \
            --vendor \"$VENDOR\" \
            --dest installer \
            --mac-package-name \"TCraftClient\""

        # Add icon if it exists
        if [ -f "assets/icon.icns" ]; then
            JPACKAGE_CMD="$JPACKAGE_CMD --icon assets/icon.icns"
        else
            echo "Warning: icon.icns not found, building without custom icon"
        fi

        # Execute command
        eval $JPACKAGE_CMD

        echo "✓ macOS installer created: installer/$APP_NAME-$APP_VERSION.dmg"
        ;;
    
    Linux*)
        echo "Building Linux installer (.deb)..."

        # Build jpackage command
        JPACKAGE_CMD="jpackage \
            --input dist \
            --name \"$APP_NAME\" \
            --main-jar TCraftClient.jar \
            --main-class $MAIN_CLASS \
            --type deb \
            --app-version $APP_VERSION \
            --vendor \"$VENDOR\" \
            --dest installer \
            --linux-package-name \"tcraft-client\""

        # Add icon if it exists
        if [ -f "assets/icon.png" ]; then
            JPACKAGE_CMD="$JPACKAGE_CMD --icon assets/icon.png"
        else
            echo "Warning: icon.png not found, building without custom icon"
        fi

        # Execute command
        eval $JPACKAGE_CMD

        echo "✓ Linux installer created: installer/tcraft-client_$APP_VERSION-1_amd64.deb"
        ;;
    
    MINGW*|MSYS*|CYGWIN*)
        echo "Building Windows installer (.exe)..."

        # Build jpackage command
        JPACKAGE_CMD="jpackage \
            --input dist \
            --name \"$APP_NAME\" \
            --main-jar TCraftClient.jar \
            --main-class $MAIN_CLASS \
            --type exe \
            --app-version $APP_VERSION \
            --vendor \"$VENDOR\" \
            --dest installer \
            --win-dir-chooser \
            --win-menu \
            --win-shortcut"

        # Add icon if it exists
        if [ -f "assets/icon.ico" ]; then
            JPACKAGE_CMD="$JPACKAGE_CMD --icon assets/icon.ico"
        else
            echo "Warning: icon.ico not found, building without custom icon"
        fi

        # Execute command
        eval $JPACKAGE_CMD

        echo "✓ Windows installer created: installer/$APP_NAME-$APP_VERSION.exe"
        ;;
    
    *)
        echo "Unknown platform. Creating generic app-image..."
        jpackage \
            --input dist \
            --name "$APP_NAME" \
            --main-jar TCraftClient.jar \
            --main-class $MAIN_CLASS \
            --type app-image \
            --app-version $APP_VERSION \
            --vendor "$VENDOR" \
            --dest installer
        ;;
esac

echo ""
echo "Build complete! Installer available in: ./installer/"
