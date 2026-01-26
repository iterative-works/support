---
name: iw-cli-ops
description: |
  Fetch issue descriptions and manage development worktrees for isolated work.

  Use when:
  - User asks to read, plan, or discuss an issue (e.g., "read the issue", "let's plan SUPP-48")
  - Need issue description, title, status, or details from any tracker
  - Starting work on an issue (creates isolated worktree + tmux session)
  - Opening or switching to existing worktrees
  - Removing completed worktrees
  - Managing the dashboard server or viewing worktree status
---

# iw-cli Operations

iw-cli is a project-local CLI tool for issue tracking and worktree management. The script is at
`./iw` in the project root and must be invoked as `./iw`, not `iw` (it's not in PATH).

## Project Configuration

This project uses:
- **Tracker**: GitHub
- **Repository**: iterative-works/support
- **Team Prefix**: SUPP (issue IDs like SUPP-123)

## Getting Issue Information

### Fetch Issue Details

```bash
# Fetch specific issue by ID
./iw issue SUPP-123

# Auto-detect from current branch (if on SUPP-123 branch)
./iw issue
```

**Output includes:** Title, status, assignee, and full description.

### Create New Issue

```bash
./iw issue create --title "Bug: something broke" --description "Details here..."
```

## Worktree Management

iw-cli creates isolated git worktrees with associated tmux sessions for each issue.

### Start Work on an Issue

```bash
./iw start SUPP-123
```

This:
1. Creates git worktree at `../iw-support-SUPP-123/`
2. Creates or checks out branch `SUPP-123`
3. Creates tmux session named `iw-support-SUPP-123`
4. Registers with dashboard
5. Attaches/switches to the session

### Open Existing Worktree

```bash
# Open specific worktree
./iw open SUPP-123

# Auto-detect from current branch
./iw open
```

### Remove Completed Worktree

```bash
# Remove worktree (prompts if uncommitted changes)
./iw rm SUPP-123

# Force remove without prompt
./iw rm SUPP-123 --force
```

**Safety checks:**
- Warns about uncommitted changes
- Prevents removal if you're inside the worktree
- Does NOT delete the git branch (manual cleanup needed)

### Register Current Worktree

```bash
# Register current worktree with dashboard (auto-detects issue from branch)
./iw register
```

## Dashboard Server

The dashboard provides a web UI for viewing active worktrees.

### Server Management

```bash
# Start background server
./iw server start

# Check server status
./iw server status

# Stop server
./iw server stop
```

### Interactive Dashboard

```bash
# Start dashboard and open browser
./iw dashboard

# Development mode (isolated, random port)
./iw dashboard --dev

# Load sample data for testing
./iw dashboard --sample-data
```

## Other Useful Commands

### Check System Setup

```bash
./iw doctor
```

Validates: git repo, config file, CLI tools (gh/glab), authentication status.

### Run Project Tests

```bash
# All tests
./iw test

# Unit tests only (Scala)
./iw test unit

# End-to-end tests only (BATS)
./iw test e2e
```

### Submit Feedback

```bash
./iw feedback "Feature request: add X" --type feature
./iw feedback "Bug: Y is broken" --description "Steps to reproduce..." --type bug
```

### Version Info

```bash
./iw version
./iw version --verbose  # includes OS, Java details
```

## Command Reference

| Command | Usage | Purpose |
|---------|-------|---------|
| `issue` | `./iw issue [ID]` | Fetch/display issue from tracker |
| `issue create` | `./iw issue create --title "..."` | Create new issue |
| `start` | `./iw start <ID>` | Create worktree + tmux session |
| `open` | `./iw open [ID]` | Open existing worktree session |
| `rm` | `./iw rm <ID> [--force]` | Remove worktree + session |
| `register` | `./iw register` | Register worktree with dashboard |
| `server` | `./iw server <start\|stop\|status>` | Manage background server |
| `dashboard` | `./iw dashboard` | Start interactive dashboard |
| `doctor` | `./iw doctor` | Check system dependencies |
| `test` | `./iw test [unit\|e2e]` | Run project tests |
| `feedback` | `./iw feedback "title"` | Submit feedback to iw-cli |
| `version` | `./iw version` | Show version info |
| `init` | `./iw init` | Initialize iw config (already done for this project) |

## Issue ID Inference

Many commands auto-detect the issue ID from the current git branch:
- Branch `SUPP-123` → issue ID `SUPP-123`
- Branch `SUPP-123-fix-bug` → issue ID `SUPP-123`
- Branch `feature/SUPP-456` → issue ID `SUPP-456`

## Environment Variables

| Variable | Purpose |
|----------|---------|
| `IW_DEBUG` | Enable debug logging when set to any value |
| `YOUTRACK_TOKEN` | API token for YouTrack tracker |
| `LINEAR_TOKEN` | API token for Linear tracker |

GitHub/GitLab authentication is handled via `gh auth` / `glab auth`.

## Getting More Details

```bash
# Describe any command
./iw --describe issue
./iw --describe start
```
