# Versioning System - TCraft Client

## Technical Overview

This document describes the dual-version scheme implemented to resolve jpackage compatibility constraints while maintaining a year-based versioning system with phase identifiers.

## Versioning Scheme

TCraft Client uses a **year-based versioning scheme** with the following format:

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
| `a26.1.0` | Alpha - first release of 2026 |
| `b26.1.0` | Beta - first release of 2026 |
| `b26.1.1` | Beta - first release of 2026, patch 1 |
| `rc26.1.0` | Release Candidate - first release of 2026 |
| `26.1.0` | Stable - first release of 2026 |
| `26.1.1` | Stable - first release of 2026, patch 1 |
| `26.2.0` | Stable - second release of 2026 |
| `b27.1.0` | Beta - first release of 2027 |

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

## jpackage Version Constraint

The `jpackage` tool enforces strict version format validation on macOS platforms:

**Constraint:** Version strings must consist of one to three dot-separated integer components.

**Valid formats:**
- `1.0.2`
- `2.3.4.5`

**Invalid formats:**
- `b1.0.2` (alphanumeric prefix)
- `v1.0.2` (non-numeric character)
- `1.0.2-beta` (hyphenated suffix)

**Error message:**
```
Bundler Mac DMG Package skipped because of a configuration problem: 
"Version [b1.0.2] contains invalid component [b1]"
```

## Implementation Architecture

### Dual-Version Configuration

The system employs two distinct version properties in `version.properties`:

```properties
app.version=b1.0.2          # Display version (user-facing)
app.version.numeric=1.0.2   # Package version (jpackage-compatible)
```

### Property Definitions

| Property | Purpose | Format | Consumer |
|----------|---------|--------|----------|
| `app.version` | User-facing version identifier | Semantic version with optional phase prefix | `AppConfig.java`, UI components |
| `app.version.numeric` | Package metadata version | Numeric dot-notation only | `jpackage`, installer metadata |

### Build Pipeline Integration

The `build-installer.sh` script implements the following logic:

1. Parse both version properties from `version.properties`
2. Validate `app.version` existence (mandatory)
3. Apply fallback: if `app.version.numeric` is undefined, use `app.version`
4. Pass `app.version.numeric` to jpackage via `--app-version` flag
5. Use `app.version` for user-visible build messages

### Advantages

- **Platform Compatibility:** Ensures jpackage compliance across macOS, Windows, and Linux
- **Semantic Versioning:** Preserves phase identifiers (alpha, beta, rc) in application UI
- **Backward Compatibility:** Automatic fallback mechanism for missing numeric version
- **Automation-Ready:** Script-based version management with auto-detection

## Version Management

### Automated Update (Recommended)

The `update-version.sh` script provides automated version management with intelligent parsing:

```bash
# Explicit numeric version specification
./update-version.sh b1.0.3 1.0.3

# Automatic numeric extraction (strips alphabetic prefix)
./update-version.sh b1.0.3

# Pure numeric version (sets both properties identically)
./update-version.sh 1.0.3
```

**Auto-detection algorithm:**
- If `NEW_VERSION` starts with alphabetic characters, strip prefix to derive numeric version
- Example: `b1.0.3` → `1.0.3`, `rc2.1.0` → `2.1.0`

### Manual Configuration

Direct modification of `version.properties`:

```properties
app.version=b1.0.3
app.version.numeric=1.0.3
```

## Versioning Schemes

### Alpha Releases (Early Development)
```properties
app.version=a26.1.0
app.version.numeric=26.1.0
```
**Use case:** Unstable, experimental features, internal testing

### Beta Releases (Testing Phase)
```properties
app.version=b26.1.0
app.version.numeric=26.1.0
```
**Use case:** Feature-complete, public testing, bug fixing

### Release Candidates (Pre-Release)
```properties
app.version=rc26.1.0
app.version.numeric=26.1.0
```
**Use case:** Final testing before stable release, no new features

### Stable Releases (Production)
```properties
app.version=26.1.0
app.version.numeric=26.1.0
```
**Use case:** Production-ready, public release

### Patch/Fix Releases
```properties
app.version=26.1.1
app.version.numeric=26.1.1
```
**Use case:** Bug fixes, minor corrections to stable release

### Multiple Fixes
```properties
app.version=26.1.2
app.version.numeric=26.1.2
```
**Progression:** `26.1.0` → `26.1.1` → `26.1.2` → `26.1.3` ...

## Build Process

### Complete Rebuild
```bash
./build-jar.sh && ./build-installer.sh
```

### Installer-Only Build
```bash
# Use when JAR artifact is unchanged
./build-installer.sh
```

## Technical Implementation Details

### Version Property Resolution

**build-installer.sh:**
```bash
APP_VERSION=$(grep '^app.version=' version.properties | cut -d'=' -f2)
APP_VERSION_NUMERIC=$(grep '^app.version.numeric=' version.properties | cut -d'=' -f2)

# Fallback mechanism
if [ -z "$APP_VERSION_NUMERIC" ]; then
    APP_VERSION_NUMERIC="$APP_VERSION"
fi
```

**jpackage invocation:**
```bash
jpackage --app-version $APP_VERSION_NUMERIC ...
```

### Application Runtime Behavior

- `AppConfig.java` loads `app.version` for UI display
- Window titles, about dialogs, and logs display full version with phase identifiers
- Installer metadata uses numeric version for system compatibility
- **Installer filenames use display version** (`app.version`) for easy visual identification

## Usage Examples

### Beta Release for 2026 (Current Development)
```bash
./update-version.sh b26.1.0
```
**Result:**
- Application UI: `TCraft Client b26.1.0`
- Installer filename: `TCraft Client-b26.1.0.dmg`
- Package metadata: `26.1.0`

**Context:** Developing in December 2025 for early 2026 release

### First Patch/Fix
```bash
./update-version.sh b26.1.1
```
**Result:**
- Application UI: `TCraft Client b26.1.1`
- Installer filename: `TCraft Client-b26.1.1.dmg`
- Package metadata: `26.1.1`

### Second Major Release of 2026
```bash
./update-version.sh b26.2.0
```
**Result:**
- Application UI: `TCraft Client b26.2.0`
- Installer filename: `TCraft Client-b26.2.0.dmg`
- Package metadata: `26.2.0`

### Stable Release (Production)
```bash
./update-version.sh 26.1.0
```
**Result:**
- Application UI: `TCraft Client 26.1.0`
- Installer filename: `TCraft Client-26.1.0.dmg`
- Package metadata: `26.1.0`

### Alpha Release
```bash
./update-version.sh a26.1.0
```
**Result:**
- Application UI: `TCraft Client a26.1.0`
- Installer filename: `TCraft Client-a26.1.0.dmg`
- Package metadata: `26.1.0`

### Release Candidate
```bash
./update-version.sh rc26.1.0
```
**Result:**
- Application UI: `TCraft Client rc26.1.0`
- Installer filename: `TCraft Client-rc26.1.0.dmg`
- Package metadata: `26.1.0`

## Version Progression Example

Typical development cycle for 2026 first release:

```
a26.1.0  → Alpha (early development)
  ↓
b26.1.0  → Beta (testing phase)
  ↓
b26.1.1  → Beta fix 1
  ↓
rc26.1.0 → Release Candidate
  ↓
26.1.0   → Stable Release
  ↓
26.1.1   → Patch fix 1
  ↓
26.1.2   → Patch fix 2
  ↓
26.2.0   → Second major release of 2026
```

## Validation

Verify configuration:
```bash
# Display current versions
grep 'app.version' version.properties

# Test build pipeline
./build-installer.sh

# Confirm installer artifact
ls -lh installer/
```

