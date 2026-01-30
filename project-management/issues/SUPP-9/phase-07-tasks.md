# Phase 07 Tasks: Pre-push hook validates tests pass

**Issue:** SUPP-9
**Phase:** 7 of 8

## Tasks

### Setup

- [ ] [impl] Create executable `pre-push` script in `.git-hooks/`

### Implementation

- [ ] [impl] Run `./mill __.test` to execute all unit tests
- [ ] [impl] Show progress message while tests are running
- [ ] [impl] Show clear error message with fix instructions when tests fail
- [ ] [impl] Exit with non-zero code on test failure

### Verification

- [ ] [test] Hook script is executable
- [ ] [test] Test with passing tests (push succeeds)
- [ ] [test] Verify `--no-verify` bypass works

### Acceptance Criteria

- [ ] [verify] `.git-hooks/pre-push` exists and is executable
- [ ] [verify] Hook runs unit tests
- [ ] [verify] Hook blocks push when tests fail
- [ ] [verify] Hook shows helpful error message
- [ ] [verify] Hook passes when tests pass

**Phase Status:** Not Started
