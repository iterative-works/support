# Phase 08 Context: Documentation for contributors

**Issue:** SUPP-9
**Phase:** 8 of 8
**Story:** Documentation for contributors

## Goals

Enable contributors to use CI and git hooks effectively:
1. Document CI workflow behavior (what runs, when)
2. Provide step-by-step hook installation instructions
3. Include troubleshooting guide for common issues
4. Document how to run checks locally before pushing

## Scope

**In scope:**
- Add section to README.md or create CONTRIBUTING.md
- Document CI jobs (compile, format, lint, test)
- Document git hooks (pre-commit, pre-push)
- Provide hook installation commands
- Include troubleshooting for common issues
- Explain hook bypass procedure for emergencies

**Out of scope:**
- API documentation (separate concern)
- Architecture documentation (separate concern)
- Detailed Mill build system documentation

## Dependencies

**From previous phases:**
- Phase 1-5: CI workflow complete
- Phase 6: Pre-commit hook for formatting
- Phase 7: Pre-push hook for tests

## Technical Approach

1. Check if CONTRIBUTING.md exists, otherwise add section to README.md
2. Document:
   - CI workflow overview
   - Hook installation (symlink commands)
   - Running checks locally
   - Troubleshooting common issues
   - Bypass procedure (discouraged but documented)

## Files to Create/Modify

- `CONTRIBUTING.md` - New: contributor documentation
  (or `README.md` if simpler)

## Testing Strategy

1. Follow the documentation as a new contributor would
2. Verify all commands work as documented
3. Verify hook installation steps are correct
4. Ensure troubleshooting covers common scenarios

## Acceptance Criteria

- [ ] Documentation explains CI workflow (4 jobs)
- [ ] Documentation includes hook installation steps
- [ ] Documentation shows how to run checks locally
- [ ] Documentation includes troubleshooting section
- [ ] Documentation explains hook bypass (for emergencies)
- [ ] Commands in documentation work when followed literally
