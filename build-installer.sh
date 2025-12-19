#!/bin/bash

# TCraft Client - Native Installer Build Script
# Creates platform-specific installers using jpackage

# Load version from version.properties
if [ ! -f "version.properties" ]; then
    echo "Error: version.properties not found"
    exit 1
fi

# Read properties from file
APP_VERSION=$(grep '^app.version=' version.properties | cut -d'=' -f2)
APP_VERSION_NUMERIC=$(grep '^app.version.numeric=' version.properties | cut -d'=' -f2)
APP_NAME=$(grep '^app.name=' version.properties | cut -d'=' -f2)
VENDOR=$(grep '^app.vendor=' version.properties | cut -d'=' -f2)

# Validate that version was loaded
if [ -z "$APP_VERSION" ]; then
    echo "Error: Could not read app.version from version.properties"
    exit 1
fi

# Use numeric version for jpackage, fallback to regular version if not set
if [ -z "$APP_VERSION_NUMERIC" ]; then
    APP_VERSION_NUMERIC="$APP_VERSION"
    echo "Warning: app.version.numeric not set, using app.version"
fi

# Validate numeric version has max 3 components (jpackage requirement)
COMPONENT_COUNT=$(echo "$APP_VERSION_NUMERIC" | tr '.' '\n' | wc -l | tr -d ' ')
if [ "$COMPONENT_COUNT" -gt 3 ]; then
    echo "Error: app.version.numeric has $COMPONENT_COUNT components: $APP_VERSION_NUMERIC"
    echo "jpackage requires max 3 components (e.g., 1.2.3)"
    echo ""
    echo "Fix by running:"
    echo "  ./update-version.sh $APP_VERSION"
    echo ""
    echo "Or manually edit version.properties to use max 3 numbers"
    exit 1
fi

MAIN_CLASS="Main"
JAR_FILE="dist/TCraftClient.jar"

echo "Building $APP_NAME v$APP_VERSION..."

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

        # Clean previous build artifacts
        rm -rf installer/*.dmg installer/*.app 2>/dev/null

        # Build jpackage command with macOS-specific options
        JPACKAGE_CMD="jpackage \
            --input dist \
            --name \"$APP_NAME\" \
            --main-jar TCraftClient.jar \
            --main-class $MAIN_CLASS \
            --type dmg \
            --app-version $APP_VERSION_NUMERIC \
            --vendor \"$VENDOR\" \
            --dest installer \
            --mac-package-name \"TCraftClient\" \
            --mac-package-identifier \"com.tcraft.client\" \
            --java-options '-Dapple.awt.application.name=TCraft Client' \
            --java-options '-Xmx1024m' \
            --java-options '-Dfile.encoding=UTF-8'"

        # Add icon if it exists
        if [ -f "assets/icon.icns" ]; then
            JPACKAGE_CMD="$JPACKAGE_CMD --icon assets/icon.icns"
        else
            echo "Warning: icon.icns not found, building without custom icon"
        fi

        # Execute command
        echo "Running jpackage..."
        eval $JPACKAGE_CMD

        if [ $? -eq 0 ]; then
            # Rename installer to use display version (with phase identifier)
            if [ -f "installer/$APP_NAME-$APP_VERSION_NUMERIC.dmg" ]; then
                mv "installer/$APP_NAME-$APP_VERSION_NUMERIC.dmg" "installer/$APP_NAME-$APP_VERSION.dmg"
                echo "✓ macOS installer created: installer/$APP_NAME-$APP_VERSION.dmg"
            else
                echo "✓ macOS installer created successfully"
            fi
            echo ""
            echo "Important: For distribution to other Macs:"
            echo "1. Users may need to right-click → Open (first time only)"
            echo "2. Or: System Settings → Privacy & Security → Open Anyway"
            echo ""
            echo "To avoid this, sign the app with:"
            echo "  codesign --force --deep --sign - \"installer/$APP_NAME.app\""
        else
            echo "✗ Failed to create macOS installer"
            exit 1
        fi
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
            --app-version $APP_VERSION_NUMERIC \
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

        if [ $? -eq 0 ]; then
            # Rename installer to use display version (with phase identifier)
            if [ -f "installer/tcraft-client_${APP_VERSION_NUMERIC}-1_amd64.deb" ]; then
                mv "installer/tcraft-client_${APP_VERSION_NUMERIC}-1_amd64.deb" "installer/tcraft-client_${APP_VERSION}-1_amd64.deb"
            fi
            echo "✓ Linux installer created: installer/tcraft-client_$APP_VERSION-1_amd64.deb"
        else
            echo "✗ Failed to create Linux installer"
            exit 1
        fi
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
            --app-version $APP_VERSION_NUMERIC \
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

        if [ $? -eq 0 ]; then
            # Rename installer to use display version (with phase identifier)
            if [ -f "installer/$APP_NAME-$APP_VERSION_NUMERIC.exe" ]; then
                mv "installer/$APP_NAME-$APP_VERSION_NUMERIC.exe" "installer/$APP_NAME-$APP_VERSION.exe"
            fi
            echo "✓ Windows installer created: installer/$APP_NAME-$APP_VERSION.exe"
        else
            echo "✗ Failed to create Windows installer"
            exit 1
        fi
        ;;
    
    *)
        echo "Unknown platform. Creating generic app-image..."
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
echo "Build complete! Installer available in: ./installer/"
