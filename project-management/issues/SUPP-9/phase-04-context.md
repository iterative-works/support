# Phase 04 Context: CI runs test suite on PR

**Issue:** SUPP-9
**Phase:** 4 - CI runs test suite on PR
**Created:** 2026-01-30

## Goals

Add test execution to the CI workflow so that PRs are validated not just for compilation, formatting, and linting, but also for functional correctness via the test suite.

## Scope

**In scope:**
- Add a `test` job to `.github/workflows/ci.yml`
- Run unit tests for all modules via Mill
- Handle integration tests appropriately (skip MongoDB tests that require external service)
- Ensure test failures are clearly visible in CI output

**Out of scope:**
- Setting up MongoDB or other external services in CI (deferred to issue #10)
- Pre-push hooks (Phase 7)
- Parallel job optimization (Phase 5)
- Test coverage reporting

## Dependencies from Previous Phases

**From Phase 1:**
- `.github/workflows/ci.yml` exists with compile job
- GitHub Actions infrastructure established (Temurin JDK 21, caching)

**From Phase 2:**
- Format job demonstrates parallel job pattern

**From Phase 3:**
- Lint job demonstrates parallel job pattern
- CI workflow structure supports multiple parallel jobs

## Technical Approach

1. **Add test job to CI workflow:**
   - New job named `test` that runs after `compile` succeeds
   - Use same infrastructure setup (Java 21, Mill, caching)
   - Run `./mill __.test` to execute all module tests

2. **Handle integration tests:**
   - MongoDB tests use `TestAspect.ifEnvSet("MONGO_URI")` - they skip automatically without the env var
   - SQL tests (Testcontainers) should work in CI
   - No special handling needed - tests self-skip when services unavailable

3. **Test output:**
   - Mill provides clear test output by default
   - Failed tests show assertion details
   - Test count and timing visible in output

## Files to Modify

- `.github/workflows/ci.yml` - Add test job with dependency on compile

## Testing Strategy

**Verification:**
1. Push workflow changes to a test branch
2. Create PR to trigger CI
3. Verify test job runs after compile completes
4. Verify test output is visible in CI logs
5. Verify MongoDB tests skip (no MONGO_URI)
6. Verify SQL tests run (Testcontainers available)

**Negative testing:**
1. Introduce a failing test locally
2. Push to verify CI catches the failure
3. Verify failure output is clear and actionable

## Acceptance Criteria

- [ ] `.github/workflows/ci.yml` includes a `test` job
- [ ] Test job depends on `compile` job (runs after compilation succeeds)
- [ ] Test job runs `./mill __.test` for all modules
- [ ] Test output is visible in GitHub Actions logs
- [ ] MongoDB integration tests skip gracefully (no MONGO_URI)
- [ ] Failed tests cause the job to fail with clear error output
- [ ] PR cannot merge if tests fail
