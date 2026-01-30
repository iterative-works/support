# Phase 06 Tasks: Pre-commit hook validates formatting locally

**Issue:** SUPP-9
**Phase:** 6 of 8

## Tasks

### Setup

- [x] [impl] Create `.git-hooks/` directory for hook scripts
- [x] [impl] Create executable `pre-commit` script

### Implementation

- [x] [impl] Detect staged `.scala` files using `git diff --cached --name-only`
- [x] [impl] Exit early if no Scala files are staged (success)
- [x] [impl] Run `./mill __.checkFormat` to validate formatting
- [x] [impl] Show clear error message with fix instructions when formatting fails
- [x] [impl] Exit with non-zero code on formatting failure

### Verification

- [x] [test] Hook script is executable (`chmod +x`)
- [x] [test] Install hook and test with well-formatted code (commit succeeds)
- [x] [test] Test with unformatted code (commit blocked with clear message)
- [x] [test] Verify `--no-verify` bypass works

### Acceptance Criteria

- [x] [verify] `.git-hooks/pre-commit` exists and is executable
- [x] [verify] Hook checks formatting of Scala files
- [x] [verify] Hook blocks commit when formatting issues found
- [x] [verify] Hook shows helpful error message
- [x] [verify] Hook passes when files are properly formatted

**Phase Status:** Complete
