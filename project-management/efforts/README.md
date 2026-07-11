<!-- PURPOSE: Defines the effort convention — the durable unit between vision and issue -->
<!-- PURPOSE: One directory per effort: intent + decisions + map + worklog; read on resume, updated as work lands -->

# Efforts

An **effort** is a long-running line of work with one goal, spanning many issues
(and possibly many repos). It is the durable thread: the conversation is
disposable, the issue artifacts are per-increment, the effort directory is what
you (human or agent) read first when resuming and update when something lands.

One directory per effort: `project-management/efforts/<slug>/` with four files:

| File | Role | Discipline |
|------|------|------------|
| `intent.md` | The value-axis compass — goal, before/after, invariants, fit | Amendable only by dated, explicit amendment (never casual edits) |
| `decisions.md` | Numbered decisions with stable IDs (`<PREFIX>-Dn`) | Append-only; change by superseding, never editing; each decision carries a `Propagates to:` checklist |
| `map.md` | Work items: candidates, active issues, done, parked | Items **reference** issues, never restate their content |
| `worklog.md` | Where we are + dated log | READ FIRST on resume; update after every meaningful decision or landed increment |

## Rules

- **No duplication.** Map items point at `project-management/issues/<id>/`
  (or a sibling repo's issue); analyses, phases, and review-state stay
  issue-scoped. The effort holds only what no single issue can: the goal, the
  cross-issue decisions, the ordering, the thread.
- **Decisions land on `main`.** A decision is a record, not an implementation —
  it must never sit only on a feature branch.
- **A decision is not done until propagated.** The `Propagates to:` checklist
  names every document that restates or depends on the decision; unchecked
  boxes are visible drift.
- **Map metadata:** each item carries `type:` (ag | wf | dx | sl | manual),
  optional `after:` (item slugs it depends on), optional `repo:` (for items
  landing in a sibling repository), and its issue reference once active.
  Items with no unmet `after:` and no shared surface are parallel-eligible.
- **Single-repo vs. multi-repo.** Efforts about developing `iw-support` itself
  stay project-local, here. Client-program efforts that merely *pull* an
  `iw-support` change do **not** live here — they live in that program's
  coordination repository and reference the `iw-support` issue via a map item's
  `repo:` field. This repo never becomes a global efforts store.

This convention assumes only markdown, git, and `project-management/` — no
stack, tracker, or forge specifics. It is decoupled from any particular
workflow tooling: an effort is just these four markdown files under version
control.
