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

The forms-plan analysis is done and written up in `forms-plan.md` (same
directory) — **awaiting Michal's sign-off**. The proposed decision: the
portaly spine wins (`FormSegment` → `UIFormBuilder` → `UIForm` →
`Interpreter`), renamed `works.iterative.forms`; `FormSchema[A]`'s
type-safety folds in as `TypedForm[A]` compiling down to the spine;
`laminar.forms`, `tailwind.form`, and the `formsCore` mini-lineage retire.
The plan carries a per-item retirement sign-off list and nine open questions.
Once signed off, decisions land as FC-D1/FC-D2 and `ssr-html-interpreter`
becomes the active slice (revised to a zero-ADT-change first cut per the
plan's slice sequence).

## Log

- **2026-07-11 — forms-plan analysis run (ultracode).** Deliberately ran
  outside the strict slice process to build momentum. Multi-agent workflow:
  5 lineage mappers → 3 opposing architecture proposals → 3-lens judge panel
  → 6 adversarial verifiers (17 agents, all claims source-checked). Judges
  voted 3–0 for the portaly spine; all six verifications of the winner's
  load-bearing claims returned "holds". Key corrections to the framing: the
  live SPA path bypasses UIForm entirely (walker duplication with real drift
  — `IsValid` hardcoded in one walker); there's a *fourth* mini-lineage
  (`formsCore`/`formsHttp`/`uiCore FormComponents[T]`) squatting on the
  `works.iterative.forms` package; `formsScenarios` compiles zero sources
  (build bug); `FormR.overrideWith` is byte-identical to `combineWith`
  (latent bug); zio-json 0.7.44 applies defaults for missing fields, so the
  planned `Field.validations` extension is wire-compatible with stored
  definitions. Wrote `forms-plan.md` for sign-off.

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
