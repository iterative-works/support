# Phase 01 Tasks: Basic CI workflow validates compilation on PR

**Issue:** SUPP-9
**Phase:** 1 of 8
**Context:** phase-01-context.md

## Implementation Tasks

### Setup

- [x] [impl] Create `.github/workflows/` directory structure

### Implementation

- [x] [impl] Create `ci.yml` with workflow name and trigger configuration (on PR to main)
- [x] [impl] Add checkout step using `actions/checkout@v4`
- [x] [impl] Add Java 21 setup using `actions/setup-java@v4` with Temurin distribution
- [x] [impl] Add Coursier cache using `actions/cache@v4` for `~/.cache/coursier`
- [x] [impl] Add Mill cache using `actions/cache@v4` for `~/.mill`
- [x] [impl] Add compilation step running `./mill __.compile` (Mill wrapper handles installation)

### Verification

- [x] [verify] Validate YAML syntax locally
- [ ] [verify] Push branch and create test PR to verify workflow runs
- [ ] [verify] Confirm all modules compile successfully in CI
- [ ] [verify] Verify caches are populated and used on subsequent runs

## Notes

- Mill wrapper `./mill` exists in repo root - handles Mill download/installation automatically
- Compilation takes ~67 seconds locally (warm cache)
- First CI run will be slower due to dependency downloads
- Cache keys use hash of `build.mill` and `.mill-version` to invalidate appropriately
