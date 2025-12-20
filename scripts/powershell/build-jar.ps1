# TCraft Client - JAR Build Script (PowerShell)
# Creates an executable JAR file from compiled classes

$ErrorActionPreference = "Stop"

# Directory configuration (Windows paths)
$OUTPUT_DIR = "out\production\tcraft-client"
$DIST_DIR = "dist"
$JAR_FILE = "$DIST_DIR\TCraftClient.jar"

# Load version from version.properties
if (-not (Test-Path "version.properties")) {
    Write-Host "[ERROR] version.properties not found" -ForegroundColor Red
    exit 1
}

# Read properties from file
$properties = Get-Content "version.properties" | Where-Object { $_ -match '=' }
$APP_VERSION = ($properties | Where-Object { $_ -match '^app.version=' }) -replace '^app.version=', ''
$APP_NAME = ($properties | Where-Object { $_ -match '^app.name=' }) -replace '^app.name=', ''

# Validate that version was loaded
if ([string]::IsNullOrEmpty($APP_VERSION) -or [string]::IsNullOrEmpty($APP_NAME)) {
    Write-Host "[ERROR] Could not read app.version or app.name from version.properties" -ForegroundColor Red
    exit 1
}

Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "  Building $APP_NAME v$APP_VERSION" -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host ""

# Compile sources first
Write-Host "[1/5] Compiling sources..." -ForegroundColor Yellow
& ".\scripts\powershell\compile-sources.ps1"
if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Compilation failed, cannot build JAR" -ForegroundColor Red
    exit 1
}
Write-Host ""

# Create dist directory if it doesn't exist
New-Item -ItemType Directory -Path $DIST_DIR -Force | Out-Null

# Clean old assets from production output
Write-Host "[2/5] Cleaning old artifacts..." -ForegroundColor Yellow
$cleanPaths = @(
    "$OUTPUT_DIR\assets",
    "$OUTPUT_DIR\version.properties",
    "$OUTPUT_DIR\*.png",
    "$OUTPUT_DIR\*.icns",
    "$OUTPUT_DIR\*.ico",
    "$OUTPUT_DIR\font"
)

foreach ($path in $cleanPaths) {
    if (Test-Path $path) {
        Remove-Item -Path $path -Recurse -Force -ErrorAction SilentlyContinue
    }
}
Write-Host "[OK]" -ForegroundColor Green -NoNewline
Write-Host " Cleaned stale resources"
Write-Host ""

# Copy assets to production output
Write-Host "[3/5] Packaging resources..." -ForegroundColor Yellow
Copy-Item -Path "assets" -Destination "$OUTPUT_DIR\" -Recurse -Force
Write-Host "[OK]" -ForegroundColor Green -NoNewline
Write-Host " Assets copied"

# Copy version.properties to production output
Copy-Item -Path "version.properties" -Destination "$OUTPUT_DIR\" -Force
Write-Host "[OK]" -ForegroundColor Green -NoNewline
Write-Host " version.properties copied"
Write-Host ""

# Create JAR file
Write-Host "[4/5] Creating JAR archive..." -ForegroundColor Yellow
jar cvfm $JAR_FILE manifest.txt -C $OUTPUT_DIR . | Out-Null

# Verify JAR was created
if (Test-Path $JAR_FILE) {
    $jarSize = (Get-Item $JAR_FILE).Length
    $jarSizeKB = [math]::Round($jarSize / 1KB, 2)
    Write-Host "[OK]" -ForegroundColor Green -NoNewline
    Write-Host " JAR created: " -NoNewline
    Write-Host $JAR_FILE -ForegroundColor Blue -NoNewline
    Write-Host " (" -NoNewline
    Write-Host "$jarSizeKB KB" -ForegroundColor Cyan -NoNewline
    Write-Host ")"
} else {
    Write-Host "[ERROR] Failed to create JAR" -ForegroundColor Red
    exit 1
}
Write-Host ""

# Create platform-specific launchers
Write-Host "[5/5] Creating platform launchers..." -ForegroundColor Yellow

# macOS launcher (.command)
@'
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
'@ | Out-File -FilePath "$DIST_DIR\TCraft Client.command" -Encoding ASCII -NoNewline

# Windows launcher (.bat)
@'
@echo off
REM TCraft Client Launcher for Windows
cd /d "%~dp0"
java -jar TCraftClient.jar
if %errorlevel% neq 0 (
    echo.
    echo Press any key to exit...
    pause >nul
)
'@ | Out-File -FilePath "$DIST_DIR\TCraft Client.bat" -Encoding ASCII

# Linux launcher (.sh)
@'
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
'@ | Out-File -FilePath "$DIST_DIR\TCraft Client.sh" -Encoding ASCII -NoNewline

# Create README
@'
# TCraft Client - How to Run

## Quick Start

macOS:    Double-click "TCraft Client.command"
Windows:  Double-click "TCraft Client.bat"
Linux:    Run "./TCraft Client.sh" in terminal
Any OS:   Run "java -jar TCraftClient.jar" in terminal

## Requirements
- Java 16 or higher

Check version: java -version
Download Java: https://adoptium.net/
'@ | Out-File -FilePath "$DIST_DIR\README.txt" -Encoding UTF8

Write-Host "[OK]" -ForegroundColor Green -NoNewline
Write-Host " Platform launchers created"
Write-Host "      " -NoNewline
Write-Host "TCraft Client.command" -ForegroundColor Blue -NoNewline
Write-Host " (macOS)"
Write-Host "      " -NoNewline
Write-Host "TCraft Client.bat" -ForegroundColor Blue -NoNewline
Write-Host " (Windows)"
Write-Host "      " -NoNewline
Write-Host "TCraft Client.sh" -ForegroundColor Blue -NoNewline
Write-Host " (Linux)"
Write-Host "      " -NoNewline
Write-Host "README.txt" -ForegroundColor Blue -NoNewline
Write-Host " (instructions)"
Write-Host ""

# Create ZIP archive
$ZIP_NAME = "$APP_NAME-$APP_VERSION.zip"
Write-Host "Creating distribution package..." -ForegroundColor White
Write-Host ""

# Remove old ZIP if exists
if (Test-Path $ZIP_NAME) {
    Remove-Item $ZIP_NAME -Force
}

# Create ZIP from dist directory contents
Compress-Archive -Path "$DIST_DIR\*" -DestinationPath $ZIP_NAME -Force

if (Test-Path $ZIP_NAME) {
    $zipSize = (Get-Item $ZIP_NAME).Length
    $zipSizeKB = [math]::Round($zipSize / 1KB, 2)
    Write-Host "[OK]" -ForegroundColor Green -NoNewline
    Write-Host " Distribution ZIP created: " -NoNewline
    Write-Host "$ZIP_NAME" -ForegroundColor Blue -NoNewline
    Write-Host " (" -NoNewline
    Write-Host "$zipSizeKB KB" -ForegroundColor Cyan -NoNewline
    Write-Host ")"
} else {
    Write-Host "[ERROR] Failed to create ZIP" -ForegroundColor Red
    exit 1
}
Write-Host ""

# Success summary
Write-Host "================================================================" -ForegroundColor Green
Write-Host "  Build Complete" -ForegroundColor Green
Write-Host "================================================================" -ForegroundColor Green
Write-Host ""
Write-Host "Artifacts:" -ForegroundColor White
Write-Host "  • " -NoNewline
Write-Host "$DIST_DIR\TCraftClient.jar" -ForegroundColor Blue -NoNewline
Write-Host "             Executable JAR"
Write-Host "  • " -NoNewline
Write-Host "$DIST_DIR\TCraft Client.command" -ForegroundColor Blue -NoNewline
Write-Host "        macOS launcher"
Write-Host "  • " -NoNewline
Write-Host "$DIST_DIR\TCraft Client.bat" -ForegroundColor Blue -NoNewline
Write-Host "            Windows launcher"
Write-Host "  • " -NoNewline
Write-Host "$DIST_DIR\TCraft Client.sh" -ForegroundColor Blue -NoNewline
Write-Host "             Linux launcher"
Write-Host "  • " -NoNewline
Write-Host "$ZIP_NAME" -ForegroundColor Blue -NoNewline
Write-Host "    Distribution package"
Write-Host ""
Write-Host "Next steps:" -ForegroundColor White
Write-Host "  1. " -ForegroundColor Cyan -NoNewline
Write-Host "Test locally:        " -NoNewline
Write-Host "java -jar $DIST_DIR\TCraftClient.jar" -ForegroundColor Blue
Write-Host "  2. " -ForegroundColor Cyan -NoNewline
Write-Host "Build installer:     " -NoNewline
Write-Host ".\build-installer.ps1" -ForegroundColor Blue
Write-Host "  3. " -ForegroundColor Cyan -NoNewline
Write-Host "Distribute package:  " -NoNewline
Write-Host "$ZIP_NAME" -ForegroundColor Blue
Write-Host ""


