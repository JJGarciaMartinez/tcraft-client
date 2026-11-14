# Versioning System - HopSync

## Technical Overview

This document describes the dual-version scheme implemented to resolve jpackage compatibility constraints while maintaining a year-based versioning system with phase identifiers.

## Versioning Scheme

HopSync uses a **year-based versioning scheme** with the following format:

### Version Format: `YY.MAJOR.FIXES`

- **YY** (Year): Last two digits of the planned release year
- **MAJOR**: Version number within that year (starts at 1, never 0)
- **FIXES**: Patch/fix number for minor corrections (0 for initial release)

### Examples:
- `26.1.0` - First release of 2026, no fixes
- `26.1.1` - First release of 2026, first fix
- `26.2.0` - Second release of 2026
- `27.1.0` - First release of 2027

### Phase Identifiers (Alpha, Beta, RC)

Pre-release versions use a single-letter prefix:

| Prefix | Meaning | Example | Description |
|--------|---------|---------|-------------|
| `a` | Alpha | `a26.1.0` | Early development, unstable |
| `b` | Beta | `b26.1.0` | Feature-complete, testing phase |
| `rc` | Release Candidate | `rc26.1.0` | Pre-release, final testing |
| *(none)* | Stable | `26.1.0` | Production-ready release |

### Third Component: Iterations vs Fixes

The third component (`FIXES`) has different meanings depending on the phase:

| Phase | Third Component Meaning | Example |
|-------|------------------------|---------|
| Alpha/Beta/RC | **Iteration number** | `b26.1.4` = 4th beta iteration |
| Stable | **Fix/patch number** | `26.1.4` = 4th fix of stable release |

**Key distinction:**
- `b26.1.4` = Fourth **beta iteration** of version 26.1
- `26.1.4` = Fourth **bug fix** of stable release 26.1

**Development progression:**
```
b26.1.1 → b26.1.2 → b26.1.3 → b26.1.4  (beta iterations)
                                   ↓
                                26.1.0  (stable release, resets to 0)
                                   ↓
                       26.1.1 → 26.1.2  (bug fixes)
```

The phase prefix (`b`, `a`, `rc`, or none) distinguishes iterations from fixes.

### Important: Year vs Development Timeline

The year component (**YY**) represents the **planned release year**, not the current development year.

**Example:**
- Current date: December 2025
- Target release: Early 2026
- Version: `b26.1.0` (beta for 2026 release)

This allows development in late 2025 for a 2026 release while maintaining clear version semantics.

### Quick Reference

| Version | Meaning |
|---------|---------|
| `a26.1.0` | Alpha - 1st iteration for version 26.1 |
| `a26.1.1` | Alpha - 2nd iteration for version 26.1 |
| `b26.1.0` | Beta - 1st iteration for version 26.1 |
| `b26.1.4` | Beta - 5th iteration for version 26.1 |
| `rc26.1.0` | Release Candidate - 1st RC for version 26.1 |
| `26.1.0` | **Stable release** of version 26.1 |
| `26.1.1` | Stable - 1st bug fix for version 26.1 |
| `26.1.2` | Stable - 2nd bug fix for version 26.1 |
| `26.2.0` | **Stable release** of version 26.2 |
| `b27.1.0` | Beta - 1st iteration for version 27.1 |

**Progression example:**
```
December 2025: a26.1.0 (alpha development)
       ↓
January 2026:  b26.1.0 (beta testing)
       ↓
February 2026: rc26.1.0 (release candidate)
       ↓
March 2026:    26.1.0 (stable release)
       ↓
April 2026:    26.1.1 (bug fix)
       ↓
June 2026:     26.2.0 (second major release)
```

## Two versions in `version.properties`

`jpackage` only accepts one to three numbers (`26.1.4`; `b26.1.4` fails with
`Version [b26.1.4] contains invalid component [b26]`), so the file keeps two:

| Property | Used for | Example |
|----------|----------|---------|
| `app.version` | What the app shows (status bar, About), the JAR and ZIP names, the release tag | `b26.1.4` |
| `app.version.numeric` | `jpackage --app-version`: installer metadata and upgrades | `26.1.4` |

`a`, `b` and `rc` versions show "(test build)" next to the version in the app. `app.name`, `app.vendor` and
`app.win.upgrade.uuid` live in the same file; the UUID never changes.

Gradle reads the file when it configures the build and copies it into the JAR, where `AppVersion` reads it at
runtime.

## Changing the version

```bash
./gradlew updateVersion -PnewVersion=b26.2.0              # numeric 26.2.0, taken from the display version
./gradlew updateVersion -PnewVersion=rc26.2.0 -Pnumeric=26.2.0
./gradlew updateVersion -PnewVersion=26.2.0               # stable: both the same
```

The task only rewrites `app.version` and `app.version.numeric`, and refuses a numeric version that `jpackage`
would reject. Editing the file by hand works too.

## Releases

A release is a tag equal to `app.version` (`b26.2.0`, `26.2.0`) pushed to GitHub; the Release workflow builds
the installers and drafts the GitHub Release. See [BUILD_GUIDE.md](BUILD_GUIDE.md#releasing).
