# TCraft Client - Minecraft Mod Updater

A desktop application for managing and updating Minecraft mods for the TCraft modded server. Built with Java Swing, this tool provides a user-friendly interface to keep your mods synchronized with the server’s mod list.

> **Note:** This project started as a personal tool, but it is designed to grow into a collaborative, community-driven project over time.

## Features

- **Automatic Mod Detection**: Detects your Minecraft installation and mods folder across Windows, macOS, and Linux
- **Mod Synchronization**: Downloads and updates mods from a remote manifest
- **Config File Management**: Automatically downloads and manages mod configuration files ✨ **NEW**
- **Visual Interface**: Clean, Minecraft-themed UI with real-time status updates
- **Progress Tracking**: Visual feedback for download and update operations
- **System Information**: Displays OS, Java version, and mod installation details
- **Non-destructive Updates**: Preserves existing configuration files while adding new ones

## Project Structure

The application follows a modular architecture organized into logical packages:

```
src/
├── Main.java                    # Application entry point
├── config/                      # Configuration and constants
│   ├── AppConfig.java          # App version and manifest URL
│   └── AssetPaths.java         # Asset resource paths
├── model/                       # Data models
│   └── ModInfo.java            # Mod metadata record
├── service/                     # Business logic layer
│   ├── DownloadService.java    # HTTP download functionality
│   ├── FileSystemService.java  # File system operations
│   ├── ModUpdater.java         # Mod update orchestration
│   └── SystemInfoService.java  # System information gathering
├── ui/                          # User interface components
│   ├── LauncherUI.java         # Main window frame
│   ├── UIController.java       # UI event handling and coordination
│   ├── HeaderPanel.java        # Top section with system info
│   ├── ModListPanel.java       # Center panel with mod cards
│   ├── ControlPanel.java       # Bottom panel with action buttons
│   ├── ModCard.java            # Individual mod display component
│   └── [supporting UI classes]
└── util/                        # Utility classes
    ├── FontLoader.java         # Custom font loading
    ├── JsonParser.java         # JSON parsing
    └── LogMessageParser.java   # Log message formatting
```

## How It Works

1. **Initialization**: The application detects the Minecraft installation directory based on the operating system
2. **Manifest Retrieval**: Downloads the mod list (JSON format) from a remote GitHub repository
3. **Comparison**: Compares local mods with the remote manifest to identify outdated or missing mods
4. **Cleanup**: Removes mods that are no longer in the server's mod list
5. **Download**: Fetches new or updated mods from the URLs specified in the manifest
6. **Status Updates**: Provides real-time visual feedback through the UI during all operations

The application uses a **Model-Service-UI** architecture:
- **Model**: Data structures (ModInfo)
- **Service**: Business logic (file operations, downloads, updates)
- **UI**: Swing-based graphical interface with event-driven updates

## Technical Details

- **Language**: Java (modern syntax with records and enhanced features)
- **UI Framework**: Java Swing with custom theming
- **Architecture**: MVC-inspired separation of concerns
- **Concurrency**: Background threading for download operations
- **Cross-platform**: Supports Windows, macOS, and Linux

## Development Setup

### Running from IntelliJ IDEA

After compiling in IntelliJ, you need to copy assets to the output directory:

**Mac/Linux:**
```bash
./scripts/bash/copy-assets.sh
```

**Windows (PowerShell):**
```powershell
.\scripts\powershell\copy-assets.ps1
```

Then run the `Main` class normally from IntelliJ.

**Why?** IntelliJ doesn't maintain the `assets/` folder structure when copying resources. The script ensures assets are in the correct location.

For automatic execution, add the appropriate script as a "Before launch" task in Run Configuration.

### Building JAR

**Mac/Linux:**
```bash
./scripts/bash/build-jar.sh
```

**Windows (PowerShell):**
```powershell
.\scripts\powershell\build-jar.ps1
```

This creates `dist/TCraftClient.jar` with all dependencies and resources correctly packaged.

### Creating Installer

**Mac/Linux:**
```bash
./scripts/bash/build-installer.sh
```

**Windows (PowerShell):**
```powershell
.\scripts\powershell\build-installer.ps1
```

Creates platform-specific installers:
- **macOS**: `.dmg` installer
- **Windows**: `.msi` installer (WiX Toolset - installed automatically if not present)
- **Linux**: `.deb` package

> **Note for Windows**: The script automatically installs WiX Toolset if it's not already available on your system. This is required to create MSI installers.

### All Available Scripts

| Task | Mac/Linux | Windows PowerShell |
|------|-----------|-------------------|
| Copy assets | `./scripts/bash/copy-assets.sh` | `.\scripts\powershell\copy-assets.ps1` |
| Compile sources | `./scripts/bash/compile-sources.sh` | `.\scripts\powershell\compile-sources.ps1` |
| Build JAR | `./scripts/bash/build-jar.sh` | `.\scripts\powershell\build-jar.ps1` |
| Build installer | `./scripts/bash/build-installer.sh` | `.\scripts\powershell\build-installer.ps1` |
| Verify JAR | `./scripts/bash/verify-jar.sh` | `.\scripts\powershell\verify-jar.ps1` |
| Update version | `./scripts/bash/update-version.sh` | `.\scripts\powershell\update-version.ps1` |
| Diagnose app | `./scripts/bash/diagnose-app.sh` | _(Not needed on Windows)_ |

### Documentation

- **[BUILD_GUIDE.md](BUILD_GUIDE.md)**: Complete build and distribution guide
- **[VERSION_GUIDE.md](VERSION_GUIDE.md)**: Versioning system explained

## System Requirements

### For End Users (Non-Technical)

- **Java 16 or higher** (Download from [adoptium.net](https://adoptium.net/))
- **Supported Operating Systems:**
  - Windows 7, 8, 10, 11
  - macOS 10.13 (High Sierra) or later
  - Linux (any modern distribution)
- **Internet connection** (for downloading mods)
- **Minecraft installed** (the app auto-detects your installation)
- **~100 MB free disk space**

**Check if you have Java:**
```bash
java -version
```
If you see version 16 or higher, you're good to go!

### For Developers

- Java Development Kit (JDK) 16+
- IntelliJ IDEA or any Java IDE
- **Platform-specific requirements:**
  - **macOS**: Xcode Command Line Tools (for building installers)
  - **Windows**: WiX Toolset (installed automatically by build-installer.ps1)
  - **Linux**: Standard build tools

## Configuration

The mod manifest URL is configured in `AppConfig.java`:
```java
public static final String URL_MANIFEST = 
    "https://raw.githubusercontent.com/JJGarciaMartinez/tcraft-mods-list/main/modList/mod-list.json";
```

## Mod Repository Structure

The mod files and manifest are hosted in a separate GitHub repository: [`tcraft-mods-list`](https://github.com/JJGarciaMartinez/tcraft-mods-list)

**Repository Layout:**
```
tcraft-mods-list/
└── modList/
    ├── mod-list.json       # Manifest file with mod metadata
    └── mods/               # Directory containing all mod .jar files
        ├── fabric-api-0.128.2+1.21.6.jar
        ├── cloth-config-19.0.147-fabric.jar
        ├── jade-1.21.8-Fabric-19.3.2.jar
        └── [other mod files...]
```

**Manifest Format (mod-list.json):**
```json
{
  "mod_pack_name": "Example Mod Pack",
  "mod_pack_version": "1.0.0",
  "mods": [
    {
      "name": "fabric-api.jar",
      "version": "0.128.2+1.21.6",
      "description": "Fabric API is a core library for Minecraft Fabric mods.",
      "author": "Fabric Team",
      "url": "https://raw.github.com/.../fabric-api-0.128.2+1.21.6.jar",
      "configUrl": "https://example.com/configs/fabric-api.toml",
      "configName": "fabric-api.toml"
    }
  ]
}
```

Each mod entry in the manifest contains:
- **name**: Display name of the mod file
- **version**: Mod version number
- **description**: Brief description of the mod's functionality
- **author**: Mod creator or team
- **url**: Direct download link to the mod .jar file
- **configUrl** (optional): URL to the mod's configuration file ✨ **NEW**
- **configName** (optional): Name of the configuration file ✨ **NEW**

> **Note**: The `configUrl` and `configName` fields are optional. If present, the config file will be downloaded to the `.minecraft/config` folder without removing existing files.

## Future Plans

This project aims to become a community-driven tool for Minecraft modded server management, with potential features including:
- Multi-server support
- Mod profile management
- Custom mod source configuration
- Community contributions and plugin system

