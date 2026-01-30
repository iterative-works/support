# Contributing to Support Library

## CI Workflow

All pull requests are validated by GitHub Actions CI. The workflow runs the following checks:

### Parallel Checks (run immediately)

| Job | Command | Description |
|-----|---------|-------------|
| **Compile** | `./mill __.compile` | Compiles all JVM and ScalaJS modules |
| **Format** | `./mill __.checkFormat` | Validates Scalafmt formatting |
| **Lint** | `./mill __.fix --check` | Validates Scalafix FP rules |

### Sequential Check (runs after compile)

| Job | Command | Description |
|-----|---------|-------------|
| **Test** | `./mill __.test` | Runs all unit tests |

## Git Hooks

The project provides optional git hooks to catch issues before they reach CI.

### Installation

```bash
# Install pre-commit hook (validates formatting)
ln -s ../../.git-hooks/pre-commit .git/hooks/pre-commit

# Install pre-push hook (runs tests)
ln -s ../../.git-hooks/pre-push .git/hooks/pre-push
```

**Note for git worktrees:** If you're using a git worktree, the hooks directory
is in the main repository's `.git/` directory. Adjust paths accordingly:

```bash
# For worktrees, you may need to find the actual .git/hooks directory
# Example: ln -s /path/to/main-repo/.git-hooks/pre-commit /path/to/main-repo/.git/hooks/pre-commit
```

### What the Hooks Do

| Hook | Trigger | What it checks | Time |
|------|---------|----------------|------|
| **pre-commit** | Before each commit | Scala formatting | ~10 seconds |
| **pre-push** | Before each push | All unit tests | ~2 minutes |

## Running Checks Locally

Before pushing, you can run the same checks CI will run:

```bash
# Compile all modules (JVM + ScalaJS)
./mill __.compile

# Check formatting
./mill __.checkFormat

# Fix formatting automatically
./mill __.reformat

# Check Scalafix rules
./mill __.fix --check

# Apply Scalafix fixes
./mill __.fix

# Run all tests
./mill __.test
```

## Troubleshooting

### "Code formatting issues detected"

Your code doesn't match the project's Scalafmt configuration.

**Fix:**
```bash
./mill __.reformat
git add -u
```

### "Tests are failing"

One or more tests are broken.

**Fix:**
```bash
# Run tests to see failures
./mill __.test

# Run tests for a specific module
./mill core.jvm.test
```

### "Scalafix violations detected"

Your code violates FP rules (e.g., using `null`, `var`, `throw`).

**Fix:**
```bash
# See what rules are violated
./mill __.fix --check

# If the violation is intentional (e.g., Java interop), add a suppression:
// scalafix:off DisableSyntax.null
val result = javaApi.mayReturnNull()  // Java API returns null
// scalafix:on DisableSyntax.null
```

### Bypassing Hooks (Emergency Only)

In rare cases, you may need to bypass hooks:

```bash
# Bypass pre-commit hook
git commit --no-verify

# Bypass pre-push hook
git push --no-verify
```

**Warning:** Use only for genuine emergencies. CI will still run all checks,
and your PR will fail if there are issues.

## Code Style

The project uses:
- **Scalafmt** for code formatting (config: `.scalafmt.conf`)
- **Scalafix** for FP rules (config: `.scalafix.conf`)

Key Scalafix rules enforced:
- No `null` usage (use `Option` instead)
- No `var` usage (use immutable values)
- No `throw` usage (use `Either`, `ZIO` error handling)
- No `return` usage (use expression-oriented style)

When interop with Java or browser APIs requires these constructs,
use documented suppressions with explanatory comments.
