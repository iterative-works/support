# Review Packet: Phase 8 - Documentation for contributors

**Issue:** SUPP-9
**Phase:** 8 of 8
**Branch:** SUPP-9-phase-08
**Date:** 2026-01-30

## Goals

Enable contributors to use CI and git hooks effectively:
1. Document CI workflow behavior
2. Provide hook installation instructions
3. Include troubleshooting guide
4. Document local check commands

## Scenarios

- [x] CI workflow is documented (compile, format, lint, test)
- [x] Git hooks are documented (pre-commit, pre-push)
- [x] Hook installation commands are provided
- [x] Local check commands are documented
- [x] Troubleshooting section covers common issues
- [x] Bypass procedure is documented (discouraged but available)

## Entry Points

1. **CONTRIBUTING.md**: New contributor documentation file

## Structure

The documentation includes:

1. **CI Workflow** - Table showing all 4 CI jobs
2. **Git Hooks** - Installation commands and what they do
3. **Running Checks Locally** - All Mill commands for local validation
4. **Troubleshooting** - Common issues and fixes
5. **Code Style** - Scalafmt and Scalafix information

## Test Summary

**Manual verification:**
- All documented commands are valid Mill commands
- Hook installation symlink syntax is correct
- Troubleshooting covers the main error scenarios

## Files Changed

```
A CONTRIBUTING.md
```

### Key Sections in CONTRIBUTING.md

1. **CI Workflow** - Explains parallel vs sequential job execution
2. **Git Hooks** - Clear installation steps with symlink commands
3. **Running Checks Locally** - Complete list of Mill commands
4. **Troubleshooting** - Solutions for common issues:
   - Formatting failures
   - Test failures
   - Scalafix violations
5. **Bypassing Hooks** - Emergency procedures (discouraged)

## Review Checklist

- [ ] Documentation is clear and actionable
- [ ] Commands are accurate
- [ ] Symlink paths are correct
- [ ] Troubleshooting covers likely issues
- [ ] Code style section matches project config
