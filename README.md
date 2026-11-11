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
src/main/java/hopsync/
├── Main.java          # Application entry point
├── config/            # App configuration (version, app.properties, asset paths)
├── event/             # EventBus and the sealed UpdateEvent hierarchy
├── model/             # Data models (ModInfo)
├── service/           # Business logic: manifest, downloads, cleanup, update coordination
├── state/             # Thread-safe update state
├── ui/                # Swing components and the event handler that updates them
└── util/              # Font/image loading, JSON parsing
src/main/resources/    # assets/ (images, font) and app.properties
src/test/java/         # JUnit 5 tests
packaging/             # Installer icons used by jpackage
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

The project uses Gradle (wrapper included) and Java 21.

```bash
./gradlew build   # compile, run tests and build build/libs/TCraftClient.jar
./gradlew test    # run tests only
./gradlew run     # launch the app
```

On Windows use `gradlew.bat`. IntelliJ IDEA imports the project directly from `build.gradle.kts`.

### Building JAR

**Mac/Linux:**
```bash
./scripts/bash/build-jar.sh
```

**Windows (PowerShell):**
```powershell
.\scripts\powershell\build-jar.ps1
```

This runs the Gradle build (including tests) and creates `dist/TCraftClient.jar` plus platform launchers.

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

- **Java 21 or higher** (only for the JAR distribution; the installers bundle their own runtime) (Download from [adoptium.net](https://adoptium.net/))
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
If you see version 21 or higher, you're good to go!

### For Developers

- Java Development Kit (JDK) 21+
- IntelliJ IDEA or any Java IDE (Gradle import)
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

