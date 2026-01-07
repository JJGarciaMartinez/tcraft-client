# TCraft Client - Source Compilation Script (PowerShell)
# Compiles Java sources with Java 16 compatibility
# Requires Java 16+ due to use of records and text blocks

$ErrorActionPreference = "Stop"  # Exit on any error

# Change to project root directory
Set-Location (Join-Path $PSScriptRoot "..\..")

# Directory configuration
$OUTPUT_DIR = "out\production\tcraft-client"

# Header
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "  TCraft Client - Clean Build Compilation" -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Clean old output
Write-Host "[1/4] Cleaning build artifacts..." -ForegroundColor Yellow
if (Test-Path "out") {
    Remove-Item -Recurse -Force "out"
    Write-Host "[OK]" -ForegroundColor Green -NoNewline
    Write-Host " Removed previous 'out/' directory"
} else {
    Write-Host "[OK]" -ForegroundColor Green -NoNewline
    Write-Host " No previous build artifacts found"
}
Write-Host ""

# Step 2: Create fresh output directories
Write-Host "[2/4] Creating output directories..." -ForegroundColor Yellow
New-Item -ItemType Directory -Path $OUTPUT_DIR -Force | Out-Null
Write-Host "[OK]" -ForegroundColor Green -NoNewline
Write-Host " Created " -NoNewline
Write-Host $OUTPUT_DIR -ForegroundColor Blue
Write-Host ""

# Step 3: Compile Java sources
Write-Host "[3/4] Compiling Java sources..." -ForegroundColor Yellow
Write-Host "      Compiler target: " -NoNewline
Write-Host "Java 16" -ForegroundColor Cyan -NoNewline
Write-Host " (bytecode version " -NoNewline
Write-Host "60.0" -ForegroundColor Cyan -NoNewline
Write-Host ")"

# Find all Java source files
$SOURCES = Get-ChildItem -Path "src" -Filter "*.java" -Recurse | Select-Object -ExpandProperty FullName
$SOURCE_COUNT = $SOURCES.Count
Write-Host "      Source files: " -NoNewline
Write-Host "$SOURCE_COUNT" -ForegroundColor Cyan
Write-Host ""

# Compile with Java 16 target
$sourcesArg = $SOURCES -join " "
$javacCommand = "javac -source 16 -target 16 -d $OUTPUT_DIR -cp `"$OUTPUT_DIR`" $sourcesArg"

try {
    Invoke-Expression $javacCommand
    Write-Host "[OK]" -ForegroundColor Green -NoNewline
    Write-Host " Compilation successful"
} catch {
    Write-Host "[ERROR]" -ForegroundColor Red -NoNewline
    Write-Host " Compilation failed"
    Write-Host $_.Exception.Message -ForegroundColor Red
    exit 1
}
Write-Host ""

# Step 4: Copy assets
Write-Host "[4/4] Copying resource assets..." -ForegroundColor Yellow
if (Test-Path "scripts\powershell\copy-assets.ps1") {
    & ".\scripts\powershell\copy-assets.ps1"
} elseif (Test-Path "scripts\bash\copy-assets.sh") {
    # Try to run the bash script if Git Bash is available
    if (Get-Command "bash" -ErrorAction SilentlyContinue) {
        bash ".\scripts\bash\copy-assets.sh"
    } else {
        # Manual copy if no script is available
        if (Test-Path "assets") {
            Copy-Item -Path "assets" -Destination "$OUTPUT_DIR\" -Recurse -Force
            Write-Host "[OK]" -ForegroundColor Green -NoNewline
            Write-Host " Assets copied successfully"
        } else {
            Write-Host "[WARNING]" -ForegroundColor Yellow -NoNewline
            Write-Host " No assets folder found"
        }
    }
} else {
    # Manual copy
    if (Test-Path "assets") {
        Copy-Item -Path "assets" -Destination "$OUTPUT_DIR\" -Recurse -Force
        Write-Host "[OK]" -ForegroundColor Green -NoNewline
        Write-Host " Assets copied successfully"
    } else {
        Write-Host "[WARNING]" -ForegroundColor Yellow -NoNewline
        Write-Host " No assets folder found"
    }
}
Write-Host ""

# Success summary
Write-Host "================================================================" -ForegroundColor Green
Write-Host "  Build Compilation Complete" -ForegroundColor Green
Write-Host "================================================================" -ForegroundColor Green
Write-Host ""
Write-Host "Output: " -NoNewline
Write-Host $OUTPUT_DIR -ForegroundColor Blue
Write-Host ""
Write-Host "Next Steps:" -ForegroundColor White
Write-Host ""
Write-Host "  1. " -ForegroundColor Cyan -NoNewline
Write-Host "Test locally:" -ForegroundColor White
Write-Host "     Run " -NoNewline
Write-Host "Main.class" -ForegroundColor Blue -NoNewline
Write-Host " from IntelliJ IDEA"
Write-Host ""
Write-Host "  2. " -ForegroundColor Cyan -NoNewline
Write-Host "Build JAR package:" -ForegroundColor White
Write-Host "     " -NoNewline
Write-Host ".\scripts\powershell\build-jar.ps1" -ForegroundColor Blue -NoNewline
Write-Host " (or " -NoNewline
Write-Host "bash .\scripts\bash\build-jar.sh" -ForegroundColor Blue -NoNewline
Write-Host ")"
Write-Host ""
Write-Host "  3. " -ForegroundColor Cyan -NoNewline
Write-Host "Build native installer:" -ForegroundColor White
Write-Host "     " -NoNewline
Write-Host ".\scripts\powershell\build-installer.ps1" -ForegroundColor Blue -NoNewline
Write-Host " (or " -NoNewline
Write-Host "bash .\scripts\bash\build-installer.sh" -ForegroundColor Blue -NoNewline
Write-Host ")"
Write-Host ""

