---
name: iw-command-creation
description: |
  Create ad-hoc scripts or project workflow helpers using iw-cli's core modules.
  Use when:
  - Creating automation scripts for repetitive tasks
  - Building project-specific workflow commands
  - Composing one-off tools from iw-cli's functional building blocks
  - Adding custom commands to a project's .iw/commands/ directory
---

# Creating Scripts with iw-cli Core Modules

iw-cli provides a set of functional Scala modules for common CLI tasks: git operations,
issue tracking, tmux sessions, console output, and more. You can compose these into
ad-hoc scripts or project-specific commands.

## When to Use This

- **Ad-hoc scripts**: One-off automation tasks (batch operations, migrations, reports)
- **Project commands**: Workflow helpers committed to `.iw/commands/` for the team
- **Custom integrations**: Combining git, issue tracking, and shell operations

## Quick Start: Ad-hoc Script

Create a script file anywhere (e.g., `my-script.scala`):

```scala
// PURPOSE: Example script using iw-cli core modules

import iw.core.*

@main def myScript(args: String*): Unit =
  Output.section("My Script")

  // Use GitAdapter for git operations
  GitAdapter.getCurrentBranch(os.pwd) match
    case Left(err) => Output.error(s"Not in a git repo: $err")
    case Right(branch) => Output.info(s"Current branch: $branch")

  // Use IssueId to parse issue identifiers
  IssueId.fromBranch(branch) match
    case Left(_) => Output.warning("Not on an issue branch")
    case Right(id) => Output.success(s"Working on issue: ${id.value}")
```

Run it with:
```bash
./iw ./<script-name>
# Or directly with scala-cli:
scala-cli run my-script.scala $IW_CORE_DIR/*.scala
```

## Project Commands

For commands you want to share with the team, place them in `.iw/commands/`:

```
project/
└── .iw/
    └── commands/
        └── my-command.scala    # Run with: ./iw ./my-command
```

Project commands are run with the `./` prefix: `./iw ./my-command`

## File Structure

Every command/script follows this structure:

### 1. Header Comments (PURPOSE lines)

```scala
// PURPOSE: Brief description of what the command does
// USAGE: iw ./command-name [args]
// ARGS:
//   --flag: Description of flag
// EXAMPLE: iw ./command-name --flag value
```

### 2. Imports

```scala
import iw.core.*                      // All core modules
import iw.core.infrastructure.*       // Infrastructure adapters (if needed)
```

No `//> using file` directives needed - the iw launcher handles the classpath automatically.

### 3. Entry Point with @main Annotation

```scala
@main def commandName(args: String*): Unit =
  // command implementation
```

**Conventions:**
- Use `@main` annotation (NOT `object` with `def main`)
- Function name should match the file name (e.g., `my-command.scala` → `def my-command`)
- Parameter is `args: String*` (varargs)
- Use `sys.exit(1)` for errors, `sys.exit(0)` for early success

## Finding Core Module Documentation

Complete API documentation is available in llms.txt format. The documentation includes
function signatures, parameter types, return types, and real usage examples.

```bash
# Read the main index (lists all modules with descriptions)
cat $IW_CORE_DIR/../llms.txt

# Read detailed documentation for a specific module
cat $IW_CORE_DIR/../docs/Output.md
cat $IW_CORE_DIR/../docs/Git.md
```

The llms.txt follows the [standard format](https://llmstxt.org):
- H1 title with blockquote summary
- H2 sections grouping related modules
- Links to detailed per-module documentation in `docs/`

Each module doc includes:
- Import statement
- All public functions with signatures
- Usage examples extracted from real commands

## Available Core Modules

For detailed API documentation with signatures and examples, see `$IW_CORE_DIR/../llms.txt`.

**Quick reference:**

| Module | Purpose | Docs |
|--------|---------|------|
| Output | Console output (info, error, success, section, keyValue) | [docs/Output.md](docs/Output.md) |
| Config | Configuration types (ProjectConfiguration, IssueTrackerType) | [docs/Config.md](docs/Config.md) |
| ConfigRepository | Read/write config files | [docs/ConfigRepository.md](docs/ConfigRepository.md) |
| IssueId | Parse and validate issue IDs, extract from branch names | [docs/IssueId.md](docs/IssueId.md) |
| Issue | Issue entity and IssueTracker trait | [docs/Issue.md](docs/Issue.md) |
| Git | GitAdapter - branch operations, remote info, status | [docs/Git.md](docs/Git.md) |
| GitWorktree | GitWorktreeAdapter - worktree create/remove/list | [docs/GitWorktree.md](docs/GitWorktree.md) |
| Process | ProcessAdapter - shell command execution | [docs/Process.md](docs/Process.md) |
| Prompt | Interactive prompts (confirm, ask) | [docs/Prompt.md](docs/Prompt.md) |
| GitHubClient | GitHub API via `gh` CLI | [docs/GitHubClient.md](docs/GitHubClient.md) |
| LinearClient | Linear API client | [docs/LinearClient.md](docs/LinearClient.md) |
| YouTrackClient | YouTrack API client | [docs/YouTrackClient.md](docs/YouTrackClient.md) |

## Example: Simple Command

```scala
// PURPOSE: Show current issue context
// USAGE: iw ./context

import iw.core.*

@main def context(args: String*): Unit =
  val cwd = os.pwd

  GitAdapter.getCurrentBranch(cwd) match
    case Left(err) =>
      Output.error(s"Not in a git repo: $err")
      sys.exit(1)
    case Right(branch) =>
      Output.keyValue("Branch", branch)

      IssueId.fromBranch(branch) match
        case Left(_) =>
          Output.warning("Not on an issue branch")
        case Right(issueId) =>
          Output.keyValue("Issue", issueId.value)
          issueId.team.foreach(t => Output.keyValue("Team", t))
```

## Example: Command with Config

```scala
// PURPOSE: List all worktrees for this project
// USAGE: iw ./list-worktrees

import iw.core.*

@main def `list-worktrees`(args: String*): Unit =
  val cwd = os.pwd
  val configPath = cwd / Constants.Paths.IwDir / "config.conf"

  // Read project config
  val config = ConfigFileRepository.read(configPath) match
    case None =>
      Output.error("No iw config found. Run './iw init' first.")
      sys.exit(1)
    case Some(c) => c

  Output.section(s"Worktrees for ${config.projectName}")

  GitWorktreeAdapter.listWorktrees(cwd) match
    case Left(err) =>
      Output.error(err)
      sys.exit(1)
    case Right(worktrees) =>
      worktrees.foreach { wt =>
        Output.info(s"  ${wt.path} [${wt.branch}]")
      }
```

## Output Conventions

Use `Output` methods consistently:
- `Output.info(msg)` - General information
- `Output.error(msg)` - Error messages (prints to stderr)
- `Output.success(msg)` - Success with checkmark (✓)
- `Output.warning(msg)` - Warning messages
- `Output.section(title)` - Section headers with formatting
- `Output.keyValue(key, value)` - Formatted key-value pairs

## Testing Your Script

```bash
# Run directly
./iw ./my-command --flag value

# Or with scala-cli for debugging
scala-cli run my-script.scala $IW_CORE_DIR/*.scala -- --flag value
```

## Tips

1. **Read llms.txt first** - start with `cat $IW_CORE_DIR/../llms.txt` to see all available modules
2. **Check existing commands** for patterns: `cat $(dirname $(which iw))/../commands/*.scala`
3. **Use `Either` returns** - most adapters return `Either[String, T]` for error handling
4. **Keep it functional** - pure functions, effects at the edges
5. **Read module docs** - for detailed API, read `$IW_CORE_DIR/../docs/<Module>.md`
