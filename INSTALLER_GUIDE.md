# TCraft Client - Installer Build Guide

This guide explains how to build installers for TCraft Client.

## Quick Start

### 1. Build the JAR
```bash
./build-jar.sh
```

This creates `dist/TCraftClient.jar` - a standalone executable JAR file.

### 2. Build the Native Installer
```bash
./build-installer.sh
```

This automatically detects your platform and creates:
- **macOS**: `.dmg` installer
- **Linux**: `.deb` package
- **Windows**: `.exe` installer

## Prerequisites

- Java 14 or higher (you have Java 21 ✓)
- Compiled classes in `out/production/`
- For macOS: Xcode Command Line Tools

## Build Process

### Step 1: Compile Your Code
If you're using IntelliJ IDEA, just build the project (Build → Build Project).
The compiled classes will be in `out/production/`.

### Step 2: Run the Build Scripts
```bash
# Build JAR only
./build-jar.sh

# Build complete installer
./build-installer.sh
```

## Output Structure

```
tcraft-client/
├── dist/
│   └── TCraftClient.jar          # Executable JAR
└── installer/
    └── TCraft Client-1.0.0.dmg   # Native installer (platform-specific)
```

## Testing Your Build

### Test the JAR
```bash
java -jar dist/TCraftClient.jar
```

### Test the Installer
- **macOS**: Double-click the `.dmg` file, drag the app to Applications
- **Windows**: Run the `.exe` installer
- **Linux**: Install with `sudo dpkg -i tcraft-client_*.deb`

## Customization

Edit `build-installer.sh` to customize:
- `APP_VERSION`: Change version number
- `VENDOR`: Change vendor name
- Icon paths and names
- Installer options

## Icons

The installer supports custom icons but they are **optional**. The script will automatically detect and use icons if they exist:

- `assets/icon.icns` - macOS (ICNS format)
- `assets/icon.ico` - Windows (ICO format)  
- `assets/icon.png` - Linux (PNG format, 512x512px recommended)

If icons are not found, the installer will build successfully using the default Java application icon.

**To create custom icons**, see the [ICON_GUIDE.md](ICON_GUIDE.md) for detailed instructions.

## Troubleshooting

### "command not found: jpackage"
You need Java 14+. Check with: `java --version`

### Assets not included
Make sure `assets/` folder exists and contains your resources.

### Installer won't open on macOS
You may need to sign the app with an Apple Developer certificate, or users can:
1. Right-click the app → Open
2. System Settings → Privacy & Security → Open Anyway

## Distribution

After building:
1. Test the installer on a clean system
2. Upload to your GitHub releases page
3. Provide installation instructions for users

## CI/CD Integration

You can automate builds using GitHub Actions to create installers for all platforms. Example workflow available in `.github/workflows/` (create if needed).
