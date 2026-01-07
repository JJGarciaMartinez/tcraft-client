#!/bin/bash

# TCraft Client - Source Compilation Script
# Compiles Java sources with Java 16 compatibility
# Requires Java 16+ due to use of records and text blocks

set -e  # Exit on any error

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

# Header
echo -e "${BOLD}${CYAN}================================================================${RESET}"
echo -e "${BOLD}${CYAN}  TCraft Client - Clean Build Compilation${RESET}"
echo -e "${BOLD}${CYAN}================================================================${RESET}"
echo ""

# Step 1: Clean old output
echo -e "${YELLOW}[1/4] Cleaning build artifacts...${RESET}"
if [ -d "out" ]; then
    rm -rf out
    echo -e "${GREEN}[OK]${RESET} Removed previous 'out/' directory"
else
    echo -e "${GREEN}[OK]${RESET} No previous build artifacts found"
fi
echo ""

# Step 2: Create fresh output directories
echo -e "${YELLOW}[2/4] Creating output directories...${RESET}"
mkdir -p out/production/tcraft-client
echo -e "${GREEN}[OK]${RESET} Created ${BLUE}out/production/tcraft-client${RESET}"
echo ""

# Step 3: Compile Java sources
echo -e "${YELLOW}[3/4] Compiling Java sources...${RESET}"
echo -e "      Compiler target: ${CYAN}Java 16${RESET} (bytecode version ${CYAN}60.0${RESET})"

# Find all Java source files
SOURCES=$(find src -name "*.java")
SOURCE_COUNT=$(echo "$SOURCES" | wc -l | tr -d ' ')
echo -e "      Source files: ${CYAN}${SOURCE_COUNT}${RESET}"
echo ""

# Compile with Java 16 target
javac -source 16 -target 16 \
    -d out/production/tcraft-client \
    -cp "out/production/tcraft-client" \
    $SOURCES

if [ $? -eq 0 ]; then
    echo -e "${GREEN}[OK]${RESET} Compilation successful"
else
    echo -e "${RED}[ERROR]${RESET} Compilation failed"
    exit 1
fi
echo ""

# Step 4: Copy assets
echo -e "${YELLOW}[4/4] Copying resource assets...${RESET}"
./scripts/bash/copy-assets.sh
if [ $? -eq 0 ]; then
    echo -e "${GREEN}[OK]${RESET} Assets copied successfully"
else
    echo -e "${RED}[ERROR]${RESET} Failed to copy assets"
    exit 1
fi
echo ""

# Success summary
echo -e "${BOLD}${GREEN}================================================================${RESET}"
echo -e "${BOLD}${GREEN}  Build Compilation Complete${RESET}"
echo -e "${BOLD}${GREEN}================================================================${RESET}"
echo ""
echo -e "${BOLD}Output:${RESET} ${BLUE}out/production/tcraft-client${RESET}"
echo ""
echo -e "${BOLD}Next Steps:${RESET}"
echo ""
echo -e "  ${CYAN}1.${RESET} ${BOLD}Test locally:${RESET}"
echo -e "     Run ${BLUE}Main.class${RESET} from IntelliJ IDEA"
echo ""
echo -e "  ${CYAN}2.${RESET} ${BOLD}Build JAR package:${RESET}"
echo -e "     ${BLUE}./scripts/bash/build-jar.sh${RESET}"
echo ""
echo -e "  ${CYAN}3.${RESET} ${BOLD}Build native installer:${RESET}"
echo -e "     ${BLUE}./scripts/bash/build-installer.sh${RESET}"
echo ""
echo -e "  ${CYAN}4.${RESET} ${BOLD}Complete build pipeline:${RESET}"
echo -e "     ${BLUE}./scripts/bash/build-jar.sh && ./scripts/bash/build-installer.sh${RESET}"
echo ""


