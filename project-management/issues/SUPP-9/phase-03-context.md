# Phase 3 Context: Scalafix enforces FP principles

**Issue:** SUPP-9
**Phase:** 3 of 8
**Story:** Scalafix validates code quality

## Goals

Add automated Scalafix linting to the CI pipeline to enforce functional programming principles. This is particularly valuable with AI agents writing code, providing an automated safety net for FP best practices.

## Scope

### In Scope
- Add Scalafix Mill plugin to the build
- Configure `.scalafix.conf` with FP enforcement rules
- Add Scalafix check job to CI workflow (parallel with existing jobs)
- Configure rules: DisableSyntax, ExplicitResultTypes, NoValInForComprehension

### Out of Scope
- Auto-fix/rewrite capabilities (check-only for now)
- Pre-commit hook for Scalafix (will be added in later phase if needed)
- Custom Scalafix rules (use built-in and community rules only)

## Dependencies from Previous Phases

- **Phase 1:** CI workflow structure (`.github/workflows/ci.yml`) - add new job
- **Phase 2:** Caching strategy for Coursier/Mill - reuse for Scalafix job

## Technical Approach

### 1. Mill Scalafix Plugin

Add the Scalafix plugin to `build.mill`. Mill has built-in Scalafix support via `ScalafixModule`.

**Key considerations:**
- Scalafix works best with Scala 3.x (semantic rules may have limitations)
- Syntactic rules (DisableSyntax) work well with Scala 3
- Need to verify which rules are fully compatible with Scala 3.7

### 2. Scalafix Configuration

Create `.scalafix.conf` with these rules:

```hocon
rules = [
  DisableSyntax,
  ExplicitResultTypes,
  NoValInForComprehension
]

DisableSyntax {
  noNulls = true
  noVars = true
  noThrows = true
  noReturns = true
}
```

### 3. CI Integration

Add a `lint` job to the workflow:
- Run in parallel with `compile` and `format` jobs
- Use same caching strategy
- Command: `./mill __.fix --check` or equivalent

## Files to Modify

1. **`build.mill`** - Add ScalafixModule mixin to modules
2. **`.scalafix.conf`** - Create with rule configuration (new file)
3. **`.github/workflows/ci.yml`** - Add lint job

## Testing Strategy

### Positive Testing
1. Run Scalafix locally on existing codebase
2. Verify all existing code passes (or identify violations to fix)
3. Create test PR and verify CI job runs

### Negative Testing
1. Introduce a `null` in test code
2. Introduce a `var` in test code
3. Verify Scalafix catches violations with clear messages

### CI Validation
1. Verify lint job runs in parallel with compile/format
2. Verify caching works for lint job
3. Verify failures show clear error messages

## Acceptance Criteria

- [ ] `.scalafix.conf` exists with configured rules
- [ ] Mill build includes ScalafixModule mixin
- [ ] CI workflow includes lint job
- [ ] `DisableSyntax` catches null, var usage
- [ ] `ExplicitResultTypes` enforces explicit return types on public APIs (if compatible)
- [ ] `NoValInForComprehension` catches mutable vals in for-comps
- [ ] Scalafix violations cause CI to fail with clear error messages
- [ ] Existing codebase passes Scalafix checks (or known violations are addressed)

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Scalafix rules not fully compatible with Scala 3.7 | Medium | Test locally first, disable incompatible rules |
| Existing code has many violations | Medium | Run check locally first, fix violations before enabling CI |
| Semantic rules require SemanticDB | Low | Use syntactic rules only if SemanticDB setup is complex |

## Notes

- Mill has built-in Scalafix support, no external plugin needed
- Focus on syntactic rules first (DisableSyntax) as they're most reliable
- ExplicitResultTypes may need tuning to avoid noise on internal APIs
