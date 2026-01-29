# Phase 02: CI validates code formatting

**Issue:** SUPP-9
**Phase:** 2 of 8
**Story:** CI validates code formatting

## Implementation Tasks

### Setup
- [ ] [impl] Review existing CI workflow structure in `.github/workflows/ci.yml`
- [ ] [impl] Verify `__.checkFormat` command works locally

### Implementation
- [ ] [impl] Add `format` job to CI workflow that runs in parallel with `compile`
- [ ] [impl] Configure format job with same cache settings as compile job
- [ ] [impl] Add `./mill __.checkFormat` step to format job

### Verification
- [ ] [verify] Verify format job syntax is correct (YAML lint)
- [ ] [verify] Push to test branch and confirm format job runs
- [ ] [verify] Confirm format job runs in parallel with compile job
- [ ] [verify] Introduce formatting error and confirm CI fails

## Acceptance Criteria

1. CI workflow includes format check job
2. Job runs Mill scalafmt check command (`__.checkFormat`)
3. Unformatted code causes CI to fail
4. Formatting errors show which files need formatting
5. Formatting check runs in parallel with compilation (no dependency)
6. PR status check reflects formatting result

## Notes

- Mill command: `./mill __.checkFormat`
- Format job should use same caching strategy as compile job
- No dependency on compile job - can run in parallel
- Job needs same permissions and GitHub Package credentials as compile job
