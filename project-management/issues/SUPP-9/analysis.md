# Story-Driven Analysis: Set up CI and git hooks for quality assurance

**Issue:** SUPP-9
**Created:** 2026-01-28
**Status:** Ready for Implementation
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
This is straightforward because we already have a working Mill build system with cross-compilation. The main work is creating the GitHub Actions workflow YAML and configuring caching for Coursier dependencies.

**Key technical challenges:**
- Setting up proper caching for Mill and Coursier to avoid slow CI runs
- Ensuring both JVM and JS modules compile in the workflow
- Handling the multi-module structure correctly

**Acceptance:**
- `.github/workflows/ci.yml` exists and runs on PR creation
- Workflow compiles all JVM modules successfully
- Workflow compiles all JS modules successfully
- Coursier dependencies are cached between runs
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
- Test job executes `mill __.test` successfully
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
Very straightforward because Scalafmt 3.7.17 is already configured (`.scalafmt.conf` exists). We just need to run `mill mill.scalalib.scalafmt.ScalafmtModule/checkFormatAll` or equivalent in the workflow.

**Acceptance:**
- CI workflow includes formatting check job
- Job runs Mill scalafmt check command
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
- Shared Mill and Coursier caches work across jobs
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

### Story 8: Scalafix enforces FP principles

```gherkin
Feature: Scalafix validates code quality
  As a team using AI agents
  I want automated enforcement of FP principles
  So that code follows our standards regardless of who wrote it

Scenario: PR with FP-compliant code passes Scalafix check
  Given I create a PR with code changes
  And the code follows FP principles (no nulls, vars, etc.)
  When GitHub Actions CI runs
  Then the Scalafix check job succeeds
  And I see a green check for linting

Scenario: PR with FP violations fails Scalafix check
  Given I create a PR with code changes
  And the code contains a null or var
  When GitHub Actions CI runs
  Then the Scalafix check job fails
  And I see which rules were violated in the CI logs
  And the PR shows a red X for linting
```

**Estimated Effort:** 2-3h
**Complexity:** Straightforward

**Technical Feasibility:**
Straightforward because Scalafix has Mill plugin support and sensible default rules. Main work is configuration and testing the rules work correctly with Scala 3.

**Key technical challenges:**
- Ensuring Scalafix rules work with Scala 3.7
- Choosing appropriate rules that catch issues without being too noisy
- Integrating with Mill build system

**Acceptance:**
- `.scalafix.conf` exists with configured rules
- Mill build includes Scalafix plugin
- CI workflow includes Scalafix check job
- `DisableSyntax` catches null, var usage
- `ExplicitResultTypes` enforces explicit return types on public APIs
- `NoValInForComprehension` catches mutable vals in for-comps
- Scalafix violations cause CI to fail with clear error messages

---

## Architectural Sketch

**Purpose:** List WHAT components each story needs, not HOW they're implemented.

### For Story 1: Basic CI workflow validates compilation on PR

**GitHub Actions Workflow:**
- `.github/workflows/ci.yml` - Main workflow file
- Workflow triggers: `push` to main, `pull_request` to main
- Compilation job definition

**Build System Integration:**
- Mill compile commands for JVM modules
- Mill compile commands for JS modules
- Coursier cache configuration
- Mill cache configuration

**Infrastructure:**
- GitHub Actions runner (ubuntu-latest likely)
- Java/JDK setup (version 21 based on current environment)
- Mill installation

---

### For Story 2: CI runs test suite on PR

**GitHub Actions Workflow:**
- Test job in `.github/workflows/ci.yml`
- Job dependencies (depends on compilation)

**Test Execution:**
- Mill test command for unit tests
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
- Mill scalafmt check command
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
- Mill test command (unit tests only per decision)
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

### For Story 8: Scalafix enforces FP principles

**Scalafix Configuration:**
- `.scalafix.conf` - Rule configuration file
- Mill plugin for Scalafix

**Rules to Enable:**
- `DisableSyntax` - Ban null, var, mutable constructs
- `ExplicitResultTypes` - Require explicit return types
- `NoValInForComprehension` - Prevent mutable vals in for-comps

**CI Integration:**
- Scalafix check job in workflow
- Clear error output for violations

---

## Resolved Technical Decisions

### RESOLVED: Git hook implementation approach

**Decision:** Option A - Simple shell scripts committed to `.git-hooks/` directory

**Rationale:** No external dependencies, full control. Future plan: iw-cli will provide a command to check and set up hooks across projects on a per-project basis.

---

### RESOLVED: Test suite characteristics

**Decision:** Option C - Unit tests in pre-push hook, integration tests in CI only

- **Pre-push hook:** Unit tests only (fast feedback)
- **CI:** All tests including integration tests
- **Developer:** Runs integration tests manually when needed

**Build system note:** Project uses Mill, not SBT.

---

### RESOLVED: Integration test infrastructure

**Decision:** Skip MongoDB integration tests in CI for now

**Investigation findings:**
- MongoDB tests (`mongo-it`, `files-mongo-it`) do NOT use Testcontainers
- They require external MongoDB and use `TestAspect.ifEnvSet("MONGO_URI")`
- Tests skip gracefully without the env var (no failure)
- SQL tests DO have proper Testcontainers support (reference implementation exists)

**Follow-up:** Created GitHub issue #10 to convert MongoDB integration tests to Testcontainers.

---

### RESOLVED: Scalafix configuration

**Decision:** Option B - Add Scalafix configuration as part of this feature

**Rationale:** With AI agents writing code, automated enforcement of FP principles provides valuable safety net. Rules to include:
- `DisableSyntax` (null, var, mutable constructs)
- `ExplicitResultTypes` (documents ZIO effects)
- `NoValInForComprehension` (immutable FP style)

Added as **Story 8** below.

---

### RESOLVED: Workflow triggering strategy

**Decision:** CI triggers on PRs to main only

**Rationale:** PRs are where the bulk of verification happens. Direct pushes to main are rare, manual, and intentional - can run CI manually if needed.

---

### RESOLVED: Dependency caching strategy

**Decision:** Option A - Conservative caching (Coursier/Mill cache only)

Cache only dependency downloads:
- `~/.cache/coursier`
- `~/.mill`

Start simple, measure CI time, optimize to include compiled artifacts later if needed

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
- Story 8 (Scalafix enforces FP principles): 2-3 hours

**Total Range:** 19-28 hours

**Confidence:** High

**Reasoning:**
- **High confidence** because all CLARIFY markers have been resolved
- CI workflow implementation is well-understood (Stories 1-3, 8), hence tighter estimates
- Git hook implementation uses simple shell scripts (Stories 4-5), reducing complexity
- MongoDB integration tests will be skipped in CI (auto-skip without MONGO_URI)
- Build system is Mill (not SBT), which has good CI support
- Scalafix has Mill plugin support and sensible default rules

**Factors contributing to estimate:**
- **GitHub Actions experience**: Workflow creation is standard work
- **Mill build**: Multi-module Mill project with JVM + JS cross-compilation
- **Caching strategy**: Conservative approach (dependencies only) simplifies setup
- **Hook implementation**: Shell scripts are straightforward, no framework overhead
- **Scalafix**: Standard configuration with well-documented rules

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

### Story 8: Scalafix enforces FP principles

**Manual Testing:**
1. Add Scalafix plugin to Mill build
2. Create `.scalafix.conf` with configured rules
3. Run Scalafix locally on well-formed code
4. Verify it passes
5. Introduce a `null` or `var` in code
6. Run Scalafix and verify it catches the violation
7. Add Scalafix check to CI workflow
8. Push changes and verify CI catches violations

**Validation:**
- Scalafix plugin integrates with Mill
- `.scalafix.conf` contains appropriate rules
- `DisableSyntax` catches null, var usage
- `ExplicitResultTypes` enforces explicit return types
- `NoValInForComprehension` catches mutable vals
- CI job runs Scalafix check
- Violations show clear error messages

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
- EBS Nexus credentials might already be available as repository secrets (see `EBS_NEXUS_USERNAME`, `EBS_NEXUS_PASSWORD` in build configuration)

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
- Understanding of Mill build system (already established)
- Decision on CLARIFY markers (especially hook implementation approach)

**Project knowledge:**
- Scala 3.7.0, Mill build system
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

3. **Story 8: Scalafix enforces FP principles** - Add linting for FP violations, complements formatting check

4. **Story 2: CI runs test suite on PR** - Adds test validation to CI

5. **Story 6: Comprehensive CI workflow with parallel jobs** - Optimizes Stories 1-4, improves feedback speed

6. **Story 4: Pre-commit hook validates formatting locally** - Establishes git hook pattern and installation process, catches formatting issues before commit

7. **Story 5: Pre-push hook validates tests pass** - Adds test validation locally, follows pattern from Story 4, prevents pushing broken code

8. **Story 7: Documentation for contributors** - Documents the complete CI and hook setup, best done when everything else is working

**Iteration Plan:**

### Iteration 1: Basic CI (Stories 1, 3, 8) - Core validation foundation
**Goal:** Get basic automated validation working in CI
**Deliverables:**
- GitHub Actions workflow compiling all modules
- Formatting check in CI (Scalafmt)
- Linting check in CI (Scalafix)
- Green/red status on PRs
**Duration:** 6-9 hours
**Value:** Immediate feedback on compilation, formatting, and FP violations

### Iteration 2: Complete CI (Stories 2, 6) - Comprehensive automated checks
**Goal:** Add test execution and optimize workflow performance
**Deliverables:**
- Tests running in CI (unit + integration where available)
- Parallel job execution
- Optimized caching
**Duration:** 4-6 hours
**Value:** Full automated validation, faster feedback through parallelization

### Iteration 3: Local hooks (Stories 4, 5) - Developer workflow enhancement
**Goal:** Provide local validation before pushing to remote
**Deliverables:**
- Pre-commit hook for formatting
- Pre-push hook for unit tests
- Shell scripts in `.git-hooks/` directory
**Duration:** 7-10 hours
**Value:** Catch issues locally, reduce CI failures, faster developer feedback

### Iteration 4: Documentation (Story 7) - Complete the feature
**Goal:** Ensure all contributors can use CI and hooks effectively
**Deliverables:**
- Contributor documentation
- Hook installation instructions
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

**Analysis Status:** Ready for Implementation

**All decisions resolved:**
- ✅ Git hooks: Shell scripts (iw-cli will manage later)
- ✅ Test strategy: Unit tests in pre-push, integration in CI
- ✅ Integration tests: Skip MongoDB in CI (issue #10 for Testcontainers)
- ✅ Scalafix: Include with FP enforcement rules
- ✅ CI triggers: PRs to main only
- ✅ Caching: Conservative (dependencies only)

**Next Steps:**

1. Run `/iterative-works:ag-create-tasks SUPP-9` to map stories to implementation phases
2. Run `/iterative-works:ag-implement SUPP-9` for iterative story-by-story implementation

**Before starting implementation:**
- Confirm Java/JDK version for CI (appears to be Java 21)
- Verify GitHub Actions is enabled on repository
