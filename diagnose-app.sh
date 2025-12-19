#!/bin/bash

# TCraft Client - Diagnostic Script for macOS App Bundle
# This script helps diagnose why the app won't open on other Macs

echo "=== TCraft Client - Diagnostic Tool ==="
echo ""

# Check if DMG exists
if [ ! -f "installer/TCraft Client-b1.0.1.dmg" ]; then
    echo "❌ DMG file not found in installer/"
    exit 1
fi

echo "✓ DMG file found"
echo ""

# Mount the DMG
echo "Mounting DMG..."
hdiutil attach "installer/TCraft Client-b1.0.1.dmg" -mountpoint /tmp/tcraft_mount 2>/dev/null

if [ $? -ne 0 ]; then
    echo "❌ Failed to mount DMG"
    exit 1
fi

echo "✓ DMG mounted at /tmp/tcraft_mount"
echo ""

# Find the app bundle
APP_PATH=$(find /tmp/tcraft_mount -name "*.app" -maxdepth 1 | head -n 1)

if [ -z "$APP_PATH" ]; then
    echo "❌ No .app bundle found in DMG"
    hdiutil detach /tmp/tcraft_mount 2>/dev/null
    exit 1
fi

echo "✓ App bundle found: $APP_PATH"
echo ""

# Check app structure
echo "=== App Bundle Structure ==="
ls -la "$APP_PATH/Contents/"
echo ""

# Check if runtime is included
echo "=== Java Runtime Check ==="
if [ -d "$APP_PATH/Contents/runtime" ]; then
    echo "✓ Java runtime is bundled"
    echo "Runtime location: $APP_PATH/Contents/runtime"

    # Check Java version
    if [ -f "$APP_PATH/Contents/runtime/Contents/Home/bin/java" ]; then
        JAVA_VERSION=$("$APP_PATH/Contents/runtime/Contents/Home/bin/java" -version 2>&1 | head -n 1)
        echo "Runtime version: $JAVA_VERSION"
    fi
else
    echo "❌ NO Java runtime bundled - This is the problem!"
    echo "   The app requires Java to be installed on the target Mac"
    echo ""
    echo "   Solution: Use --runtime-image option when building"
fi
echo ""

# Check Info.plist
echo "=== Info.plist Configuration ==="
if [ -f "$APP_PATH/Contents/Info.plist" ]; then
    echo "Main class:"
    /usr/libexec/PlistBuddy -c "Print :JVMMainClassName" "$APP_PATH/Contents/Info.plist" 2>/dev/null || echo "  Not found"
    echo ""
    echo "Main JAR:"
    /usr/libexec/PlistBuddy -c "Print :JVMAppClasspath" "$APP_PATH/Contents/Info.plist" 2>/dev/null || echo "  Not found"
    echo ""
    echo "Bundle identifier:"
    /usr/libexec/PlistBuddy -c "Print :CFBundleIdentifier" "$APP_PATH/Contents/Info.plist" 2>/dev/null || echo "  Not found"
fi
echo ""

# Check for JAR file
echo "=== JAR Files ==="
find "$APP_PATH/Contents" -name "*.jar" -exec ls -lh {} \;
echo ""

# Check for signature/notarization
echo "=== Code Signature Check ==="
codesign -dv "$APP_PATH" 2>&1 | head -n 5
echo ""

# Check if app can be executed
echo "=== Execution Test ==="
echo "Attempting to open app..."
open "$APP_PATH" &
OPEN_PID=$!
sleep 3

# Check if process is running
if ps -p $OPEN_PID > /dev/null 2>&1; then
    echo "✓ App appears to be running (PID: $OPEN_PID)"
    echo "  Check if window appeared"
else
    echo "❌ App process terminated immediately"
    echo ""
    echo "Checking Console logs for errors..."
    log show --predicate 'process == "TCraft Client"' --last 1m --info 2>/dev/null | tail -n 20
fi

# Cleanup
echo ""
echo "Cleaning up..."
hdiutil detach /tmp/tcraft_mount 2>/dev/null
echo "✓ Done"

