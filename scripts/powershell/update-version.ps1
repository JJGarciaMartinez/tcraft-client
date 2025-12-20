# TCraft Client - Version Update Script (PowerShell)
# Updates version strings in version.properties

param(
    [Parameter(Mandatory=$true, Position=0)]
    [string]$NewVersion,

    [Parameter(Mandatory=$false, Position=1)]
    [string]$NewVersionNumeric = ""
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrEmpty($NewVersion)) {
    Write-Host "[ERROR] Missing version argument" -ForegroundColor Red
    Write-Host ""
    Write-Host "Usage:" -ForegroundColor White
    Write-Host "  .\update-version.ps1 <new-version> [numeric-version]" -ForegroundColor Blue
    Write-Host ""
    Write-Host "Examples:" -ForegroundColor White
    Write-Host "  .\update-version.ps1 b1.0.3 1.0.3" -ForegroundColor Blue -NoNewline
    Write-Host "   # Beta with explicit numeric" -ForegroundColor Yellow
    Write-Host "  .\update-version.ps1 1.0.3" -ForegroundColor Blue -NoNewline
    Write-Host "          # Stable release" -ForegroundColor Yellow
    exit 1
}

# Set numeric version
if ([string]::IsNullOrEmpty($NewVersionNumeric)) {
    $NewVersionNumeric = $NewVersion
}

# Extract numeric version if not provided and version starts with letter
if (($NewVersionNumeric -eq $NewVersion) -and ($NewVersion -match '^[a-zA-Z]')) {
    # Remove leading letters (like 'b', 'a', 'rc') from version
    $NewVersionNumeric = $NewVersion -replace '^[a-zA-Z]+', ''
    Write-Host "[INFO]" -ForegroundColor Cyan -NoNewline
    Write-Host " Auto-detected numeric version: " -NoNewline
    Write-Host "$NewVersionNumeric" -ForegroundColor White
}

# Validate and limit numeric version to max 3 components (jpackage requirement)
$components = $NewVersionNumeric -split '\.'
$COMPONENT_COUNT = $components.Count

if ($COMPONENT_COUNT -gt 3) {
    Write-Host "[WARN]" -ForegroundColor Yellow -NoNewline
    Write-Host " Numeric version has $COMPONENT_COUNT components, jpackage allows max 3"
    # Truncate to first 3 components
    $NewVersionNumeric = ($components[0..2] -join '.')
    Write-Host "[INFO]" -ForegroundColor Cyan -NoNewline
    Write-Host " Truncated to: " -NoNewline
    Write-Host "$NewVersionNumeric" -ForegroundColor White
}

# Backup version.properties
if (Test-Path "version.properties") {
    Copy-Item "version.properties" "version.properties.bak" -Force
}

# Update version.properties
$content = Get-Content "version.properties"
$content = $content -replace '^app\.version=.*', "app.version=$NewVersion"
$content = $content -replace '^app\.version\.numeric=.*', "app.version.numeric=$NewVersionNumeric"
$content | Set-Content "version.properties" -Encoding UTF8

# Remove backup file
Remove-Item "version.properties.bak" -Force -ErrorAction SilentlyContinue

Write-Host "[OK]" -ForegroundColor Green -NoNewline
Write-Host " Version updated to " -NoNewline
Write-Host "$NewVersion" -ForegroundColor White
Write-Host "[OK]" -ForegroundColor Green -NoNewline
Write-Host " Numeric version set to " -NoNewline
Write-Host "$NewVersionNumeric" -ForegroundColor White
Write-Host ""
Write-Host "Impact:" -ForegroundColor White
Write-Host "  • " -NoNewline
Write-Host "AppConfig.java" -ForegroundColor Blue -NoNewline
Write-Host "        Runtime display: " -NoNewline
Write-Host "$NewVersion" -ForegroundColor Cyan
Write-Host "  • " -NoNewline
Write-Host "build-installer.ps1" -ForegroundColor Blue -NoNewline
Write-Host "    Installer version: " -NoNewline
Write-Host "$NewVersionNumeric" -ForegroundColor Cyan
Write-Host ""
Write-Host "Next steps:" -ForegroundColor White
Write-Host "  .\scripts\powershell\compile-sources.ps1; .\scripts\powershell\build-jar.ps1; .\scripts\powershell\build-installer.ps1" -ForegroundColor Blue
Write-Host ""


