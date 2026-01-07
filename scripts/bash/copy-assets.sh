#!/bin/bash

# TCraft Client - Asset Copy Script
# Copies assets to IntelliJ output directory
# Run after compiling or use as pre-build task

set -e

# Ensure all scripts have execute permissions
source "$(dirname "$0")/ensure-permissions.sh"

# Change to project root directory
cd "$(dirname "$0")/../.."

# ANSI Color codes
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RESET='\033[0m'

OUTPUT_DIR="out/production/tcraft-client"

echo "Copying assets to output directory..."

# Create assets directory if it doesn't exist
mkdir -p "$OUTPUT_DIR/assets"

# Copy all assets
cp -r assets/* "$OUTPUT_DIR/assets/"

echo -e "${GREEN}[OK]${RESET} Assets successfully copied to ${BLUE}${OUTPUT_DIR}/assets/${RESET}"


