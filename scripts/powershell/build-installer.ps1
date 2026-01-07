# TCraft Client - Native Installer Build Script (PowerShell)
# Creates platform-specific installers using jpackage with WiX

$ErrorActionPreference = "Stop"

# Load version from version.properties
if (-not (Test-Path "version.properties")) {
    Write-Host "[ERROR] version.properties not found" -ForegroundColor Red
    exit 1
}

# Read properties from file
$properties = Get-Content "version.properties" | Where-Object { $_ -match '=' }
$APP_VERSION = ($properties | Where-Object { $_ -match '^app.version=' }) -replace '^app.version=', ''
$APP_VERSION_NUMERIC = ($properties | Where-Object { $_ -match '^app.version.numeric=' }) -replace '^app.version.numeric=', ''
$APP_NAME = ($properties | Where-Object { $_ -match '^app.name=' }) -replace '^app.name=', ''
$VENDOR = ($properties | Where-Object { $_ -match '^app.vendor=' }) -replace '^app.vendor=', ''
$WIN_UPGRADE_UUID = ($properties | Where-Object { $_ -match '^app.win.upgrade.uuid=' }) -replace '^app.win.upgrade.uuid=', ''

# Validate that version was loaded
if ([string]::IsNullOrEmpty($APP_VERSION)) {
    Write-Host "[ERROR] Could not read app.version from version.properties" -ForegroundColor Red
    exit 1
}

# Use numeric version for jpackage, fallback to regular version if not set
if ([string]::IsNullOrEmpty($APP_VERSION_NUMERIC)) {
    $APP_VERSION_NUMERIC = $APP_VERSION
    Write-Host "[WARN] app.version.numeric not set, using app.version" -ForegroundColor Yellow
}

# Validate numeric version has max 3 components (jpackage requirement)
$versionComponents = $APP_VERSION_NUMERIC.Split('.')
if ($versionComponents.Count -gt 3) {
    Write-Host "[ERROR] app.version.numeric has $($versionComponents.Count) components: $APP_VERSION_NUMERIC" -ForegroundColor Red
    Write-Host "          jpackage requires max 3 components (e.g., 1.2.3)" -ForegroundColor Red
    Write-Host ""
    Write-Host "Fix by running:" -ForegroundColor White
    Write-Host "  .\scripts\powershell\update-version.ps1 $APP_VERSION" -ForegroundColor Blue
    Write-Host ""
    Write-Host "Or manually edit version.properties to use max 3 numbers"
    exit 1
}

$MAIN_CLASS = "Main"
$JAR_FILE = "dist\TCraftClient.jar"

Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "  Building Native Installer - $APP_NAME v$APP_VERSION" -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host ""

# Check if JAR exists
if (-not (Test-Path $JAR_FILE)) {
    Write-Host "[WARN] JAR file not found, building..." -ForegroundColor Yellow
    & ".\scripts\powershell\build-jar.ps1"
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERROR] Failed to build JAR" -ForegroundColor Red
        exit 1
    }
    Write-Host ""
}

# Function to check if WiX is installed
function Test-WixInstalled {
    try {
        $wixPath = Get-Command "candle.exe" -ErrorAction SilentlyContinue
        if ($wixPath) {
            return $true
        }

        # Check in common installation paths
        $commonPaths = @(
            "${env:ProgramFiles(x86)}\WiX Toolset v3.11\bin",
            "${env:ProgramFiles(x86)}\WiX Toolset v3.14\bin",
            "${env:ProgramFiles}\WiX Toolset v3.11\bin",
            "${env:ProgramFiles}\WiX Toolset v3.14\bin"
        )

        foreach ($path in $commonPaths) {
            if (Test-Path "$path\candle.exe") {
                $env:PATH = "$path;$env:PATH"
                return $true
            }
        }

        return $false
    }
    catch {
        return $false
    }
}

# Function to install WiX Toolset
function Install-WixToolset {
    Write-Host "[SETUP] WiX Toolset not found, installing..." -ForegroundColor Yellow
    Write-Host ""

    # Check if dotnet tool is available (for WiX 4.x via dotnet tool)
    $dotnetAvailable = Get-Command "dotnet" -ErrorAction SilentlyContinue

    if ($dotnetAvailable) {
        Write-Host "[INFO] Installing WiX via .NET tool..." -ForegroundColor Cyan
        try {
            # Install WiX as a global .NET tool
            dotnet tool install --global wix --version 4.0.5
            if ($LASTEXITCODE -eq 0) {
                Write-Host "[OK] WiX installed successfully via .NET tool" -ForegroundColor Green
                return $true
            }
        }
        catch {
            Write-Host "[WARN] Failed to install WiX via .NET tool, trying alternative method..." -ForegroundColor Yellow
        }
    }

    # Alternative: Download and install WiX 3.11 manually
    Write-Host "[INFO] Downloading WiX Toolset 3.11..." -ForegroundColor Cyan

    $wixVersion = "3.11.2"
    $wixUrl = "https://github.com/wixtoolset/wix3/releases/download/wix3112rtm/wix311-binaries.zip"
    $wixZip = "$env:TEMP\wix-binaries.zip"
    $wixExtractPath = "$env:LOCALAPPDATA\WiX"

    try {
        # Download WiX binaries
        Write-Host "[INFO] Downloading from: $wixUrl" -ForegroundColor Gray
        Invoke-WebRequest -Uri $wixUrl -OutFile $wixZip -UseBasicParsing

        # Extract to local directory
        Write-Host "[INFO] Extracting to: $wixExtractPath" -ForegroundColor Gray
        if (Test-Path $wixExtractPath) {
            Remove-Item $wixExtractPath -Recurse -Force
        }
        Expand-Archive -Path $wixZip -DestinationPath $wixExtractPath -Force

        # Add to PATH for current session
        $env:PATH = "$wixExtractPath;$env:PATH"

        # Verify installation
        if (Test-Path "$wixExtractPath\candle.exe") {
            Write-Host "[OK] WiX Toolset installed successfully to: $wixExtractPath" -ForegroundColor Green
            Write-Host "[INFO] Added to PATH for current session" -ForegroundColor Cyan
            Write-Host ""
            Write-Host "[NOTE] To make WiX available permanently, add to system PATH:" -ForegroundColor Yellow
            Write-Host "       $wixExtractPath" -ForegroundColor Gray
            Write-Host ""

            # Clean up
            Remove-Item $wixZip -Force -ErrorAction SilentlyContinue
            return $true
        }
        else {
            throw "WiX binaries not found after extraction"
        }
    }
    catch {
        Write-Host "[ERROR] Failed to install WiX Toolset: $_" -ForegroundColor Red
        Write-Host ""
        Write-Host "Please install WiX manually from:" -ForegroundColor Yellow
        Write-Host "  https://wixtoolset.org/releases/" -ForegroundColor Cyan
        Write-Host ""
        return $false
    }
}

# Check and install WiX if needed
Write-Host "Platform: Windows" -ForegroundColor White
Write-Host "Package:  .msi installer (WiX)" -ForegroundColor White
Write-Host ""

if (-not (Test-WixInstalled)) {
    if (-not (Install-WixToolset)) {
        Write-Host "[ERROR] Cannot proceed without WiX Toolset" -ForegroundColor Red
        exit 1
    }
}
else {
    Write-Host "[OK] WiX Toolset is installed" -ForegroundColor Green
}

Write-Host ""

# Create installer directory
New-Item -ItemType Directory -Path "installer" -Force | Out-Null

# Clean previous build artifacts
Write-Host "[INFO] Cleaning previous build artifacts..." -ForegroundColor Cyan
Remove-Item "installer\*.msi" -Force -ErrorAction SilentlyContinue
Remove-Item "installer\*.exe" -Force -ErrorAction SilentlyContinue

# Build jpackage command with Windows-specific options
$jpackageArgs = @(
    "--input", "dist",
    "--name", $APP_NAME,
    "--main-jar", "TCraftClient.jar",
    "--main-class", $MAIN_CLASS,
    "--type", "msi",
    "--app-version", $APP_VERSION_NUMERIC,
    "--vendor", $VENDOR,
    "--dest", "installer",
    "--win-dir-chooser",
    "--win-menu",
    "--win-shortcut",
    "--win-menu-group", $APP_NAME
)

# Add Windows upgrade UUID if available (required for seamless updates)
if (-not [string]::IsNullOrEmpty($WIN_UPGRADE_UUID)) {
    $jpackageArgs += "--win-upgrade-uuid"
    $jpackageArgs += $WIN_UPGRADE_UUID
    Write-Host "[OK] Using Windows upgrade UUID: $WIN_UPGRADE_UUID" -ForegroundColor Green
}
else {
    Write-Host "[WARN] win-upgrade-uuid not set, users will need to manually uninstall old versions" -ForegroundColor Yellow
}

# Add icon if it exists
if (Test-Path "assets\icon.ico") {
    $jpackageArgs += "--icon"
    $jpackageArgs += "assets\icon.ico"
    Write-Host "[OK] Using custom icon: assets\icon.ico" -ForegroundColor Green
}
else {
    Write-Host "[WARN] icon.ico not found, building without custom icon" -ForegroundColor Yellow
}

# Add Java options
$jpackageArgs += "--java-options"
$jpackageArgs += "-Xmx1024m"
$jpackageArgs += "--java-options"
$jpackageArgs += "-Dfile.encoding=UTF-8"

# Execute jpackage
Write-Host ""
Write-Host "Running jpackage..." -ForegroundColor White
Write-Host ""

try {
    & jpackage $jpackageArgs

    if ($LASTEXITCODE -eq 0) {
        # Rename installer to use display version (with phase identifier)
        $numericInstaller = "installer\$APP_NAME-$APP_VERSION_NUMERIC.msi"
        $displayInstaller = "installer\$APP_NAME-$APP_VERSION.msi"

        if (Test-Path $numericInstaller) {
            if (Test-Path $displayInstaller) {
                Remove-Item $displayInstaller -Force
            }
            Move-Item $numericInstaller $displayInstaller
        }

        if (Test-Path $displayInstaller) {
            $fileSize = (Get-Item $displayInstaller).Length
            $fileSizeMB = [math]::Round($fileSize / 1MB, 2)
            Write-Host ""
            Write-Host "[OK] Windows installer created: installer\$APP_NAME-$APP_VERSION.msi ($fileSizeMB MB)" -ForegroundColor Green
        }
        else {
            Write-Host ""
            Write-Host "[OK] Windows installer created successfully" -ForegroundColor Green
        }

        Write-Host ""
        Write-Host "================================================================" -ForegroundColor Green
        Write-Host "  Installer Build Complete" -ForegroundColor Green
        Write-Host "================================================================" -ForegroundColor Green
        Write-Host ""
        Write-Host "Output: .\installer\" -ForegroundColor White
        Write-Host ""
        Write-Host "Distribution Notes:" -ForegroundColor Yellow
        Write-Host "  - Users can install via double-clicking the .msi file" -ForegroundColor White
        Write-Host "  - The installer will add Start Menu shortcuts" -ForegroundColor White
        Write-Host "  - Users can choose the installation directory" -ForegroundColor White
        Write-Host ""
    }
    else {
        Write-Host ""
        Write-Host "[ERROR] Failed to create Windows installer" -ForegroundColor Red
        Write-Host ""
        Write-Host "Common issues:" -ForegroundColor Yellow
        Write-Host "  - Ensure WiX Toolset is properly installed" -ForegroundColor White
        Write-Host "  - Check that app.version.numeric has max 3 components" -ForegroundColor White
        Write-Host "  - Verify the JAR file exists and is valid" -ForegroundColor White
        Write-Host ""
        exit 1
    }
}
catch {
    Write-Host ""
    Write-Host "[ERROR] Exception during installer creation: $_" -ForegroundColor Red
    Write-Host ""
    exit 1
}
