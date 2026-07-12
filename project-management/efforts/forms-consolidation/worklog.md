<!-- PURPOSE: Living thread of the forms-consolidation effort — READ FIRST when resuming this work -->
<!-- PURPOSE: Where we are + dated log; the conversation is disposable, this file is durable -->

---
title: Forms Consolidation — Worklog
created: 2026-07-11
status: living — READ THIS FIRST when resuming this work
companion: intent.md, decisions.md, map.md (same directory)
---

# Forms Consolidation — Worklog

## Where we are right now

Effort just framed (2026-07-11). The Standing Intent is written: one
declarative form core with a family of implementations (SSR HTML, SPA HTML,
data capture, form+data serialize/rebuild, PDF), closing on a rich form driven
multiple ways plus a client migration guide. Three candidate slices are
seeded; the spine decision (which of `portaly.forms` vs `FormSchema[A]` wins,
what gets reconciled/retired) is deliberately deferred to the first slice.
Next unblocked step: carve the first slice with `sl-define "forms-plan"`.

## Log

- **2026-07-11 — Effort framed.** Ran sl-frame. Grounded against the codebase:
  found three form lineages — `portaly.forms` (mature, declarative
  `FormSegment` → `UIForm` → `Interpreter`, renders SPA + PDF, no browser-HTML
  SSR), `ui...laminar.forms` `FormSchema[A]` (elegant, type-safe, SPA-only,
  unfinished), and deprecated imperative `tailwind.form` widgets. Key finding:
  "the SPA declarative one" is under-determined — two rival declarative ADTs
  with no bridge, and SSR genuinely does not exist. Framed the goal as
  consolidation onto one core with many implementations; deletion/migration of
  consumers and a form editor deferred as downstream/future horizons (the
  editor will get its own intent). Trust boundary: rival lineages migrated or
  retired only with explicit sign-off, plan approved up front. Wrote intent +
  seeded map/decisions/worklog. Still open for reaction: whether the form
  editor should be folded in under a vision doc rather than deferred.
