# Phase 3 Tasks: Scalafix enforces FP principles

**Issue:** SUPP-9
**Phase:** 3 of 8
**Context:** `phase-03-context.md`

## Tasks

### Setup

- [ ] [test] Research Mill ScalafixModule API for Scala 3.7 compatibility
- [ ] [impl] Create `.scalafix.conf` with FP enforcement rules

### Build Integration

- [ ] [impl] Add ScalafixModule mixin to BaseModule in `build.mill`
- [ ] [test] Verify Scalafix runs locally with `./mill __.fix --check`
- [ ] [impl] Fix any existing violations in codebase (if found)

### CI Integration

- [ ] [impl] Add lint job to `.github/workflows/ci.yml`
- [ ] [test] Verify lint job runs in parallel with compile/format

### Validation

- [ ] [test] Test negative case: introduce `null` and verify Scalafix catches it
- [ ] [test] Test negative case: introduce `var` and verify Scalafix catches it
- [ ] [impl] Clean up test violations and verify final CI state

## Acceptance Criteria

From `phase-03-context.md`:

- [ ] `.scalafix.conf` exists with configured rules
- [ ] Mill build includes ScalafixModule mixin
- [ ] CI workflow includes lint job
- [ ] `DisableSyntax` catches null, var usage
- [ ] Scalafix violations cause CI to fail with clear error messages
- [ ] Existing codebase passes Scalafix checks

## Notes

- Mill 1.1.0-RC2 has built-in ScalafixModule support
- Focus on syntactic rules (DisableSyntax) - most reliable with Scala 3
- May need to adjust ExplicitResultTypes if it causes too much noise
