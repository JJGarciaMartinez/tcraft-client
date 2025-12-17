#!/bin/bash

# TCraft Client - JAR Build Script
# Creates an executable JAR file from compiled classes

echo "Building TCraft Client JAR..."

# Create dist directory if it doesn't exist
mkdir -p dist

# Copy assets to production output
echo "Copying assets..."
cp -r assets out/production/

# Create JAR file
echo "Creating JAR..."
jar cvfm dist/TCraftClient.jar manifest.txt -C out/production .

# Verify JAR was created
if [ -f "dist/TCraftClient.jar" ]; then
    echo "✓ JAR created successfully: dist/TCraftClient.jar"
    echo "  Run with: java -jar dist/TCraftClient.jar"
else
    echo "✗ Failed to create JAR"
    exit 1
fi
