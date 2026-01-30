# Phase 04 Tasks: CI runs test suite on PR

**Issue:** SUPP-9
**Phase:** 4 - CI runs test suite on PR
**Created:** 2026-01-30

## Tasks

### Setup
- [x] [impl] Verify existing tests pass locally with `./mill __.test`

### Implementation
- [x] [impl] Add `test` job to `.github/workflows/ci.yml`
- [x] [impl] Configure test job to depend on compile job
- [x] [impl] Add test execution step with `./mill __.test`

### Validation
- [x] [impl] Verify CI workflow YAML syntax is valid
- [ ] [impl] Create PR to trigger CI and verify test job runs
- [ ] [impl] Verify MongoDB tests skip gracefully (no MONGO_URI in CI)
- [ ] [impl] Verify test output is visible in GitHub Actions logs

## Notes

- Test job follows same infrastructure pattern as compile/format/lint jobs
- MongoDB tests auto-skip via `TestAspect.ifEnvSet("MONGO_URI")`
- SQL tests should work (Testcontainers available in GitHub Actions)
- Test job depends on compile to avoid wasted resources if compilation fails
