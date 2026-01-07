# Branching Strategy - TCraft Client

## Overview

This project uses a simplified Git Flow strategy optimized for individual development while maintaining release stability.

## Branch Structure

### Main Branches

| Branch | Purpose | Rules |
|--------|---------|-------|
| `main` | **Stable releases only**. Every commit here should be release-ready. | Tags created here |
| `develop` | **Active development**. Integration branch for features and fixes. | Daily work |

### Working Branches (Temporary)

| Type | Format | Example | When to use |
|------|--------|---------|-------------|
| Feature | `feature/short-name` | `feature/dark-mode` | New functionality |
| Fix | `fix/description` | `fix/download-timeout` | Bug fixes |
| Refactor | `refactor/area` | `refactor/services` | Code improvements |

## Workflow

### Visual Flow

```
feature/new-ui ────┐
                   │
fix/bug-download ──┼──► develop ──► main
                   │        │         │
refactor/cleanup ──┘        │         │
                            ▼         ▼
                       (testing)   (release)
                                      │
                                      ▼
                                   tag: b26.2.0
```

### Typical Development Cycle

#### 1. Starting a New Feature

```bash
# Create feature branch from develop
git checkout develop
git pull origin develop
git checkout -b feature/my-feature

# Work on the feature
# ... make commits ...

# When done, merge back to develop
git checkout develop
git merge feature/my-feature
git branch -d feature/my-feature
git push origin develop
```

#### 2. Bug Fix (Non-urgent)

```bash
# Same as feature, but use fix/ prefix
git checkout -b fix/download-error develop
# ... fix the bug ...
git checkout develop
git merge fix/download-error
git branch -d fix/download-error
```

#### 3. Urgent Hotfix (Production Bug)

```bash
# Create fix directly from main
git checkout main
git checkout -b fix/critical-bug

# Fix and merge to main
git checkout main
git merge fix/critical-bug
git tag b26.1.5  # Patch version

# Also merge to develop
git checkout develop
git merge fix/critical-bug
git branch -d fix/critical-bug
```

#### 4. Creating a Release

```bash
# When develop is stable and ready
git checkout main
git merge develop
git tag b26.2.0
git push origin main --tags

# Continue development on develop
git checkout develop
# Update version.properties for next version
```

## Version Tags

Tags follow the format defined in `VERSION_GUIDE.md`:

| Tag | Phase | Meaning |
|-----|-------|---------|
| `a26.1.0` | Alpha | Early development, unstable |
| `b26.1.0` | Beta | Feature-complete, testing |
| `rc26.1.0` | Release Candidate | Final testing |
| `26.1.0` | Stable | Production-ready |

**Important:** Tags are only created on `main` branch.

## Branch Naming Conventions

### Do's

- `feature/user-authentication`
- `fix/null-pointer-download`
- `refactor/service-layer`

### Don'ts

- `my-branch` (no type prefix)
- `feature/FixBugAndAddFeature` (mixed concerns)
- `Feature/dark-mode` (wrong capitalization)

## Quick Reference

### Daily Commands

```bash
# Start work
git checkout develop
git pull origin develop

# Create feature branch
git checkout -b feature/my-work

# Save progress
git add .
git commit -m "feat: description"

# Finish feature
git checkout develop
git merge feature/my-work
git branch -d feature/my-work
git push origin develop
```

### Release Commands

```bash
# Prepare release
git checkout main
git merge develop
git tag b26.2.0
git push origin main --tags
```

## Commit Message Convention

Use conventional commits for clarity:

| Prefix | Use for |
|--------|---------|
| `feat:` | New features |
| `fix:` | Bug fixes |
| `refactor:` | Code refactoring |
| `docs:` | Documentation |
| `chore:` | Maintenance tasks |
| `style:` | Formatting changes |

Example: `feat: add dark mode toggle to settings`