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

The **ssr-html-interpreter slice is code-complete** and its output —
`uiform-gap-inventory.md` — is written. SSR browser HTML now exists:
`UIFormHtmlRenderer` (UIForm → scalatags, HTMX-enriched POST form),
`RequiredValidation` (shared, from the `optional` flag), and
`SsrFormScenario` hosting a rich form in the scenarios server with a full
GET/POST loop (conditions re-evaluate, repeated rows add/remove
server-side, errors render, serialized+reloaded declaration renders
byte-identical HTML). 55 tests across forms + scenarios, all green.
Next unblocked step: **core-hardening-spa-refold**, gated on Michal
reading the gap inventory (it proposes ADT-level dispositions).

## Log

- **2026-07-11 — Browser verification made repeatable.** Two artifacts:
  `webapp-knowledge/` (server start/stop, page selectors, HTMX wait
  strategies, gotchas — enables autonomous verification runs) and an
  automated browser suite `./mill formsScenarios.jvm.e2e` — Cucumber +
  Playwright via our own `e2e-testing` module (first in-repo consumer,
  dogfooding). Five scenarios, one per browser-only bug from the
  verification session; the suite starts the scenarios server in-process
  (port 8392), so no Mill daemon lock and no external server. The
  `e2e-testing` framework gained an optional `channel` launch config
  (bundled chromium can't resolve host libs on nix-profile machines;
  system Chrome via `channel = "chrome"` works). Details in
  `e2e-generation-summary.md`.

- **2026-07-11 — Real-browser e2e verification (Playwright) found and fixed
  four bugs the route tests missed.** Full journey now passes in Chromium:
  render → condition reveal via HTMX → blank-submit errors with resolved
  labels → fill (Czech diacritics) → add row → remove row keeping the other
  row's data → submit → success page with faithful data. Fixes: `novalidate`
  on the form (htmx halts ALL requests on HTML5 invalidity — checked
  `form.noValidate`, not `hx-validate="false"`); re-render trigger narrowed
  to committed-choice controls (outerHTML swap on text-field change wiped
  in-flight input; morph swaps are the slice-2+ path); UTF-8 charset in
  meta + Content-Type (browsers fell back to Latin-1 submission); POST body
  decoding via `QueryParams.decode` (zio-http's `asURLEncodedForm` merges
  duplicate names, corrupting `__items`); validator label resolution scoped
  to the form prefix like the renderers. Gap inventory gained a "found only
  by real-browser verification" section — browser e2e must be part of the
  SSR loop's regression suite.

- **2026-07-11 — ssr-html-interpreter slice done.** Characterization tests
  first (UIFormBuilder fold, wire codecs, FormR algebra — first tests in
  the lineage), then TDD: SSR renderer templated on `UIFormXMLRenderer`
  (form-level `hx-post`/`hx-trigger=change`/`hx-swap=outerHTML`, degrading
  to plain POST), `RequiredValidation` in forms/shared, `SsrFormScenario`
  with route-level integration tests plus a live-server curl check. Fixed
  the `formsScenarios` moduleDir bug (both platforms compiled zero
  sources; fixing it exposed a dead sbt-era `BuildInfo` reference).
  Builder changes along the way (all additive): validation errors now
  attach as `ErrorMessage` decorations; `Repeated` emits hidden `__items`
  fields so item state round-trips through HTML forms. Wrote
  `uiform-gap-inventory.md` — notable: Condition semantics drift across
  the four walkers (`IsValid` ×3 behaviors, `NonEmpty` blank-vs-presence),
  Repeated grouping erased by the fold, `Enum`/`Date` can't be required,
  buttons carry no intent. `ScenariosServer` honors `PORT` now.

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
