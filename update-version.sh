#!/bin/bash

# Update Version Script
# Updates the version in version.properties

if [ -z "$1" ]; then
    echo "Usage: ./update-version.sh <new-version> [numeric-version]"
    echo "Example: ./update-version.sh b1.0.3 1.0.3"
    echo "         ./update-version.sh 1.0.3"
    exit 1
fi

NEW_VERSION="$1"
NEW_VERSION_NUMERIC="${2:-$NEW_VERSION}"

# Extract numeric version if not provided and version starts with letter
if [ "$NEW_VERSION_NUMERIC" == "$NEW_VERSION" ] && [[ "$NEW_VERSION" =~ ^[a-zA-Z] ]]; then
    # Remove leading letters (like 'b', 'a', 'rc') from version
    NEW_VERSION_NUMERIC=$(echo "$NEW_VERSION" | sed 's/^[a-zA-Z]*//')
    echo "Auto-detected numeric version: $NEW_VERSION_NUMERIC"
fi

# Validate and limit numeric version to max 3 components (jpackage requirement)
COMPONENT_COUNT=$(echo "$NEW_VERSION_NUMERIC" | tr '.' '\n' | wc -l | tr -d ' ')
if [ "$COMPONENT_COUNT" -gt 3 ]; then
    echo "Warning: Numeric version has $COMPONENT_COUNT components, jpackage allows max 3"
    # Truncate to first 3 components
    NEW_VERSION_NUMERIC=$(echo "$NEW_VERSION_NUMERIC" | cut -d'.' -f1-3)
    echo "Truncated to: $NEW_VERSION_NUMERIC"
fi

# Update version.properties
sed -i.bak "s/^app.version=.*/app.version=$NEW_VERSION/" version.properties
sed -i.bak "s/^app.version.numeric=.*/app.version.numeric=$NEW_VERSION_NUMERIC/" version.properties

# Remove backup file
rm version.properties.bak 2>/dev/null

echo "✓ Version updated to $NEW_VERSION in version.properties"
echo "✓ Numeric version set to $NEW_VERSION_NUMERIC"
echo ""
echo "This change will affect:"
echo "  - AppConfig.java (runtime version display: $NEW_VERSION)"
echo "  - build-installer.sh (installer version: $NEW_VERSION_NUMERIC)"
echo ""
echo "Rebuild your project to see changes:"
echo "  ./build-jar.sh && ./build-installer.sh"

