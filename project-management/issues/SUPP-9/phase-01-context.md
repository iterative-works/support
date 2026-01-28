# Phase 01: Basic CI workflow validates compilation on PR

**Issue:** SUPP-9
**Phase:** 1 of 8
**Story:** As a contributor, I want CI to automatically verify my PR compiles, so that I know my changes don't break the build

## Goals

This phase establishes the foundational CI workflow that validates code compilation for all modules (JVM and JS cross-compilation) on pull requests.

**Outcomes:**
1. GitHub Actions workflow file exists at `.github/workflows/ci.yml`
2. Workflow triggers on pull requests to main branch
3. Compilation succeeds for all JVM modules
4. Compilation succeeds for all JS modules
5. Coursier and Mill dependencies are cached between runs
6. Failed compilation shows clear error messages in CI logs

## Scope

### In Scope
- Create `.github/workflows/ci.yml` workflow file
- Configure workflow to trigger on PRs to main
- Set up Java 21 and Mill installation
- Configure Coursier cache for dependencies
- Configure Mill cache for build artifacts
- Run `mill __.compile` to compile all modules
- Handle multi-module cross-compilation (JVM + JS)

### Out of Scope
- Test execution (Phase 02)
- Formatting checks (Phase 02)
- Scalafix linting (Phase 03)
- Parallel job optimization (Phase 05)
- Git hooks (Phase 06-07)
- Documentation (Phase 08)

## Dependencies

### From Previous Phases
None - this is the first phase.

### External Dependencies
- GitHub repository with Actions enabled
- Mill build system (already configured in build.mill)
- Java 21 (matches local development environment)

## Technical Approach

### Workflow Structure

Create a minimal, focused workflow that:
1. Uses `ubuntu-latest` runner
2. Checks out the repository
3. Sets up Java 21 using `actions/setup-java@v4`
4. Caches Coursier dependencies (~/.cache/coursier)
5. Caches Mill artifacts (~/.mill)
6. Installs Mill using coursier
7. Runs `mill __.compile` to compile all modules

### Caching Strategy

Conservative caching (dependencies only) as decided in analysis:
- **Coursier cache**: `~/.cache/coursier` - Contains downloaded dependencies
- **Mill cache**: `~/.mill` - Contains Mill installation and wrapper cache

Cache keys will use a hash of `build.mill` to invalidate when build definition changes.

### Mill Commands

The project uses Mill with the following relevant commands:
- `mill __.compile` - Compile all modules (JVM + JS)
- Individual module compilation available via `mill <module>.compile`

The `root` module aggregates all publishable modules, but `__.compile` targets all modules including tests and scenarios.

### Error Handling

- Mill returns non-zero exit code on compilation failure
- GitHub Actions will fail the workflow on non-zero exit
- Compilation errors are printed to stdout/stderr and captured in CI logs

## Files to Modify

### New Files
- `.github/workflows/ci.yml` - Main CI workflow file

### Existing Files
None modified in this phase.

## Testing Strategy

### Manual Verification
1. Push the workflow to a branch
2. Create a test PR against main
3. Verify workflow triggers and runs
4. Verify all JVM modules compile successfully
5. Verify all JS modules compile successfully
6. Verify caches are populated on first run
7. Run workflow again to verify cache hits
8. Introduce a compilation error and verify failure is reported clearly

### Verification Checklist
- [ ] Workflow appears in GitHub Actions tab
- [ ] Workflow triggers on PR creation
- [ ] Java 21 is set up correctly
- [ ] Coursier cache is used
- [ ] Mill cache is used
- [ ] All JVM modules compile
- [ ] All JS modules compile
- [ ] Compilation failure shows clear error
- [ ] PR shows green/red status check

## Acceptance Criteria

From the user story (Story 1 in analysis.md):

```gherkin
Scenario: PR with valid code passes compilation check
  Given I create a PR with code changes
  When GitHub Actions CI runs
  Then the compilation job succeeds for all JVM modules
  And the compilation job succeeds for all JS modules
  And I see a green check on my PR

Scenario: PR with compilation errors fails CI
  Given I create a PR with code that doesn't compile
  When GitHub Actions CI runs
  Then the compilation job fails
  And I see the compilation error in the CI logs
  And the PR shows a red X
```

**Specific Acceptance:**
1. `.github/workflows/ci.yml` exists and is valid YAML
2. Workflow runs on PR creation/update to main
3. Workflow compiles all JVM modules successfully
4. Workflow compiles all JS modules successfully
5. Coursier dependencies are cached between runs
6. Failed compilation shows clear error messages in CI logs
7. PR status check reflects compilation result

## Notes

- This is a minimal first phase - optimization comes in Phase 05
- Integration tests (mongo-it, files-mongo-it) won't be affected as they're only test modules
- The `verify.compile()` task could be used, but `mill __.compile` is more direct
- ScalaJS modules require additional compilation time but use the same Mill command
