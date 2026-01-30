# Phase 3 Tasks: Scalafix enforces FP principles

**Issue:** SUPP-9
**Phase:** 3 of 8
**Context:** `phase-03-context.md`

## Tasks

### Setup

- [x] [test] Research Mill ScalafixModule API for Scala 3.7 compatibility
- [x] [impl] Create `.scalafix.conf` with FP enforcement rules

### Build Integration

- [x] [impl] Add ScalafixModule mixin to BaseModule in `build.mill`
- [x] [test] Verify Scalafix runs locally with `./mill __.fix --check`
- [x] [impl] Fix any existing violations in codebase (if found)

### CI Integration

- [x] [impl] Add lint job to `.github/workflows/ci.yml`
- [x] [test] Verify lint job runs in parallel with compile/format

### Validation

- [x] [test] Test negative case: introduce `null` and verify Scalafix catches it
- [x] [test] Test negative case: introduce `var` and verify Scalafix catches it
- [x] [impl] [x] [reviewed] Clean up test violations and verify final CI state

## Acceptance Criteria

From `phase-03-context.md`:

- [x] `.scalafix.conf` exists with configured rules
- [x] Mill build includes ScalafixModule mixin
- [x] CI workflow includes lint job
- [x] `DisableSyntax` catches null, var usage
- [x] Scalafix violations cause CI to fail with clear error messages
- [x] Existing codebase passes Scalafix checks

## Notes

- Mill 1.1.0-RC2 has built-in ScalafixModule support
- Focus on syntactic rules (DisableSyntax) - most reliable with Scala 3
- May need to adjust ExplicitResultTypes if it causes too much noise

**Phase Status:** Complete
