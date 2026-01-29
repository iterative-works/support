# Review Packet: Phase 02 - CI validates code formatting

**Issue:** SUPP-9
**Phase:** 2 of 8
**Branch:** SUPP-9

## Goals

This phase adds a formatting validation job to the CI workflow that ensures all code follows the Scalafmt configuration.

**Outcomes:**
1. CI workflow includes a formatting check job
2. Job runs `mill __.checkFormat` command
3. Unformatted code causes CI to fail
4. Formatting errors show which files need formatting
5. Formatting check runs quickly (parallel with compilation)

## User Story

> As a team, I want CI to verify code follows our formatting standards, so that code style stays consistent across the codebase

## Verification Scenarios

### Scenario: PR with properly formatted code passes formatting check

- [ ] Create a PR with code changes
- [ ] All code is formatted according to `.scalafmt.conf`
- [ ] GitHub Actions CI runs
- [ ] The formatting check job succeeds
- [ ] PR shows a green check for formatting

### Scenario: PR with unformatted code fails formatting check

- [ ] Create a PR with code changes
- [ ] Some code is not formatted according to `.scalafmt.conf`
- [ ] GitHub Actions CI runs
- [ ] The formatting check job fails
- [ ] CI logs show which files need formatting
- [ ] PR shows a red X for formatting

## Entry Points

| File | Purpose |
|------|---------|
| `.github/workflows/ci.yml` | CI workflow defining compile and format jobs |

## Architecture Diagram

```
                    GitHub Actions CI
                    (on: pull_request)
                           │
              ┌────────────┴────────────┐
              │                         │
              ▼                         ▼
      ┌───────────────┐       ┌───────────────────┐
      │    compile    │       │      format       │
      │               │       │                   │
      │ ./mill        │       │ ./mill            │
      │   __.compile  │       │   __.checkFormat  │
      └───────────────┘       └───────────────────┘
              │                         │
              └────────────┬────────────┘
                           │
                           ▼
                    PR Status Check
```

Both jobs run **in parallel** (no `needs:` dependency between them).

## Files Changed

### Modified Files

| File | Change Summary |
|------|----------------|
| `.github/workflows/ci.yml` | Added `format` job with checkFormat command |

### Change Details

**`.github/workflows/ci.yml`** (lines 49-85)

Added new `format` job:
- **Name:** "Check Formatting"
- **Runner:** `ubuntu-latest`
- **Permissions:** `contents: read`, `packages: read` (for GitHub Packages access)
- **Steps:**
  1. Checkout code (`actions/checkout@v4`)
  2. Set up Java 21 (`actions/setup-java@v4`)
  3. Cache Coursier dependencies (`actions/cache@v4`)
  4. Cache Mill (`actions/cache@v4`)
  5. Run `./mill __.checkFormat` with Coursier credentials

Key implementation details:
- Job runs **independently** of `compile` job (parallel execution)
- Uses identical caching strategy as `compile` job
- Same GitHub Packages authentication pattern for dependency resolution

## Implementation Summary

The implementation adds a `format` job that mirrors the structure of the existing `compile` job but runs the scalafmt check command instead. The job:

1. **Runs in parallel** - No `needs:` directive, so it starts immediately alongside the compile job
2. **Uses same infrastructure** - Same Java version, same caching, same authentication
3. **Checks all modules** - The `__.checkFormat` target runs against all modules in the build
4. **Fails on violations** - Mill's checkFormat returns non-zero when files are unformatted

## Review Checklist

- [ ] Format job appears in workflow YAML
- [ ] Format job has no `needs:` dependency on compile (runs in parallel)
- [ ] Job uses correct Mill command (`./mill __.checkFormat`)
- [ ] Job has same caching configuration as compile job
- [ ] Job has proper permissions for GitHub Packages access
- [ ] PURPOSE comments at top of file are accurate
