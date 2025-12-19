#!/bin/bash
# Script to copy assets to IntelliJ output directory
# Run after compiling or use as pre-build task

OUTPUT_DIR="out/production/tcraft-client"

echo "Copying assets to output directory..."

# Create assets directory if it doesn't exist
mkdir -p "$OUTPUT_DIR/assets"

# Copy all assets
cp -r assets/* "$OUTPUT_DIR/assets/"

echo "✓ Assets successfully copied to $OUTPUT_DIR/assets/"

