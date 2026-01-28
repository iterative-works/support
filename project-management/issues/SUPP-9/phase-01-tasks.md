# Phase 01 Tasks: Basic CI workflow validates compilation on PR

**Issue:** SUPP-9
**Phase:** 1 of 8
**Context:** phase-01-context.md

## Implementation Tasks

### Setup

- [ ] [impl] Create `.github/workflows/` directory structure

### Implementation

- [ ] [impl] Create `ci.yml` with workflow name and trigger configuration (on PR to main)
- [ ] [impl] Add checkout step using `actions/checkout@v4`
- [ ] [impl] Add Java 21 setup using `actions/setup-java@v4` with Temurin distribution
- [ ] [impl] Add Coursier cache using `actions/cache@v4` for `~/.cache/coursier`
- [ ] [impl] Add Mill cache using `actions/cache@v4` for `~/.mill`
- [ ] [impl] Add Mill installation step using coursier (`cs install mill`)
- [ ] [impl] Add compilation step running `./mill __.compile`

### Verification

- [ ] [verify] Validate YAML syntax locally
- [ ] [verify] Push branch and create test PR to verify workflow runs
- [ ] [verify] Confirm all modules compile successfully in CI
- [ ] [verify] Verify caches are populated and used on subsequent runs

## Notes

- Mill wrapper `./mill` exists in repo root - use that instead of global `mill` command
- Compilation takes ~67 seconds locally (warm cache)
- First CI run will be slower due to dependency downloads
- Cache key should include hash of `build.mill` to invalidate on build changes
