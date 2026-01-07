# TCraft Client - Asset Copy Script (PowerShell)
# Copies assets to IntelliJ output directory
# Run after compiling or use as pre-build task

$ErrorActionPreference = "Stop"

# Change to project root directory
Set-Location (Join-Path $PSScriptRoot "..\..")

$OUTPUT_DIR = "out\production\tcraft-client"

Write-Host "Copying assets to output directory..."

# Create assets directory if it doesn't exist
New-Item -ItemType Directory -Path "$OUTPUT_DIR\assets" -Force | Out-Null

# Copy all assets
if (Test-Path "assets") {
    Copy-Item -Path "assets\*" -Destination "$OUTPUT_DIR\assets\" -Recurse -Force
    Write-Host "[OK]" -ForegroundColor Green -NoNewline
    Write-Host " Assets successfully copied to " -NoNewline
    Write-Host "$OUTPUT_DIR\assets\" -ForegroundColor Blue
} else {
    Write-Host "[ERROR]" -ForegroundColor Red -NoNewline
    Write-Host " Assets folder not found"
    exit 1
}


