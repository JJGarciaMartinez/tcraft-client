#!/bin/bash

# TCraft Client - JAR Build Script
# Creates an executable JAR file from compiled classes

# Load version from version.properties
if [ ! -f "version.properties" ]; then
    echo "Error: version.properties not found"
    exit 1
fi

# Read properties from file
APP_VERSION=$(grep '^app.version=' version.properties | cut -d'=' -f2)
APP_NAME=$(grep '^app.name=' version.properties | cut -d'=' -f2)

# Validate that version was loaded
if [ -z "$APP_VERSION" ] || [ -z "$APP_NAME" ]; then
    echo "Error: Could not read app.version or app.name from version.properties"
    exit 1
fi

echo "Building $APP_NAME v$APP_VERSION..."

# Create dist directory if it doesn't exist
mkdir -p dist

# Copy assets to production output
echo "Copying assets..."
cp -r assets out/production/

# Copy version.properties to production output (to be included in JAR)
echo "Copying version.properties..."
cp version.properties out/production/

# Create JAR file
echo "Creating JAR..."
jar cvfm dist/TCraftClient.jar manifest.txt -C out/production .

# Verify JAR was created
if [ -f "dist/TCraftClient.jar" ]; then
    echo "✓ JAR created successfully: dist/TCraftClient.jar"
else
    echo "✗ Failed to create JAR"
    exit 1
fi

# Create platform-specific launchers
echo "Creating launchers..."

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
- Java 14 or higher

Check version: java -version
Download Java: https://adoptium.net/
EOF

echo "✓ Launchers created for all platforms"
echo ""
echo "Distribution package ready in: dist/"
echo "  • TCraftClient.jar (main application)"
echo "  • TCraft Client.command (macOS)"
echo "  • TCraft Client.bat (Windows)"
echo "  • TCraft Client.sh (Linux)"
echo "  • README.txt (instructions)"
echo ""

# Create ZIP archive
ZIP_NAME="${APP_NAME}-${APP_VERSION}.zip"
echo "Creating distribution ZIP: $ZIP_NAME"

# Remove old ZIP if exists
rm -f "dist/$ZIP_NAME"

# Create ZIP from dist directory contents
cd dist || exit 1
zip -r "$ZIP_NAME" * -x "*.zip"
cd .. || exit 1

# Move ZIP to project root
mv "dist/$ZIP_NAME" "$ZIP_NAME"

if [ -f "$ZIP_NAME" ]; then
    echo "✓ ZIP created successfully: $ZIP_NAME"
    echo ""
    echo "Ready to distribute:"
    echo "  • $ZIP_NAME (all platforms)"
else
    echo "✗ Failed to create ZIP"
    exit 1
fi


