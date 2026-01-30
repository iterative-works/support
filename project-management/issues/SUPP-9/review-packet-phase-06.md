# Review Packet: Phase 6 - Pre-commit hook validates formatting locally

**Issue:** SUPP-9
**Phase:** 6 of 8
**Branch:** SUPP-9-phase-06
**Date:** 2026-01-30

## Goals

Provide fast local feedback on code formatting before commits:
1. Create pre-commit hook that validates Scala formatting
2. Block commits with unformatted code
3. Show clear error message with fix instructions

## Scenarios

- [x] Hook script exists and is executable
- [x] Hook detects staged Scala files
- [x] Hook exits early (success) when no Scala files staged
- [x] Hook runs Scalafmt check
- [x] Hook blocks commit on formatting failure with clear message
- [x] Hook can be bypassed with `--no-verify`

## Entry Points

1. **Pre-commit hook**: `.git-hooks/pre-commit` - Main hook script

## Diagrams

### Hook Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    git commit                               │
└─────────────────────────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                  pre-commit hook                            │
└─────────────────────────────────────────────────────────────┘
                             │
                             ▼
                ┌────────────────────────┐
                │ Any staged .scala files?│
                └────────────────────────┘
                     │            │
                    No           Yes
                     │            │
                     ▼            ▼
              ┌──────────┐  ┌────────────────┐
              │ exit 0   │  │ mill checkFormat│
              │ (pass)   │  └────────────────┘
              └──────────┘        │
                           ┌──────┴──────┐
                         Pass           Fail
                           │              │
                           ▼              ▼
                    ┌──────────┐   ┌────────────────┐
                    │ exit 0   │   │ Show error     │
                    │ (pass)   │   │ exit 1 (block) │
                    └──────────┘   └────────────────┘
```

## Test Summary

**Manual verification performed:**
- Script syntax validated with `bash -n`
- Hook passes when no files staged
- Hook passes when only non-Scala files staged
- Hook is executable (755 permissions)

**Note:** This project uses git worktrees, so hook installation is slightly different. The hook script in `.git-hooks/` can be symlinked from the main repo's `.git/hooks/` directory. Documentation for this will be added in Phase 8.

## Files Changed

```
A .git-hooks/pre-commit
```

### Key Implementation Details

**Pre-commit hook** (`.git-hooks/pre-commit`):
- Uses `set -euo pipefail` for strict error handling
- Detects staged Scala files with `git diff --cached --name-only --diff-filter=ACM`
- Exits early (success) if no Scala files are staged
- Runs `./mill __.checkFormat` to validate formatting
- Shows colored error box with fix instructions on failure
- Exits with non-zero code to block the commit

**Error message includes:**
- Clear "COMMIT BLOCKED" header
- Fix command: `./mill __.reformat`
- Bypass option: `git commit --no-verify` (discouraged)

## Review Checklist

- [ ] Script uses proper error handling (`set -euo pipefail`)
- [ ] Script correctly detects staged Scala files
- [ ] Error message is clear and helpful
- [ ] Script is portable (uses `/usr/bin/env bash`)
- [ ] No security issues in the script
