# HopSync

A desktop app that keeps Minecraft modpacks in sync. Paste the link a server admin gives you, pick the game
folder (the official launcher, a Prism, CurseForge or Modrinth App instance, or any other), review what changes
and apply it. HopSync downloads, updates and removes only the files it installed; your own mods and the configs
you edited stay as they are.

## Features

- **Several packs**, each in its own folder; the same pack can live in several instances.
- **Review before applying**: new, updated and removed mods, configs and download size, in the pack header.
- **Safe updates**: parallel downloads checked by size and SHA-512, atomic moves, cancel at any time; the state
  on disk always matches what HopSync recorded, so retrying or continuing only fetches what is missing.
- **Mod list** with tabs (Pack, Tus mods, Configs), search, "Solo cambios" and expandable rows.
- **Instance detection** for the official launcher, Prism Launcher, CurseForge and Modrinth App.
- Windows, macOS and Linux. The interface is in Spanish.

## Modpack manifest

A pack is a `manifest.json` (format 2) published over https, usually as a GitHub release asset. See
[hopsync-demo](https://github.com/JJGarciaMartinez/hopsync-demo) for the schema and an example pack; its link is
the demo the add-pack wizard offers.

## Project structure

Three Gradle modules; dependencies only point inward (`desktop → platform → core`), enforced by ArchUnit tests.

```
core/        Domain: manifest model and parser, sync planner and executor, pack sessions, ports (no Swing, no HTTP)
platform/    Adapters: HTTP downloads, manifest fetching and cache, settings, game folder and instance detection
desktop/     Swing app (FlatLaf): store, reducer and effects, screens, theme kit, Main and packaging tasks
packaging/   Installer icons and the launchers of the portable ZIP
```

## Development

Java 21 and the Gradle wrapper (`gradlew.bat` on Windows).

```bash
./gradlew build      # compile, test and build desktop/build/libs/HopSync.jar
./gradlew test       # tests only
./gradlew run        # launch the app
```

Packaging and versioning are Gradle tasks too; see [BUILD_GUIDE.md](BUILD_GUIDE.md) and
[VERSION_GUIDE.md](VERSION_GUIDE.md). Branches and pull requests follow [BRANCHING.md](BRANCHING.md).

## Requirements

- **Installers** (`.msi`, `.dmg`, `.deb`): bundle their own Java; nothing else to install.
- **Portable ZIP**: Java 21 or newer ([adoptium.net](https://adoptium.net/)).
- An internet connection, and Minecraft installed with a mod loader for the pack.

Settings are kept in `%APPDATA%\HopSync`, `~/Library/Application Support/HopSync` or `~/.config/hopsync`.
