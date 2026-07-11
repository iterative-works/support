<!-- PURPOSE: Running state + dated log for the release-and-ci effort — READ FIRST on resume -->
<!-- PURPOSE: Update after every meaningful decision or landed increment -->

# Release & CI — Worklog

## Where we are

The effort's three founding increments have **all landed on `main`**: CI + git
hooks (SUPP-9), dual-publish to Nexus + GitHub Packages (SUPP-24), and the
testcontainers pin (SUPP-28). The intent's three goal bullets are met and the
invariants hold as of this writing. The thread is effectively complete; it
stays open as the home for the distribution/quality axis, with two unscoped
candidates in the map.

## Log

### 2026-07-11 — Effort created (scaffolding)

Introduced the efforts convention into `iw-support` and modelled the existing,
already-completed release/CI/quality work as the first effort. Back-filled from
the three merged issues:

- Intent, invariants, and fit test written from SUPP-24 / SUPP-9 / SUPP-28.
- Decisions RCI-D1..D4 recorded with `Propagates to:` boxes checked (the work
  is landed).
- Map lists all three as **Done**; two candidates seeded (plugin-resolvability
  follow-up, release automation/changelog).

Primary purpose of this pass is scaffolding — having the effort structure
present in the repo — not new work. Next real increment, if any, comes from
promoting a candidate.
