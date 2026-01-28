# Story-Driven Analysis: Set up CI and git hooks for quality assurance

**Issue:** SUPP-9
**Created:** 2026-01-28
**Status:** Draft
**Classification:** Feature

## Problem Statement

The project currently lacks automated quality gates, which means code quality issues can reach the repository and production. Contributors have no automated feedback before pushing code, and PRs cannot be verified automatically.

**Current pain points:**
- No verification that code compiles across all modules (JVM + JS cross-compilation)
- No automated test execution before merging
- No formatting verification (despite Scalafmt being configured)
- Contributors can push broken code to remote
- PR reviewers cannot see test results or formatting issues automatically

**Value delivered:**
Implementing CI and git hooks will provide fast, automated feedback on code quality, catch issues early in the development cycle, reduce review burden, and ensure consistent code quality standards across the multi-module codebase.

## User Stories

### Story 1: Basic CI workflow validates compilation on PR

```gherkin
Feature: CI validates code compilation
  As a contributor
  I want CI to automatically verify my PR compiles
  So that I know my changes don't break the build

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

**Estimated Effort:** 3-4h
**Complexity:** Straightforward

**Technical Feasibility:**
This is straightforward because we already have a working SBT build system with cross-compilation. The main work is creating the GitHub Actions workflow YAML and configuring caching for SBT dependencies.

**Key technical challenges:**
- Setting up proper caching for SBT and Coursier to avoid slow CI runs
- Ensuring both JVM and JS modules compile in the workflow
- Handling the multi-module structure correctly

**Acceptance:**
- `.github/workflows/ci.yml` exists and runs on PR creation
- Workflow compiles all JVM modules successfully
- Workflow compiles all JS modules successfully
- SBT dependencies are cached between runs
- Failed compilation shows clear error messages in CI logs

---

### Story 2: CI runs test suite on PR

```gherkin
Feature: CI validates tests pass
  As a contributor
  I want CI to automatically run tests on my PR
  So that I know my changes don't break existing functionality

Scenario: PR with passing tests succeeds CI
  Given I create a PR with code changes
  And all tests pass locally
  When GitHub Actions CI runs
  Then the test job runs all unit tests
  And all tests pass
  And I see test results in the CI logs

Scenario: PR with failing tests fails CI
  Given I create a PR with code changes
  And some tests fail
  When GitHub Actions CI runs
  Then the test job reports the failures
  And I see which tests failed in the CI logs
  And the PR shows a red X
```

**Estimated Effort:** 2-3h
**Complexity:** Straightforward

**Technical Feasibility:**
Straightforward because we already have tests in the project (found `*Spec.scala` files). We just need to add a test execution step to the workflow. The main consideration is test execution time.

**Key technical challenges:**
- Understanding full test suite runtime (currently unknown)
- Handling integration tests that might need external services (mongo-support-it, files-mongo-it modules exist)
- Deciding whether to run all tests or only unit tests in basic CI

**Acceptance:**
- CI workflow includes a test job that runs after compilation
- Test job executes `sbt test` successfully
- Failed tests show clear error messages
- Test results are visible in CI logs
- Integration tests are either excluded or properly handled

---

### Story 3: CI validates code formatting

```gherkin
Feature: CI validates code formatting
  As a team
  I want CI to verify code follows our formatting standards
  So that code style stays consistent across the codebase

Scenario: PR with properly formatted code passes formatting check
  Given I create a PR with code changes
  And all code is formatted according to .scalafmt.conf
  When GitHub Actions CI runs
  Then the formatting check job succeeds
  And I see a green check for formatting

Scenario: PR with unformatted code fails formatting check
  Given I create a PR with code changes
  And some code is not formatted according to .scalafmt.conf
  When GitHub Actions CI runs
  Then the formatting check job fails
  And I see which files need formatting in the CI logs
  And the PR shows a red X for formatting
```

**Estimated Effort:** 1-2h
**Complexity:** Straightforward

**Technical Feasibility:**
Very straightforward because Scalafmt 3.7.17 is already configured (`.scalafmt.conf` exists). We just need to run `sbt scalafmtCheckAll` in the workflow.

**Acceptance:**
- CI workflow includes formatting check job
- Job runs `sbt scalafmtCheckAll` (or equivalent)
- Unformatted code causes CI to fail
- Formatting errors show which files need formatting
- Formatting check runs quickly (can be parallel with other jobs)

---

### Story 4: Pre-commit hook validates formatting locally

```gherkin
Feature: Pre-commit hook validates formatting
  As a contributor
  I want my code to be checked for formatting before committing
  So that I catch formatting issues before they reach the remote

Scenario: Committing well-formatted code succeeds
  Given I have staged code changes
  And all staged files are properly formatted
  When I run git commit
  Then the pre-commit hook runs
  And the hook validates formatting
  And the commit succeeds

Scenario: Committing unformatted code is blocked
  Given I have staged code changes
  And some staged files are not formatted according to .scalafmt.conf
  When I run git commit
  Then the pre-commit hook runs
  And the hook detects formatting issues
  And the hook shows which files need formatting
  And the commit is blocked
```

**Estimated Effort:** 4-6h
**Complexity:** Moderate

**Technical Feasibility:**
Moderate complexity because we need to choose a hook implementation approach and ensure it works reliably across different contributor environments.

**Key technical challenges:**
- Choosing hook implementation: shell script, sbt-git-hooks, pre-commit framework, or lefthook
- Making the hook work across different environments (Linux, macOS, potentially Windows)
- Ensuring fast feedback (should only check staged files, not entire codebase)
- Making hook installation easy for contributors
- Balancing thoroughness vs speed (full compilation would be too slow)

**Acceptance:**
- Pre-commit hook is committed to repository
- Hook checks formatting of staged Scala files
- Hook provides clear error messages when formatting issues are found
- Hook runs quickly (< 10 seconds for typical changes)
- Documentation explains how to install the hook
- Hook can be bypassed in emergency (but this is discouraged)

---

### Story 5: Pre-push hook validates tests pass

```gherkin
Feature: Pre-push hook validates tests
  As a contributor
  I want tests to run before pushing to remote
  So that I don't push broken code to the team

Scenario: Pushing with passing tests succeeds
  Given I have committed changes
  And all affected tests pass
  When I run git push
  Then the pre-push hook runs
  And the hook runs relevant tests
  And all tests pass
  And the push succeeds

Scenario: Pushing with failing tests is blocked
  Given I have committed changes
  And some tests fail
  When I run git push
  Then the pre-push hook runs
  And the hook detects test failures
  And the hook shows which tests failed
  And the push is blocked
```

**Estimated Effort:** 3-4h
**Complexity:** Moderate

**Technical Feasibility:**
Moderate complexity because running tests locally can be time-consuming. We need to balance thoroughness with developer productivity. May need to allow running subset of tests or provide fast-fail behavior.

**Key technical challenges:**
- Test suite runtime is currently unknown (need to measure)
- Pre-push hook might be too slow if test suite is large
- Deciding which tests to run: all tests, only affected tests, or quick subset
- Handling integration tests that need external services
- Providing clear feedback when tests fail

**Acceptance:**
- Pre-push hook is committed to repository
- Hook runs test suite before allowing push
- Hook provides clear output showing test results
- Hook completes in reasonable time (< 2 minutes for unit tests)
- Documentation explains the hook behavior
- Hook can be bypassed with explicit flag (for emergency use only)

---

### Story 6: Comprehensive CI workflow with parallel jobs

```gherkin
Feature: CI workflow with optimized parallel execution
  As a team
  I want CI to run checks in parallel
  So that we get fast feedback on PRs

Scenario: PR triggers parallel CI jobs
  Given I create a PR with code changes
  When GitHub Actions CI runs
  Then compilation, tests, and formatting run in parallel
  And I see results from all jobs
  And total CI time is minimized

Scenario: One job failing marks the entire CI as failed
  Given I create a PR with code changes
  And the formatting check fails but tests pass
  When GitHub Actions CI completes
  Then the overall CI status is failed
  And I can see that formatting was the failing job
  And the PR cannot be merged
```

**Estimated Effort:** 2-3h
**Complexity:** Straightforward

**Technical Feasibility:**
Straightforward because GitHub Actions supports parallel jobs natively. This is primarily workflow organization and optimization work.

**Key technical challenges:**
- Structuring the workflow for maximum parallelism
- Sharing cached dependencies between parallel jobs
- Ensuring jobs fail fast to save CI minutes
- Setting up proper job dependencies (tests depend on compilation)

**Acceptance:**
- CI workflow has separate jobs for compilation, tests, and formatting
- Jobs run in parallel where possible
- Shared SBT and Coursier caches work across jobs
- Job status is clearly visible in GitHub PR UI
- Failed jobs provide clear error messages
- Workflow triggers on push to main and PRs to main

---

### Story 7: Documentation for contributors

```gherkin
Feature: Contributor documentation for CI and hooks
  As a new contributor
  I want clear documentation on CI and git hooks
  So that I can set up my environment correctly

Scenario: Contributor reads setup documentation
  Given I am a new contributor to the project
  When I read the documentation
  Then I understand how to install git hooks locally
  And I know what CI checks will run on my PRs
  And I know how to run the same checks locally
  And I know what to do if checks fail

Scenario: Contributor installs hooks successfully
  Given I am setting up the project locally
  When I follow the hook installation instructions
  Then the hooks are installed correctly
  And I can verify they work with a test commit
  And I know how to troubleshoot common issues
```

**Estimated Effort:** 2-3h
**Complexity:** Straightforward

**Technical Feasibility:**
Straightforward documentation work. Main challenge is making it clear and actionable for contributors of different experience levels.

**Acceptance:**
- README or CONTRIBUTING.md includes section on CI and hooks
- Documentation explains how to install git hooks locally
- Documentation explains what each CI job does
- Documentation shows how to run checks locally (before pushing)
- Documentation includes troubleshooting for common issues
- Documentation explains hook bypass procedure (for emergencies only)
- Documentation is clear and tested by following it step-by-step

---

## Architectural Sketch

**Purpose:** List WHAT components each story needs, not HOW they're implemented.

### For Story 1: Basic CI workflow validates compilation on PR

**GitHub Actions Workflow:**
- `.github/workflows/ci.yml` - Main workflow file
- Workflow triggers: `push` to main, `pull_request` to main
- Compilation job definition

**Build System Integration:**
- SBT compile commands for JVM modules
- SBT compile commands for JS modules
- Coursier cache configuration
- SBT cache configuration

**Infrastructure:**
- GitHub Actions runner (ubuntu-latest likely)
- Java/JDK setup (version 21 based on current environment)
- SBT installation

---

### For Story 2: CI runs test suite on PR

**GitHub Actions Workflow:**
- Test job in `.github/workflows/ci.yml`
- Job dependencies (depends on compilation)

**Test Execution:**
- SBT test command for unit tests
- Test result reporting
- Test failure output formatting

**Considerations:**
- Integration test handling (mongo-it, files-mongo-it modules)
- Test parallelization settings
- Test timeout configuration

---

### For Story 3: CI validates code formatting

**GitHub Actions Workflow:**
- Formatting check job in `.github/workflows/ci.yml`

**Formatting Verification:**
- SBT scalafmtCheckAll command
- Scalafmt configuration (already exists: `.scalafmt.conf`)
- Formatting error output

---

### For Story 4: Pre-commit hook validates formatting locally

**Git Hook Implementation:**
- Pre-commit hook script (location TBD based on implementation choice)
- Hook installer or setup mechanism

**Formatting Check:**
- Scalafmt execution for staged files only
- Staged file detection
- Error reporting to user
- Exit code handling

**Documentation:**
- Hook installation instructions
- Hook behavior explanation

---

### For Story 5: Pre-push hook validates tests pass

**Git Hook Implementation:**
- Pre-push hook script
- Hook installer or setup mechanism

**Test Execution:**
- SBT test command (full or subset)
- Test result capture
- Error reporting to user
- Exit code handling

**Documentation:**
- Hook behavior explanation
- Bypass procedure (for emergencies)

---

### For Story 6: Comprehensive CI workflow with parallel jobs

**GitHub Actions Workflow:**
- Job parallelization configuration
- Job dependencies (tests depend on compilation)
- Shared cache strategy across jobs
- Job naming and organization

**Optimization:**
- Fail-fast configuration
- Cache key strategy
- Job matrix if needed (for different Scala versions or platforms)

---

### For Story 7: Documentation for contributors

**Documentation Files:**
- README.md or CONTRIBUTING.md updates
- Possibly separate DEVELOPMENT.md

**Content:**
- CI workflow explanation
- Git hooks setup instructions
- Local check execution commands
- Troubleshooting guide
- Hook bypass procedure

---

## Technical Risks & Uncertainties

### CLARIFY: Git hook implementation approach

The issue mentions multiple options but doesn't specify which to use. We need to choose the implementation strategy for git hooks.

**Questions to answer:**
1. Which hook implementation approach should we use: shell scripts, sbt-git-hooks plugin, pre-commit framework, or lefthook?
2. Do contributors primarily work on Linux/macOS, or do we need Windows support?
3. Is there a team preference for any of these tools?
4. Should hooks be automatically installed (e.g., via SBT plugin) or manually installed by contributors?

**Options:**

- **Option A: Simple shell scripts committed to `.git-hooks/` directory**
  - Pros: No dependencies, full control, easy to customize, works everywhere
  - Cons: Requires manual installation, contributors must remember to install, no hook management features

- **Option B: sbt-git-hooks plugin**
  - Pros: Automatic installation via SBT, integrates well with Scala projects, hooks defined in build.sbt
  - Cons: Adds plugin dependency, less flexible than shell scripts, SBT-specific

- **Option C: pre-commit framework (Python-based)**
  - Pros: Rich ecosystem of hooks, automatic installation, supports multiple languages, widely used
  - Cons: Requires Python, adds external dependency, might be overkill for our needs

- **Option D: lefthook (Go-based)**
  - Pros: Fast, language-agnostic, YAML configuration, parallel hook execution, popular in modern projects
  - Cons: Requires Go or binary installation, adds external dependency, learning curve

**Impact:** Affects Stories 4 and 5. Decision determines hook installation process, contributor setup complexity, and maintainability.

**Recommendation pending clarification:** I'd lean toward Option A (shell scripts) or Option D (lefthook) depending on whether we want minimal dependencies or modern features. But this should be Michal's call based on team preferences and existing tooling.

---

### CLARIFY: Test suite characteristics

The full test suite runtime and characteristics are unknown, which affects multiple stories.

**Questions to answer:**
1. How long does the full test suite take to run (`sbt test`)?
2. Do integration tests (mongo-it, files-mongo-it) require external services (MongoDB)?
3. Should integration tests run in CI, or only unit tests?
4. Should pre-push hook run all tests, or only fast unit tests?
5. Are there test categorization or tagging in place (unit vs integration)?

**Options:**

- **Option A: Run all tests everywhere (CI and pre-push hook)**
  - Pros: Maximum confidence, catches all issues
  - Cons: Slow feedback, may frustrate contributors if tests take too long

- **Option B: Run only unit tests in hooks, all tests in CI**
  - Pros: Fast local feedback, comprehensive CI coverage
  - Cons: Contributors might push code that fails integration tests

- **Option C: Run unit tests in pre-push hook, integration tests only in CI, unit tests in pre-commit**
  - Pros: Balanced approach, fast commits, reasonable push time, comprehensive CI
  - Cons: More complex setup, requires test categorization

**Impact:** Affects Stories 2, 5, and 6. Decision determines CI runtime, contributor workflow smoothness, and test coverage strategy.

**Recommendation pending clarification:** Measure test suite runtime first, then decide. If under 1 minute, Option A is fine. If 1-3 minutes, Option B. If longer, Option C with test categorization.

---

### CLARIFY: Integration test infrastructure

Integration tests exist (mongo-it, files-mongo-it) but their infrastructure needs are unclear.

**Questions to answer:**
1. Do integration tests use Docker containers (e.g., Testcontainers)?
2. Do integration tests require external MongoDB installation?
3. Should CI run integration tests?
4. If CI runs integration tests, do we need service containers in the workflow?
5. Are integration tests reliable and fast enough for CI?

**Options:**

- **Option A: Skip integration tests in CI initially (Stories 1-3), add later**
  - Pros: Faster initial implementation, simpler CI workflow
  - Cons: Less comprehensive coverage, integration issues not caught in CI

- **Option B: Run integration tests with Docker/Testcontainers in CI**
  - Pros: Comprehensive testing, catches integration issues
  - Cons: Slower CI, requires service container setup, potential flakiness

- **Option C: Run integration tests only on main branch, not on PRs**
  - Pros: Faster PR feedback, still catches issues before release
  - Cons: Integration issues discovered late, might block main branch

**Impact:** Affects Story 2 and potentially Story 6. Decision determines CI complexity and test coverage.

**Recommendation pending clarification:** Start with Option A, implement basic CI without integration tests first, then add integration test support in a follow-up story once we understand the infrastructure needs.

---

### CLARIFY: Scalafix configuration

The issue mentions "Scalafix (if configured)" but it's unclear if it's set up.

**Questions to answer:**
1. Is Scalafix configured in this project?
2. If not, should we add it as part of this feature?
3. What Scalafix rules should we enforce?
4. Should Scalafix run in CI, pre-commit hook, or both?

**Options:**

- **Option A: Skip Scalafix entirely (not configured, not mentioned in requirements)**
  - Pros: Simpler implementation, fewer moving parts
  - Cons: Missing potential code quality checks

- **Option B: Add Scalafix configuration as part of this feature**
  - Pros: Better code quality, consistent patterns, catches issues early
  - Cons: Increases scope significantly, requires rule selection and configuration

- **Option C: Add Scalafix in a separate future issue**
  - Pros: Keeps this feature focused, allows proper Scalafix setup later
  - Cons: Delays Scalafix benefits

**Impact:** Affects Story 3 and Story 4 if Scalafix is included. Decision determines CI and hook complexity.

**Recommendation pending clarification:** Option A or C. The issue says "(if configured)" which implies it's optional. Skip Scalafix in this feature unless Michal explicitly wants it included.

---

### CLARIFY: Workflow triggering strategy

The issue specifies "Push to main, Pull requests to main" but some details need clarification.

**Questions to answer:**
1. Should CI run on push to any branch, or only main and PR branches?
2. Should CI run on draft PRs?
3. Should CI run differently for main vs PRs (e.g., integration tests only on main)?
4. Should we have separate workflows for PR validation vs main branch?

**Options:**

- **Option A: Single workflow that runs on both push to main and PR to main**
  - Pros: Simpler, consistent behavior, easier to maintain
  - Cons: Might run redundantly (PR merge triggers both PR and push)

- **Option B: Separate workflows for PR and main with different job configurations**
  - Pros: Can run different checks (integration tests on main only)
  - Cons: More complex, potential duplication

- **Option C: Single workflow with conditional jobs based on trigger**
  - Pros: Single workflow file, different behavior when needed
  - Cons: More complex workflow logic

**Impact:** Affects Story 1 and Story 6. Decision determines workflow structure and CI behavior.

**Recommendation pending clarification:** Start with Option A (simplest), refactor to Option C if we need different behavior for main vs PRs.

---

### CLARIFY: Dependency caching strategy

SBT and Coursier caching is mentioned but strategy needs definition.

**Questions to answer:**
1. What should be cached: SBT boot directory, Coursier cache, target directories, all of the above?
2. Should cache be shared across all jobs or per-job?
3. What should be the cache key: `build.sbt` hash, lock file, date-based?
4. Should we cache compiled artifacts (target directories) or only dependencies?

**Options:**

- **Option A: Cache only dependency downloads (Coursier cache)**
  - Pros: Simple, safe, always compiles from scratch
  - Cons: Slower CI, doesn't leverage compiled artifacts

- **Option B: Cache dependencies and compiled artifacts (target directories)**
  - Pros: Faster CI, reuses compilation
  - Cons: Larger cache, potential for stale artifacts, cache invalidation complexity

- **Option C: Aggressive caching with careful invalidation**
  - Pros: Maximum speed
  - Cons: Complex, risk of cache-related bugs

**Impact:** Affects Story 1 and Story 6. Decision determines CI speed and complexity.

**Recommendation pending clarification:** Start with Option A (conservative), measure CI time, then optimize to Option B if needed. Typical SBT project caching includes:
- `~/.sbt`
- `~/.ivy2/cache`
- `~/.cache/coursier`

---

## Total Estimates

**Story Breakdown:**
- Story 1 (Basic CI workflow validates compilation on PR): 3-4 hours
- Story 2 (CI runs test suite on PR): 2-3 hours
- Story 3 (CI validates code formatting): 1-2 hours
- Story 4 (Pre-commit hook validates formatting locally): 4-6 hours
- Story 5 (Pre-push hook validates tests pass): 3-4 hours
- Story 6 (Comprehensive CI workflow with parallel jobs): 2-3 hours
- Story 7 (Documentation for contributors): 2-3 hours

**Total Range:** 17-25 hours

**Confidence:** Medium

**Reasoning:**
- **Medium confidence** because several CLARIFY markers need resolution before we have full clarity
- CI workflow implementation is well-understood (Stories 1-3), hence tighter estimates
- Git hook implementation has more uncertainty (Stories 4-5) due to tooling choice and environment variations
- Test suite characteristics are unknown, which affects Stories 2 and 5 estimates
- Integration test handling is unclear, which could add time to Story 2
- Estimates assume straightforward implementation with modern GitHub Actions features
- Estimates assume no major issues with multi-module cross-compilation in CI
- Documentation (Story 7) estimate assumes standard contributor guide, not extensive troubleshooting docs

**Factors contributing to estimate:**
- **GitHub Actions experience**: Workflow creation is standard work, but multi-module SBT project adds complexity
- **Caching strategy**: Proper SBT caching setup will take experimentation to optimize
- **Hook implementation**: Choice of tooling significantly affects implementation time (shell scripts faster than framework setup)
- **Testing unknowns**: Integration test handling could add 2-4 hours if complex infrastructure needed
- **Cross-compilation**: JVM + JS compilation in CI might have edge cases to handle

**Estimate adjustments based on CLARIFY resolution:**
- If integration tests need Docker services in CI: +2-4 hours to Story 2
- If we choose pre-commit framework or lefthook: +1-2 hours to Stories 4-5 for setup
- If we add Scalafix configuration: +3-5 hours to Stories 3-4
- If test suite is very slow (>5 minutes): +2-3 hours to Stories 2 and 5 for optimization

---

## Testing Approach

**Per Story Testing:**

Each story should have verification steps to confirm it works correctly.

### Story 1: Basic CI workflow validates compilation on PR

**Manual Testing:**
1. Create workflow file locally
2. Create a test PR with valid code
3. Verify workflow triggers and compilation succeeds
4. Create a test PR with broken code (syntax error)
5. Verify workflow triggers and compilation fails with clear error

**Validation:**
- Workflow appears in Actions tab
- Both JVM and JS modules compile
- Cache hits on subsequent runs
- Failure shows helpful error message
- PR shows status check correctly

---

### Story 2: CI runs test suite on PR

**Manual Testing:**
1. Create test job in workflow
2. Push changes with passing tests
3. Verify tests run and pass in CI
4. Introduce a failing test
5. Verify CI catches the failure with clear output

**Validation:**
- Test job runs after compilation
- All test modules execute
- Test output is captured in logs
- Failing tests show test name and error
- Integration tests handled correctly (or excluded if decided in CLARIFY)

---

### Story 3: CI validates code formatting

**Manual Testing:**
1. Add formatting check job to workflow
2. Push well-formatted code
3. Verify formatting check passes
4. Push unformatted code (violates .scalafmt.conf)
5. Verify formatting check fails with file list

**Validation:**
- Formatting check runs quickly (parallel with other jobs)
- Check reports which files need formatting
- Check respects .scalafmt.conf rules
- Check exit code causes workflow failure

---

### Story 4: Pre-commit hook validates formatting locally

**Manual Testing:**
1. Install hook according to setup procedure
2. Stage well-formatted code
3. Commit and verify hook allows it
4. Stage unformatted code
5. Try to commit and verify hook blocks it
6. Verify hook shows which files need formatting
7. Format the files and verify commit succeeds

**Validation:**
- Hook installs correctly on Linux/macOS (and Windows if supported)
- Hook only checks staged files, not entire codebase
- Hook runs quickly (< 10 seconds)
- Hook error messages are clear and actionable
- Hook can be bypassed with `--no-verify` if needed (but documented as discouraged)

---

### Story 5: Pre-push hook validates tests pass

**Manual Testing:**
1. Install hook according to setup procedure
2. Make changes with passing tests
3. Commit and push, verify hook allows it
4. Make changes that break tests
5. Commit and try to push, verify hook blocks it
6. Verify hook shows test failure details
7. Fix tests and verify push succeeds

**Validation:**
- Hook runs appropriate test suite (unit tests, or subset if decided)
- Hook completes in reasonable time (< 2 minutes ideally)
- Hook output shows test progress and results
- Hook can be bypassed with force flag if needed (documented)
- Hook doesn't run tests if no commits to push

---

### Story 6: Comprehensive CI workflow with parallel jobs

**Manual Testing:**
1. Create PR with changes
2. Verify multiple jobs start simultaneously
3. Check that cache sharing works between jobs
4. Verify test job waits for compilation to pass
5. Fail one job (e.g., formatting) and verify overall status is failed

**Validation:**
- Jobs run in parallel where possible
- Dependencies respected (tests after compilation)
- Overall workflow status reflects all job statuses
- Cache improves second run speed significantly
- Job failures are clearly visible in PR status checks

---

### Story 7: Documentation for contributors

**Manual Testing:**
1. Write documentation following the architectural sketch
2. Have someone unfamiliar with the setup follow the docs
3. Verify they can install hooks successfully
4. Verify they understand what CI does
5. Verify they can run checks locally

**Validation:**
- Documentation is clear and unambiguous
- Hook installation steps work exactly as documented
- Local check commands are correct and runnable
- Troubleshooting section covers common issues
- Documentation tested by following it step-by-step

---

**Test Data Strategy:**
- Use actual project code for CI testing (real compilation, real tests)
- Create intentional failures for negative testing (syntax errors, test failures, formatting issues)
- Test hook behavior with various file states (staged, unstaged, mixed)
- Test cache behavior with multiple workflow runs

**Regression Coverage:**
- Existing tests should continue to pass in CI
- Existing code should compile successfully in CI
- Hooks should not prevent valid commits
- Hooks should not prevent emergency pushes (with --no-verify)

---

## Deployment Considerations

### Database Changes
None. This feature does not modify application code or data models.

### Configuration Changes

**GitHub repository settings:**
- Enable GitHub Actions if not already enabled
- Configure branch protection rules for main branch (should require CI checks to pass)
- Potentially add required status checks: compilation, tests, formatting

**Environment variables (if needed):**
- None required for basic CI
- If integration tests run in CI with external services, might need credentials

**Workflow secrets:**
- None required for public workflows
- EBS Nexus credentials might already be available as repository secrets (see `EBS_NEXUS_USERNAME`, `EBS_NEXUS_PASSWORD` in project/project/plugins.sbt)

### Rollout Strategy

**Incremental deployment:**
1. **Phase 1**: Implement CI workflow (Stories 1-3) - can be merged and deployed independently
2. **Phase 2**: Add git hooks (Stories 4-5) - can be merged but optional for contributors initially
3. **Phase 3**: Optimize workflow (Story 6) - refinement of Phase 1
4. **Phase 4**: Add documentation (Story 7) - completes the feature

**Feature flag strategy:**
Not applicable - workflows and hooks are opt-in by nature.

**Gradual rollout:**
- Merge workflow files first, announce in team chat
- Make hooks available but optional initially
- After validation period, document hooks as recommended practice
- Eventually enforce via code review

### Rollback Plan

**If CI workflow has issues:**
- Delete or rename `.github/workflows/ci.yml` to disable it
- No impact on contributors' local work
- Can fix issues in a new PR

**If git hooks cause problems:**
- Contributors can remove hooks from `.git/hooks/` directory
- Or use `git commit --no-verify` and `git push --no-verify`
- Update documentation to reflect known issues
- Fix hooks and re-release

**No dangerous operations:**
- This feature is purely additive (validation only)
- Does not modify code or data
- Easy to disable if needed

---

## Dependencies

### Prerequisites

**Before starting Story 1:**
- GitHub repository with Actions enabled (should already be the case)
- Understanding of SBT build system (already established)
- Decision on CLARIFY markers (especially hook implementation approach)

**Project knowledge:**
- Scala 3.7.0, SBT build system
- Multi-module project with JVM + JS cross-compilation
- Scalafmt 3.7.17 configured
- Test framework in use (appears to be specs-based tests)
- Project builds successfully locally

### Story Dependencies

**Sequential dependencies:**
- Story 2 (CI tests) logically follows Story 1 (CI compilation) - should compile before testing
- Story 4 (pre-commit hook) should be implemented before Story 5 (pre-push hook) - establishes hook pattern
- Story 6 (optimized workflow) refines Stories 1-3 - optimization of initial implementation
- Story 7 (documentation) should be last - documents the complete setup

**Can be parallelized:**
- Stories 1-3 (CI workflow) can be implemented independently of Stories 4-5 (git hooks)
- Story 3 (formatting CI) can be developed in parallel with Stories 1-2
- Stories 4-5 can be developed in parallel once hook implementation approach is chosen

**Recommended sequence:**
1. Story 1 (CI compilation) - foundation
2. Stories 2 and 3 in parallel (CI tests and formatting) - build on Story 1
3. Story 4 (pre-commit hook) - establish hook pattern
4. Story 5 (pre-push hook) - follow hook pattern
5. Story 6 (CI optimization) - refine based on real usage
6. Story 7 (documentation) - document complete solution

### External Blockers

**Potential blockers:**
- GitHub Actions must be enabled on the repository
- If integration tests need external services, those must be available or mockable
- If using a hook framework (pre-commit, lefthook), contributors need to install it
- Team must agree on hook implementation approach (CLARIFY marker)

**No known current blockers:**
- Build system already working
- Tests already exist
- Scalafmt already configured
- GitHub repository accessible

---

## Implementation Sequence

**Recommended Story Order:**

1. **Story 1: Basic CI workflow validates compilation on PR** - Establishes the CI foundation, highest value (catches broken builds immediately), unblocks other CI stories

2. **Story 3: CI validates code formatting** - Quick win, runs fast in parallel with compilation, catches trivial issues early

3. **Story 2: CI runs test suite on PR** - Adds test validation to CI, requires resolving test strategy CLARIFY marker

4. **Story 6: Comprehensive CI workflow with parallel jobs** - Optimizes Stories 1-3, improves feedback speed, best done after understanding real CI performance

5. **Story 4: Pre-commit hook validates formatting locally** - Establishes git hook pattern and installation process, catches formatting issues before commit

6. **Story 5: Pre-push hook validates tests pass** - Adds test validation locally, follows pattern from Story 4, prevents pushing broken code

7. **Story 7: Documentation for contributors** - Documents the complete CI and hook setup, best done when everything else is working

**Iteration Plan:**

### Iteration 1: Basic CI (Stories 1, 3) - Core validation foundation
**Goal:** Get basic automated validation working in CI
**Deliverables:**
- GitHub Actions workflow compiling all modules
- Formatting check in CI
- Green/red status on PRs
**Duration:** 4-6 hours
**Value:** Immediate feedback on compilation and formatting issues

### Iteration 2: Complete CI (Stories 2, 6) - Comprehensive automated checks
**Goal:** Add test execution and optimize workflow performance
**Deliverables:**
- Tests running in CI
- Parallel job execution
- Optimized caching
**Duration:** 4-6 hours
**Value:** Full automated validation, faster feedback through parallelization

### Iteration 3: Local hooks (Stories 4, 5) - Developer workflow enhancement
**Goal:** Provide local validation before pushing to remote
**Deliverables:**
- Pre-commit hook for formatting
- Pre-push hook for tests
- Hook installation mechanism
**Duration:** 7-10 hours
**Value:** Catch issues locally, reduce CI failures, faster developer feedback

### Iteration 4: Documentation (Story 7) - Complete the feature
**Goal:** Ensure all contributors can use CI and hooks effectively
**Deliverables:**
- Contributor documentation
- Setup instructions
- Troubleshooting guide
**Duration:** 2-3 hours
**Value:** Lower barrier to entry, self-service setup, reduced support burden

---

## Documentation Requirements

- [x] README.md or CONTRIBUTING.md with CI and hooks section
- [x] Hook installation instructions (step-by-step)
- [x] CI workflow explanation (what runs, when, why)
- [x] Local check execution guide (how to run checks before pushing)
- [x] Troubleshooting section (common issues and solutions)
- [x] Hook bypass procedure (for emergencies, clearly marked as discouraged)
- [ ] Architecture Decision Record (ADR) for hook implementation choice (if using ADRs)
- [ ] GitHub Actions workflow comments (inline documentation in ci.yml)

---

**Analysis Status:** Ready for Review - Pending CLARIFY Marker Resolution

**Next Steps:**

1. **Resolve CLARIFY markers with Michal:**
   - Hook implementation approach (shell scripts vs framework)
   - Test suite characteristics (runtime, integration tests)
   - Integration test infrastructure needs
   - Scalafix inclusion decision
   - Workflow triggering strategy
   - Dependency caching strategy

2. **After CLARIFY resolution:**
   - Run `/iterative-works:ag-create-tasks SUPP-9` to map stories to implementation phases
   - Run `/iterative-works:ag-implement SUPP-9` for iterative story-by-story implementation

3. **Before starting implementation:**
   - Measure test suite runtime (`time sbt test`)
   - Confirm Java/JDK version for CI (appears to be Java 21)
   - Verify GitHub Actions is enabled on repository
   - Check if integration tests can run without external services
