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

The forms-plan slice is **done and signed off in full** (FC-D1…FC-D7 in
`decisions.md`; plan in `forms-plan.md`). Portaly spine wins; typed layer
grafts on as `TypedForm[A]`; retirement list approved; one-time converters;
czech-support extras module; scalatags+HTMX SSR; closed `FieldKind` +
`Custom(id)` (audit done); submission-time message snapshots for faithful
rebuild; default `uiform.xsl` will ship. Next unblocked step:
**ssr-html-interpreter** slice — zero ADT changes: characterization tests
(first tests in the lineage; needs test sub-modules added to build.mill),
UIForm→scalatags renderer templated on `UIFormXMLRenderer`, GET/POST loop
with HTMX fragment swaps, hosted in the scenarios server (fix the
formsScenarios moduleDir bug), serialize/reload dynamism proof riding along.

## Log

- **2026-07-11 — Plan signed off in full (FC-D1…FC-D7).** Michal approved
  retirement + converters + czech-support + scalatags/HTMX + default xsl in
  round one; rounds two settled the last two questions: field-type
  vocabulary = closed `FieldKind`+`Custom(id)` with wire-stable codec
  (client-repo audit run: generic core + `base:*` + parameterized numbers;
  `cmi:*`/`medeca:*` stay Custom; found real vocabulary drift `mds_mdt`),
  and faithful rebuild = submission-time message snapshot (drafts excluded,
  skin out of scope, retroactive provenance-stamped enrichment of old
  submissions). Found client `uiform.xsl` reference implementations for the
  shipped default (cmi-portaly, medeca-modul-poptavky). Also spotted a
  form-editor prototype in cmi-portaly (`FormEditorScenario`,
  `forms:fieldType`/`forms:condition`) — relevant to the parked
  form-editor horizon. Marked forms-plan done in the map.

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
