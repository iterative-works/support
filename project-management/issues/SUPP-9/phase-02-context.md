# Phase 02: CI validates code formatting

**Issue:** SUPP-9
**Phase:** 2 of 8
**Story:** As a team, I want CI to verify code follows our formatting standards, so that code style stays consistent across the codebase

## Goals

This phase adds a formatting validation job to the CI workflow that ensures all code follows the Scalafmt configuration.

**Outcomes:**
1. CI workflow includes a formatting check job
2. Job runs `mill` scalafmt check command
3. Unformatted code causes CI to fail
4. Formatting errors show which files need formatting
5. Formatting check runs quickly (parallel with compilation)

## Scope

### In Scope
- Add formatting check job to `.github/workflows/ci.yml`
- Configure job to run scalafmt check via Mill
- Ensure clear error output when formatting issues are found
- Run formatting check in parallel with compilation job

### Out of Scope
- Test execution (Phase 04)
- Scalafix linting (Phase 03)
- Parallel job optimization beyond basic parallelism (Phase 05)
- Git hooks (Phase 06-07)
- Documentation (Phase 08)

## Dependencies

### From Previous Phases
- Phase 01: Basic CI workflow exists at `.github/workflows/ci.yml`

### External Dependencies
- Scalafmt configured (`.scalafmt.conf` exists)
- Mill scalafmt plugin available

## Technical Approach

### Mill Scalafmt Commands

Mill provides scalafmt support through its built-in scalafmt module. The check command:
- `mill __.checkFormat` or `mill mill.scalalib.scalafmt.ScalafmtModule/checkFormatAll` - Check all files without modifying them

Need to verify the exact command available in this Mill setup.

### Job Structure

Add a new job `format` that:
1. Runs in parallel with `compile` job (no dependency)
2. Uses same caching strategy for dependencies
3. Runs scalafmt check command
4. Reports which files need formatting on failure

### Error Handling

- Scalafmt returns non-zero exit code when files are unformatted
- Error output lists the files that need formatting
- GitHub Actions will fail the workflow on non-zero exit

## Files to Modify

### Modified Files
- `.github/workflows/ci.yml` - Add formatting check job

### New Files
None.

## Testing Strategy

### Manual Verification
1. Push the updated workflow to a branch
2. Create a test PR with properly formatted code
3. Verify both compile and format jobs run in parallel
4. Verify format job passes
5. Introduce formatting issues (e.g., remove spaces, break indentation)
6. Verify format job fails with clear file listing
7. Fix formatting and verify job passes

### Verification Checklist
- [ ] Format job appears in workflow
- [ ] Format job runs in parallel with compile job
- [ ] Well-formatted code passes the check
- [ ] Unformatted code fails the check
- [ ] Failure output shows which files need formatting
- [ ] PR status shows format check result

## Acceptance Criteria

From the user story (Story 3 in analysis.md):

```gherkin
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

**Specific Acceptance:**
1. CI workflow includes format check job
2. Job runs Mill scalafmt check command
3. Unformatted code causes CI to fail
4. Formatting errors show which files need formatting
5. Formatting check runs in parallel with compilation (no dependency)
6. PR status check reflects formatting result

## Notes

- Scalafmt 3.7.17 is already configured in `.scalafmt.conf`
- This is a quick phase - mainly workflow YAML changes
- Format check is independent of compilation, so can run in parallel
- Phase 05 will optimize the overall workflow structure
