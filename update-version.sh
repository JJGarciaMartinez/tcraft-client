#!/bin/bash

# TCraft Client - Version Update Script
# Updates version strings in version.properties

set -e

# ANSI Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
RESET='\033[0m'

if [ -z "$1" ]; then
    echo -e "${RED}[ERROR]${RESET} Missing version argument"
    echo ""
    echo -e "${BOLD}Usage:${RESET}"
    echo -e "  ${BLUE}./update-version.sh${RESET} ${CYAN}<new-version>${RESET} ${CYAN}[numeric-version]${RESET}"
    echo ""
    echo -e "${BOLD}Examples:${RESET}"
    echo -e "  ${BLUE}./update-version.sh${RESET} ${CYAN}b1.0.3 1.0.3${RESET}   ${YELLOW}# Beta with explicit numeric${RESET}"
    echo -e "  ${BLUE}./update-version.sh${RESET} ${CYAN}1.0.3${RESET}          ${YELLOW}# Stable release${RESET}"
    exit 1
fi

NEW_VERSION="$1"
NEW_VERSION_NUMERIC="${2:-$NEW_VERSION}"

# Extract numeric version if not provided and version starts with letter
if [ "$NEW_VERSION_NUMERIC" == "$NEW_VERSION" ] && [[ "$NEW_VERSION" =~ ^[a-zA-Z] ]]; then
    # Remove leading letters (like 'b', 'a', 'rc') from version
    NEW_VERSION_NUMERIC=$(echo "$NEW_VERSION" | sed 's/^[a-zA-Z]*//')
    echo -e "${CYAN}[INFO]${RESET} Auto-detected numeric version: ${BOLD}${NEW_VERSION_NUMERIC}${RESET}"
fi

# Validate and limit numeric version to max 3 components (jpackage requirement)
COMPONENT_COUNT=$(echo "$NEW_VERSION_NUMERIC" | tr '.' '\n' | wc -l | tr -d ' ')
if [ "$COMPONENT_COUNT" -gt 3 ]; then
    echo -e "${YELLOW}[WARN]${RESET} Numeric version has ${COMPONENT_COUNT} components, jpackage allows max 3"
    # Truncate to first 3 components
    NEW_VERSION_NUMERIC=$(echo "$NEW_VERSION_NUMERIC" | cut -d'.' -f1-3)
    echo -e "${CYAN}[INFO]${RESET} Truncated to: ${BOLD}${NEW_VERSION_NUMERIC}${RESET}"
fi

# Update version.properties
sed -i.bak "s/^app.version=.*/app.version=$NEW_VERSION/" version.properties
sed -i.bak "s/^app.version.numeric=.*/app.version.numeric=$NEW_VERSION_NUMERIC/" version.properties

# Remove backup file
rm version.properties.bak 2>/dev/null

echo -e "${GREEN}[OK]${RESET} Version updated to ${BOLD}${NEW_VERSION}${RESET}"
echo -e "${GREEN}[OK]${RESET} Numeric version set to ${BOLD}${NEW_VERSION_NUMERIC}${RESET}"
echo ""
echo -e "${BOLD}Impact:${RESET}"
echo -e "  • ${BLUE}AppConfig.java${RESET}        Runtime display: ${CYAN}${NEW_VERSION}${RESET}"
echo -e "  • ${BLUE}build-installer.sh${RESET}    Installer version: ${CYAN}${NEW_VERSION_NUMERIC}${RESET}"
echo ""
echo -e "${BOLD}Next steps:${RESET}"
echo -e "  ${BLUE}./compile-sources.sh && ./build-jar.sh && ./build-installer.sh${RESET}"

