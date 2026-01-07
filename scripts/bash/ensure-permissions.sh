#!/bin/bash

# TCraft Client - Ensure Script Permissions
# Automatically grants execute permissions to all bash scripts if needed
# This script is sourced by other scripts to ensure proper permissions

# Get the directory where bash scripts are located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Flag to track if any permissions were changed
PERMISSIONS_CHANGED=false

# Check and fix permissions for all .sh files in the bash directory
for script in "$SCRIPT_DIR"/*.sh; do
    if [ -f "$script" ]; then
        # Check if the script is executable
        if [ ! -x "$script" ]; then
            # Grant execute permissions
            chmod +x "$script" 2>/dev/null
            if [ $? -eq 0 ]; then
                PERMISSIONS_CHANGED=true
            fi
        fi
    fi
done

# If permissions were changed, notify (optional, can be silenced)
if [ "$PERMISSIONS_CHANGED" = true ]; then
    # Silent execution - permissions fixed automatically
    # Uncomment the line below if you want to see a notification
    # echo "✓ Script permissions have been automatically updated"
    :
fi

