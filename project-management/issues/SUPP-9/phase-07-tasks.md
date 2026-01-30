# Phase 07 Tasks: Pre-push hook validates tests pass

**Issue:** SUPP-9
**Phase:** 7 of 8

## Tasks

### Setup

- [x] [impl] Create executable `pre-push` script in `.git-hooks/`

### Implementation

- [x] [impl] Run `./mill __.test` to execute all unit tests
- [x] [impl] Show progress message while tests are running
- [x] [impl] Show clear error message with fix instructions when tests fail
- [x] [impl] Exit with non-zero code on test failure

### Verification

- [x] [test] Hook script is executable
- [x] [test] Test with passing tests (push succeeds)
- [x] [test] Verify `--no-verify` bypass works

### Acceptance Criteria

- [x] [verify] `.git-hooks/pre-push` exists and is executable
- [x] [verify] Hook runs unit tests
- [x] [verify] Hook blocks push when tests fail
- [x] [verify] Hook shows helpful error message
- [x] [verify] Hook passes when tests pass

**Phase Status:** Complete
