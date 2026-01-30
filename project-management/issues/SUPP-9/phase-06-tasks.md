# Phase 06 Tasks: Pre-commit hook validates formatting locally

**Issue:** SUPP-9
**Phase:** 6 of 8

## Tasks

### Setup

- [ ] [impl] Create `.git-hooks/` directory for hook scripts
- [ ] [impl] Create executable `pre-commit` script

### Implementation

- [ ] [impl] Detect staged `.scala` files using `git diff --cached --name-only`
- [ ] [impl] Exit early if no Scala files are staged (success)
- [ ] [impl] Run `./mill __.checkFormat` to validate formatting
- [ ] [impl] Show clear error message with fix instructions when formatting fails
- [ ] [impl] Exit with non-zero code on formatting failure

### Verification

- [ ] [test] Hook script is executable (`chmod +x`)
- [ ] [test] Install hook and test with well-formatted code (commit succeeds)
- [ ] [test] Test with unformatted code (commit blocked with clear message)
- [ ] [test] Verify `--no-verify` bypass works

### Acceptance Criteria

- [ ] [verify] `.git-hooks/pre-commit` exists and is executable
- [ ] [verify] Hook checks formatting of Scala files
- [ ] [verify] Hook blocks commit when formatting issues found
- [ ] [verify] Hook shows helpful error message
- [ ] [verify] Hook passes when files are properly formatted

**Phase Status:** Not Started
