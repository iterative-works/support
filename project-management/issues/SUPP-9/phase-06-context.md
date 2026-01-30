# Phase 06 Context: Pre-commit hook validates formatting locally

**Issue:** SUPP-9
**Phase:** 6 of 8
**Story:** Pre-commit hook validates formatting locally

## Goals

Provide fast local feedback on code formatting before commits reach the remote repository:
1. Create a pre-commit hook that validates Scala formatting
2. Hook should check only staged files (fast feedback)
3. Hook should block commits with unformatted code
4. Hook installation should be documented

## Scope

**In scope:**
- Create `.git-hooks/pre-commit` shell script
- Check formatting of staged `.scala` files only
- Use Mill's Scalafmt check command
- Provide clear error messages when formatting issues are found
- Document hook installation in README or CONTRIBUTING

**Out of scope:**
- Pre-commit framework (lefthook, husky) - using simple shell scripts per decision
- Automatic formatting on commit (user should format explicitly)
- Windows support (Linux/macOS focus)
- Hook auto-installation (manual setup, future iw-cli integration)

## Dependencies

**From previous phases:**
- Phase 2: Scalafmt is configured and working (`.scalafmt.conf`)
- Phase 5: CI workflow validates formatting on PRs

**Decision context from analysis.md:**
- Simple shell scripts in `.git-hooks/` directory (no external dependencies)
- Future: iw-cli will provide command to install hooks across projects

## Technical Approach

1. Create `.git-hooks/` directory for hook scripts
2. Create `pre-commit` script that:
   - Gets list of staged `.scala` files
   - Runs Scalafmt check on those files
   - Exits with non-zero code if formatting issues found
   - Provides helpful error message with fix instructions
3. Add installation instructions to documentation (Phase 8)

## Files to Create/Modify

- `.git-hooks/pre-commit` - New: pre-commit hook script
- (Documentation will be added in Phase 8)

## Testing Strategy

1. Install hook locally: `ln -sf ../../.git-hooks/pre-commit .git/hooks/pre-commit`
2. Test with well-formatted code: commit should succeed
3. Test with unformatted code: commit should be blocked with clear message
4. Verify hook only checks staged files (not entire codebase)
5. Verify hook runs quickly (< 10 seconds for typical changes)

## Acceptance Criteria

- [ ] `.git-hooks/pre-commit` script exists and is executable
- [ ] Hook checks formatting of staged Scala files only
- [ ] Hook blocks commit when formatting issues are found
- [ ] Hook shows clear error message with fix instructions
- [ ] Hook passes when all staged files are properly formatted
- [ ] Hook can be bypassed with `--no-verify` (for emergencies)
