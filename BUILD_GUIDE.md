# HopSync - Build and Distribution Guide

Everything runs through the Gradle wrapper (`./gradlew`, or `gradlew.bat` on Windows). The app name, vendor and
version come from `version.properties` (see [VERSION_GUIDE.md](VERSION_GUIDE.md)).

## Tasks

| Task | Output | What it does |
|------|--------|--------------|
| `./gradlew build` | `desktop/build/libs/HopSync.jar` | Compiles, runs every test and builds the self-contained JAR (FlatLaf and fonts inside). |
| `./gradlew run` | | Launches the app from the sources. |
| `./gradlew jpackage` | `desktop/build/installer/` | Native installer of the OS it runs on: `.msi` on Windows, `.dmg` on macOS, `.deb` on Linux. Bundles a Java runtime. |
| `./gradlew portableZip` | `desktop/build/distributions/HopSync-<version>-portable.zip` | The JAR plus `HopSync.bat`, `HopSync.sh`, `HopSync.command` and a README, for players who already have Java 21. |
| `./gradlew updateVersion -PnewVersion=<v> [-Pnumeric=<n>]` | `version.properties` | Sets `app.version` and `app.version.numeric`. |

## Installers (`jpackage`)

`jpackage` comes with the JDK 21 that Gradle uses to compile, and only builds installers for the OS it runs on,
so a release builds on Windows, macOS and Linux (the release workflow does it for you). What each one gets:

- **Common**: name and vendor `HopSync`, `--app-version` from `app.version.numeric`, `-Xmx1024m`,
  `-Dfile.encoding=UTF-8`, icons from `packaging/`.
- **Windows (`.msi`)**: `app.win.upgrade.uuid` (never change it: it lets a new installer replace the old one,
  including the TCraftClient installers), Start menu entry, desktop shortcut and folder chooser.
  Needs the [WiX Toolset](https://wixtoolset.org/) on the PATH when building locally.
- **macOS (`.dmg`)**: identifier `app.hopsync.client`. Unsigned: the first time, players open it with
  right-click → Open, or System Settings → Privacy & Security → Open Anyway. If it does not open on another Mac,
  `./scripts/bash/diagnose-app.sh` inspects the built `.dmg`.
- **Linux (`.deb`)**: package `hopsync`, with a menu entry. Needs `dpkg-deb` and `fakeroot`.

Running `jpackage` again replaces the previous installer.

## Releasing

1. Merge `develop` into `main` (see [BRANCHING.md](BRANCHING.md)) with the new version already set:
   `./gradlew updateVersion -PnewVersion=26.2.0`, commit `chore: release 26.2.0`.
2. Tag the commit with exactly `app.version` and push the tag:
   ```bash
   git tag 26.2.0
   git push origin 26.2.0
   ```
3. The **Release** workflow (`.github/workflows/release.yml`) checks that the tag matches `app.version`, builds
   and tests on the three OSes, runs `jpackage` on each and `portableZip` on Linux, and drafts a GitHub Release
   with the `.msi`, `.dmg`, `.deb` and the portable ZIP. `a`, `b` and `rc` tags are marked as prereleases.
4. Review the draft on GitHub (notes, files) and publish it.

## Before publishing

- Install the new version over the previous one and check that packs, settings and installed mods are kept.
- Open the app on a clean system (no settings) to see the first-run wizard.

## Troubleshooting

- **"app.version.numeric=…: jpackage needs one to three numbers"**: fix it with `updateVersion`; the numeric
  version cannot carry the `a`/`b`/`rc` prefix.
- **Windows: jpackage cannot find WiX**: install WiX and open a new terminal so the PATH is reloaded.
- **The version shown in the app is old**: `./gradlew clean build`.
