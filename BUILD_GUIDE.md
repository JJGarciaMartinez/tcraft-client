# TCraft Client - Build and Distribution Guide

This guide explains the complete workflow for creating TCraft Client builds, from version updates to generating native installers.

## Complete Workflow

```
1. Update Version → 2. Clean Compile → 3. Build JAR → 4. Build Installer
   (update-version.sh)  (compile-sources.sh)  (build-jar.sh)  (build-installer.sh)
```

---

## Step 1: Update Version (CRITICAL)

**Before starting any build**, you must update the project version using the `update-version.sh` script.

> **📖 For detailed versioning documentation, see [VERSION_GUIDE.md](VERSION_GUIDE.md)**

### Dual-Version System

TCraft Client implements a dual-version scheme to maintain compatibility with `jpackage` requirements while supporting semantic versioning with phase identifiers (beta, alpha, rc).

**Two version properties:**
- `app.version` - Display version with phase identifiers (e.g., `b26.1.0`)
- `app.version.numeric` - Numeric-only version for jpackage (e.g., `26.1.0`)

### Year-Based Versioning

TCraft Client uses a **year-based versioning scheme**: `YY.MAJOR.FIXES`

- **YY**: Last two digits of planned release year (e.g., `26` for 2026)
- **MAJOR**: Version number within that year (starts at 1, never 0)
- **FIXES**: Patch/fix number (0 for initial release, increments for bug fixes)

**Phase identifiers:** `a` (alpha), `b` (beta), `rc` (release candidate)

**Important:** The year represents the **planned release**, not current development year.

**Examples:**
- `b26.1.0` - Beta, first release of 2026 (developed in late 2025)
- `26.1.1` - Stable, first release with patch 1
- `26.2.0` - Second major release of 2026
- `a27.1.0` - Alpha, first release of 2027

### How does `update-version.sh` work?

This script centralizes version management by automatically modifying the `version.properties` file, which is the **single source of truth** for the project version.

#### Usage:
```bash
./update-version.sh <new-version> [numeric-version]
```

#### Examples:

**Beta release (recommended):**
```bash
./update-version.sh b26.1.0
# Auto-detects numeric version: 26.1.0
```

**Beta with fix:**
```bash
./update-version.sh b26.1.1
# Auto-detects numeric version: 26.1.1
```

**Explicit numeric version:**
```bash
./update-version.sh b26.1.0 26.1.0
```

**Stable release:**
```bash
./update-version.sh 26.1.0
# Both versions set to: 26.1.0
```

**Alpha release:**
```bash
./update-version.sh a26.1.0
# Auto-detects numeric version: 26.1.0
```

**Release candidate:**
```bash
./update-version.sh rc26.1.0
# Auto-detects numeric version: 26.1.0
```

**Second release of the year:**
```bash
./update-version.sh b26.2.0
# Auto-detects numeric version: 26.2.0
```

#### What does the script do?

1. **Validates** that you provided a version as an argument
2. **Auto-detects** numeric version by stripping alphabetic prefixes
3. **Updates** `version.properties` with both versions:
   ```properties
   app.version=b1.0.3
   app.version.numeric=1.0.3
   ```
4. **Displays** a summary of what was updated:
   ```
   ✓ Version updated to b26.1.0 in version.properties
   ✓ Numeric version set to 26.1.0
   
   This change will affect:
     - AppConfig.java (runtime version display: b26.1.0)
     - build-installer.sh (installer version: 26.1.0)
   
   Rebuild your project to see changes:
     ./build-jar.sh && ./build-installer.sh
   ```

#### Why is this critical?

- **Dual-version support**: Maintains phase identifiers while satisfying jpackage constraints
- **`AppConfig.java`** reads `app.version` from `version.properties` for UI display
- **`build-installer.sh`** reads `app.version.numeric` for jpackage compatibility
- **Single source of truth**: No risk of version mismatches
- **Platform compatibility**: Works across macOS, Windows, and Linux

### Manual Update (alternative)

If you prefer not to use the script, you can edit `version.properties` directly:

```properties
# TCraft Client Version Configuration
app.version=b26.1.0
app.version.numeric=26.1.0
app.name=TCraft Client
app.vendor=ModInstallerCraft
```

**Important:** When manually editing, ensure:
- `app.version.numeric` contains only integers separated by dots
- No alphabetic characters or hyphens in the numeric version
- See [VERSION_GUIDE.md](VERSION_GUIDE.md) for valid formats

---

## Step 2: Compile the Code

Before building the JAR, make sure to compile your Java code.

### In IntelliJ IDEA:
1. Go to **Build → Build Project** (shift+F10 on macOS)
2. Compiled classes will be saved in `out/production/`

### From Terminal:
```bash
# If using javac manually
javac -d out/production src/**/*.java
```

---

## Step 3: JAR Build (Debug Distribution)

The `dist/` directory contains the executable JAR version of your application, ideal for **debugging and quick testing**.

### Running the Build:
```bash
./build-jar.sh
```

### What does `build-jar.sh` do?

1. **Reads** the display version from `version.properties`:
   ```bash
   APP_VERSION=$(grep '^app.version=' version.properties | cut -d'=' -f2)
   APP_NAME=$(grep '^app.name=' version.properties | cut -d'=' -f2)
   ```
   **Note:** Uses `app.version` (the full version with phase identifiers) for filenames and user-facing content.

2. **Creates** the `dist/` directory if it doesn't exist

3. **Copies** assets to the compilation directory:
   ```bash
   cp -r assets out/production/
   ```

4. **Copies** `version.properties` to include it in the JAR:
   ```bash
   cp version.properties out/production/
   ```
   **Important:** The entire `version.properties` file (including both `app.version` and `app.version.numeric`) is embedded in the JAR for runtime access by `AppConfig.java`.

5. **Generates** the executable JAR using the manifest:
   ```bash
   jar cvfm dist/TCraftClient.jar manifest.txt -C out/production .
   ```

6. **Creates** platform-specific launchers (`.command` for macOS, `.bat` for Windows, `.sh` for Linux)

7. **Generates** README.txt file with usage instructions

8. **Creates** a ZIP file with all `dist/` contents using the display version format:
   ```bash
   ${APP_NAME}-${APP_VERSION}.zip
   # Example: TCraft Client-b1.0.3.zip (includes phase identifier)
   ```

### Output:
```
dist/
├── TCraftClient.jar           # Executable JAR
├── TCraft Client.command      # Launcher for macOS
├── TCraft Client.bat          # Launcher for Windows
├── TCraft Client.sh           # Launcher for Linux
└── README.txt                 # Usage instructions

Project root/
└── TCraft Client-b26.1.0.zip   # Distribution ZIP (uses app.version)
```

### Testing the JAR:
```bash
# Option 1: Run directly
java -jar dist/TCraftClient.jar

# Option 2: Use your platform's launcher
# macOS
./dist/TCraft\ Client.command

# Linux
./dist/TCraft\ Client.sh

# Windows
dist\TCraft Client.bat
```

### When to use dist/?

- **Debugging**: Quick changes and testing
- **Development**: Feature testing
- **Quick distribution**: The generated ZIP is perfect for sharing with beta testers
- **Cross-platform**: The ZIP includes launchers for Windows, macOS and Linux
- **Not for production**: Use native installer for end users

---

## Step 4: Installer Build (Production Distribution)

The `installer/` directory contains the professional native installer for end-user distribution.

### Running the Build:
```bash
./build-installer.sh
```

### What does `build-installer.sh` do?

1. **Reads** both version properties from `version.properties`:
   ```bash
   APP_VERSION=$(grep '^app.version=' version.properties | cut -d'=' -f2)
   APP_VERSION_NUMERIC=$(grep '^app.version.numeric=' version.properties | cut -d'=' -f2)
   APP_NAME=$(grep '^app.name=' version.properties | cut -d'=' -f2)
   VENDOR=$(grep '^app.vendor=' version.properties | cut -d'=' -f2)
   ```
   **Note:** 
   - `APP_VERSION` is used for display messages and output filenames
   - `APP_VERSION_NUMERIC` is passed to jpackage (required for compatibility)
   - Automatic fallback to `APP_VERSION` if `APP_VERSION_NUMERIC` is not set

2. **Validates** that the JAR exists (if not, runs `build-jar.sh` automatically)

3. **Detects** your platform and prepares platform-specific arguments:
   - **macOS**: Generates `.dmg` with `.icns` icon
   - **Windows**: Generates `.exe` with `.ico` icon
   - **Linux**: Generates `.deb` with `.png` icon

4. **Runs jpackage** to create the native installer:
   ```bash
   jpackage \
     --input dist \
     --name "$APP_NAME" \
     --main-jar TCraftClient.jar \
     --main-class Main \
     --type dmg \
     --app-version "$APP_VERSION_NUMERIC" \    # Uses numeric version!
     --vendor "$VENDOR" \
     --icon assets/icon.icns \
     --dest installer
   ```
   **Critical:** The `--app-version` flag must receive a numeric-only version. This is why `app.version.numeric` exists.

5. **Renames** the installer to include the display version (with phase identifier):
   ```bash
   mv "installer/TCraft Client-26.1.0.dmg" "installer/TCraft Client-b26.1.0.dmg"
   ```
   **Note:** Installer filenames use the display version (`app.version`) for easy visual identification of beta/alpha releases.

### Output:
```
installer/
└── TCraft Client-b26.1.0.dmg    # Native installer (filename shows display version)
```

**Version in filename:** The installer filename uses the display version (`b26.1.0`) for easy visual identification, while the internal package metadata uses the numeric version (`26.1.0`) for OS compatibility.

### Installers by Platform:

| Platform | Format | Extension | Required Icon |
|----------|--------|-----------|---------------|
| macOS    | DMG    | `.dmg`    | `assets/icon.icns` |
| Windows  | EXE    | `.exe`    | `assets/icon.ico` |
| Linux    | Debian | `.deb`    | `assets/icon.png` |

### When to use installer/?

- **Production**: End-user distribution
- **Releases**: GitHub releases, official website
- **Professional**: Native installation with icons and system configuration

---

## Complete Release Workflow

### Example: Creating beta version b26.1.0 (December 2025 for 2026 release)

```bash
# 1. Update version (auto-detects numeric version)
./update-version.sh b26.1.0

# 2. Clean compile with assets
./compile-sources.sh

# 3. Create JAR for testing
./build-jar.sh

# 4. Test the JAR
java -jar dist/TCraftClient.jar

# 5. If everything works, you have:
#    - dist/TCraftClient.jar (for direct execution)
#    - TCraft Client-b26.1.0.zip (for distribution with phase identifier)

# 6. Optionally, create production installer
./build-installer.sh

# 7. Files ready for distribution:
ls -la *.zip installer/
# TCraft Client-b26.1.0.zip          <- Cross-platform ZIP (displays b26.1.0)
# installer/TCraft Client-b26.1.0.dmg <- Native installer (displays b26.1.0)
```

### Example: Creating stable version 26.1.0 (Production release)

```bash
# 1. Update version (both versions will be 26.1.0)
./update-version.sh 26.1.0

# 2. Clean compile
./compile-sources.sh

# 3-7. Same steps as above...

# Result files:
# TCraft Client-26.1.0.zip          <- Cross-platform ZIP
# installer/TCraft Client-26.1.0.dmg <- Native installer
```

### Example: Creating patch fix 26.1.1

```bash
# 1. Update version for bug fix
./update-version.sh 26.1.1

# 2. Clean compile and build
./compile-sources.sh
./build-jar.sh && ./build-installer.sh

# Result: TCraft Client-26.1.1.dmg (patch release)
```

### Quick Build (Complete Pipeline):
```bash
./update-version.sh b26.1.0 && \
./compile-sources.sh && \
./build-jar.sh && \
./build-installer.sh
```

This single command:
1. Updates the version
2. Compiles with clean output
3. Builds the JAR and ZIP
4. Creates the native installer

---

## Prerequisites

- **Java 14+** (You have Java 21)
- **Compiled code** in `out/production/`
- **macOS**: Xcode Command Line Tools
- **Icons** (optional but recommended):
  - `assets/icon.icns` - macOS (512x512px)
  - `assets/icon.ico` - Windows (256x256px)
  - `assets/icon.png` - Linux (512x512px)

---

## Comparison: dist/ vs installer/

| Feature | `dist/` (JAR) | `installer/` (Native) |
|---------|---------------|------------------------|
| **Build speed** | Fast (seconds) | Slow (minutes) |
| **Size** | Small (~MBs) | Large (~includes JRE) |
| **Installation** | Requires Java | Standalone, no Java required |
| **Icons** | Generic icon | Custom icon |
| **Use case** | Development/Debug | Production/Users |
| **Distribution** | Beta testers | General public |

---

## Troubleshooting

### Error: "version.properties not found"
```bash
# Verify the file exists
ls -la version.properties

# If it doesn't exist, create it:
cat > version.properties << EOF
app.version=26.1.0
app.version.numeric=26.1.0
app.name=TCraft Client
app.vendor=ModInstallerCraft
EOF
```

### Error: "Version contains invalid component"
This error occurs when jpackage receives a non-numeric version.

```bash
# Check your version.properties
cat version.properties

# Make sure app.version.numeric exists and contains only numbers
# Valid:   app.version.numeric=26.1.0
# Invalid: app.version.numeric=b26.1.0

# Fix by running:
./update-version.sh b26.1.0  # Will auto-detect numeric version

# Or manually edit version.properties to add:
# app.version.numeric=26.1.0
```

**See [VERSION_GUIDE.md](VERSION_GUIDE.md) for detailed versioning requirements.**

### Error: "command not found: jpackage"
```bash
# Check your Java version (you need 14+)
java --version

# Install a newer version if necessary
brew install openjdk@21
```

### JAR doesn't include assets or version.properties
```bash
# Verify JAR contents
jar tf dist/TCraftClient.jar | grep -E "(assets|version.properties)"

# You should see:
# version.properties
# assets/icon.png
# assets/font/minecraft-mojangles.ttf
# ...
```

### Version in app doesn't match
```bash
# Clean completely and rebuild
rm -rf out dist installer
# Compile in IntelliJ (Build → Rebuild Project)
./build-jar.sh && ./build-installer.sh
```

### Installer won't open on macOS (Security)
Users must:
1. **Right-click** the app → **Open**
2. Or go to **System Settings → Privacy & Security → Open Anyway**

To avoid this, sign the app with an Apple Developer certificate.

---

## Distribution

### ZIP Distribution (Cross-platform)
```bash
# 1. Create the JAR and ZIP
./build-jar.sh

# 2. The ZIP will be in the project root
ls -la *.zip
# TCraft Client-b26.1.0.zip (uses app.version with phase identifier)

# 3. Distribute the ZIP
# - GitHub Releases as additional file
# - Direct sending to beta testers
# - Download on your website
```

**ZIP Contents:**
- Executable JAR
- Launchers for Windows, macOS and Linux
- README with instructions

**Advantages:**
- No installation required
- Works on any platform with Java
- Easy to unzip and run
- Ideal for testing and development

**Versioning:** ZIP files use `app.version` (with phase identifiers like `b1.0.3`) for clarity in beta/alpha releases.

### GitHub Releases
```bash
# 1. Create the installer
./build-installer.sh

# 2. Upload to GitHub Releases
# - Go to your repository → Releases → Draft a new release
# - Tag: v26.1.0 (use numeric version for consistency)
# - Title: TCraft Client b26.1.0 (use display version)
# - Attach files:
#   - installer/TCraft Client-b26.1.0.dmg (native installer)
#   - TCraft Client-b26.1.0.zip (cross-platform JAR)
```

**Best Practice:** Use numeric version for Git tags (`v26.1.0`) but display version (`b26.1.0`) in release titles, descriptions, and filenames.

### Testing before distribution
```bash
# 1. Test the JAR
java -jar dist/TCraftClient.jar

# 2. Test the installer on a clean system
# - Install the application
# - Verify it opens correctly
# - Test all functionalities
```


