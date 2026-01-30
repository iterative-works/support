# Phase 07 Context: Pre-push hook validates tests pass

**Issue:** SUPP-9
**Phase:** 7 of 8
**Story:** Pre-push hook validates tests pass

## Goals

Prevent pushing broken code to the remote repository:
1. Create a pre-push hook that runs unit tests before allowing push
2. Block push if tests fail
3. Provide clear output showing test progress and failures

## Scope

**In scope:**
- Create `.git-hooks/pre-push` shell script
- Run unit tests via Mill (`./mill __.test`)
- Block push if any tests fail
- Show clear error message with fix instructions
- Exit early if there are no commits to push

**Out of scope:**
- Integration tests in hook (would be too slow)
- Running tests only for changed modules (complex, premature optimization)
- Windows support (Linux/macOS focus)

## Dependencies

**From previous phases:**
- Phase 4: Tests run in CI and pass
- Phase 6: Pre-commit hook pattern established

**Decision context from analysis.md:**
- Unit tests only in pre-push hook (integration tests in CI)
- Simple shell scripts in `.git-hooks/` directory

## Technical Approach

1. Create `.git-hooks/pre-push` script following Phase 6 pattern
2. Script should:
   - Check if there are commits to push (exit early if none)
   - Run `./mill __.test` for all modules
   - Exit with non-zero code if tests fail
   - Show helpful error message with bypass instructions
3. Pattern: Same structure as pre-commit hook

## Files to Create/Modify

- `.git-hooks/pre-push` - New: pre-push hook script

## Testing Strategy

1. Install hook: `ln -sf ../../.git-hooks/pre-push .git/hooks/pre-push`
2. Test with passing tests: push should succeed
3. Introduce a failing test and try to push: should be blocked
4. Verify `--no-verify` bypass works

## Acceptance Criteria

- [ ] `.git-hooks/pre-push` script exists and is executable
- [ ] Hook runs unit tests before allowing push
- [ ] Hook blocks push when tests fail
- [ ] Hook shows clear error message with fix instructions
- [ ] Hook passes when all tests pass
- [ ] Hook can be bypassed with `git push --no-verify`
