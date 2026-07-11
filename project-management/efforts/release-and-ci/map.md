<!-- PURPOSE: Work map for the release-and-ci effort — items reference issues, never restate them -->
<!-- PURPOSE: Candidates / active / done / parked; each item carries type and issue reference -->

# Release & CI — Map

Items **reference** `project-management/issues/<id>/`; they never restate issue
content. Metadata: `type:` (ag | wf | dx | sl | manual), optional `after:`,
optional `repo:`.

## Done

- **CI + git hooks** — `type: ag` — issue: [SUPP-9](../../issues/SUPP-9/)
  GitHub Actions validates JVM + JS compilation, tests, and formatting on PRs;
  git hooks give the same feedback pre-push. Realizes the second goal bullet.
- **Dual-publish (Nexus + GitHub Packages)** — `type: wf` — issue:
  [SUPP-24](../../issues/SUPP-24/) — `after: supp-9`
  Realizes RCI-D1..D3 and the first goal bullet.
- **Pin testcontainers for Docker 25+** — `type: dx` — issue:
  [SUPP-28](../../issues/SUPP-28/)
  Realizes RCI-D4 and the third goal bullet (no silent skips). Resolved in
  `ec69743f`.

## Active

_(none)_

## Candidates

- **mill-iw-support plugin resolvability follow-up** — `type: dx`
  SUPP-24 Stage A left open whether `mill-iw-support` resolves from public Mill
  plugin sources; if the Nexus `//| repositories:` directive had to be
  restored, a plugin-migration follow-up belongs here. Confirm current state
  before promoting.
- **Release automation / changelog** — `type: wf`
  Tag-driven releases exist (`v*`); a generated changelog / release-notes step
  is a plausible next increment on the same distribution axis. Not yet scoped.

## Parked

_(none)_
