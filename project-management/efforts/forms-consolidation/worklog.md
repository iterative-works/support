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

The **core-hardening-spa-refold slice is running**
(`project-management/issues/iw-support-form-effort-3/tracker.md` is the
step-level state). Landed so far: shared `Condition.eval` +
`Repeated.instances` with all five walkers refolded, `FormData` as the
typed value currency with the SSR loop running on it, the declared
`Validation` vocabulary evaluating in `DeclaredValidation` on the SSR
POST path, `FieldKind` as the closed field-type dispatch index (FC-D6),
the full UIForm/ADT hardening from the gap inventory (Enum/Date
optionality, disabled/context decorations, button intent, UIRepeatedGroup,
form/section error slots, IdPath ids), and `TypedForm[A]` +
`InputSchema` relocation (the FormSchema salvage — typed declarations
erase to the plain Form, FormCodec captures FormData as a typed value),
and the czech-support extraction (FC-D3) — the Czech/CMI domain now
lives in its own in-repo CrossModule with packages unchanged, forms no
longer depends on email/paygate/filesMongo. Next per the tracker:
formsCore/formsHttp/FormComponents retirement + the package rename.

## Log

- **2026-07-12 — czech-support extraction (FC-D3).** The Czech/CMI
  domain left `forms` for the new `czech-support` CrossModule: ARES/VIES
  (services, endpoints, wire models), the submission chain
  (SubmissionService/DsSubmissionService/SubmitResult/Submission/
  SubmissionRepository + live REST impls and the client Endpoints/
  Codecs/User surface), the CMI resolvers (BaseValidationResolver,
  LiveFieldTypeResolver, BaseButtonHandler with `complete_ares`),
  `Enum.yesno`, and MongoConfig. Grep-first across both client repos
  settled the shape: every piece is live in at least one client and
  referenced by package name, so everything moved same-package — clients
  add one dependency at upgrade and keep compiling. The js Endpoints
  object moved whole because medeca passes the object itself into
  `BaseValidationResolver.layer`; ButtonHandler split instead (trait +
  empty stay as the generic seam). `Enum.yesno` became a top-level
  extension in package portaly.forms, red-green pinned through the same
  wildcard import medeca's declarations use. forms.jvm dropped email/
  paygate/filesMongo (zero in-repo use; both clients declare direct
  deps); czech-support.jvm now carries the sttp zio-json dependency the
  registry services previously borrowed from paygate transitively.
  Characterization tests pin yesno call sites, ARES address accessors,
  and the VIES EU country set. All gates green incl. browser e2e 5/5.

- **2026-07-12 — TypedForm lands; the FormSchema salvage is done.**
  `InputSchema` moved verbatim to `ui/forms/shared` (same package —
  laminar.forms and medeca client code compile untouched; it was
  platform-neutral as the plan verified), and `forms` now declares its
  `uiForms` dependency explicitly instead of riding autocomplete's.
  `TypedForm[A]` is FormSchema's applicative algebra (field/zip/bimap/
  section) emitting plain `SectionSegment`s; `form(id, version)` erases
  to the ordinary `Form` plus a root-bound `FormCodec[A]` — decode
  accumulates errors across fields, blank counts as missing (matching
  DeclaredValidation), encode writes renderable FormData. Decode
  semantics deliberately follow `FieldBuilder.Input`, not
  `FormSchema.Control`, whose decodeOptional path ignores the posted
  value and always fails required fields — the salvage takes the proven
  half. Pinned: the erased form is case-class-equal to a hand-written
  one and runs the standard builder fold and declared validation, so
  the typed layer cannot fork the core. All gates green incl. browser
  e2e 5/5 and full-repo compile.

- **2026-07-12 — UIForm/ADT hardening closes the whole gap inventory.**
  Seven gaps, five green commits, each TDD'd with wire pins. Enum and
  Date declare required-ness (`optional` defaults TRUE to preserve stored
  never-required behavior — deliberately unlike Field, like Repeated);
  disabled/context ride the fold as decorations (SSR renders disabled
  inputs with a hidden mirror so the POST loop keeps their state);
  `ButtonIntent` Submit/ServerAction/ClientAction wire-pinned with legacy
  = ServerAction, UIButton's dead `buttonType` string replaced, declared
  Submit suppresses the SSR chrome; `UIRepeatedGroup` keeps row
  boundaries with everything needed to derive add/remove (builder stops
  emitting sibling `__items` hidden fields — SSR emits per row, XML/PDF
  drops the chrome, restoring pre-slice output), group errors render;
  `UIForm.errors` + populated section decorations give form/section
  errors a place; `UIFormId = IdPath` ends dash-split path
  reconstruction (pinned red-first by a dashed Display id). Client
  migration notes recorded in the tracker — biggest: custom UIForm
  walkers with `case _` fallbacks silently drop repeated content until
  they learn UIRepeatedGroup. All gates green incl. browser e2e 5/5 and
  a full-repo compile.

- **2026-07-12 — FieldKind closed vocabulary dispatches the interpreters.**
  `enum FieldKind` + `Custom(id)` with `of`/`wireId` as the hand-written
  codec, pinned over the full client-audit id list (incl. the
  `medeca:mds_mdt` drift id staying visibly Custom). DEVIATION from
  FC-D6's letter, matching the plan's core-shape text: `FieldType.id`
  stays the wire/stored truth — clients pattern-match the string and
  `text`/`string` alias in production, so a kind-first case class can't
  re-encode byte-exactly; `FieldType.kind` derives instead and
  `FieldType(FieldKind.Email)` constructs typed. Exhaustive dispatch in
  the SSR renderer (base:email/base:phone now render email/tel inputs),
  XML-renderer number normalization and JS `FieldTypeResolver.empty`;
  `LiveFieldTypeResolver` refolds at SPA-refold (cmi:* arms leave at
  czech-support). All green incl. browser e2e 5/5.

- **2026-07-12 — Declared Validation vocabulary evaluates end to end.**
  `ValidationRule`/`ValidationState` relocated to forms/shared
  (correction to the plan: ValidationState carried an airstream import
  for a deprecated zero-caller helper — preserved verbatim JS-side as
  `ReactiveValidationState`). `enum Validation` (Required/Email/Pattern/
  MinLength/MaxLength/Rule) rides on `Field.validations = Nil` —
  wire-pinned both ways (legacy JSON decodes to Nil, new shape frozen in
  the characterization spec). RequiredValidation renamed
  `DeclaredValidation`; pure `Validation.check` runs the format checks
  (blank values pass — emptiness is Required's concern; Rule binds at
  the edges later). SSR scenario declares Email and the POST loop
  re-renders its error; browser e2e stays 5/5.

- **2026-07-12 — FormData lands; SSR loop runs on it.** `FormData`
  (`Map[AbsolutePath, List[FieldValue]]`, `Text`/`File`) implements
  FormState, keeps `parse` posted-body ingestion and the `__items`
  convention; `overrideWith` actually overrides — pinned red-green
  (FormR's was byte-identical to combineWith; zero callers anywhere,
  client repos included). SsrFormScenario ingests, rewrites and dumps
  through FormData; the success page no longer stringifies file values
  through a wire codec. Remaining FormR uses are edges only (repository
  stack, FormRJsonEncoder for medeca, PDF interpreters, persistence
  services) rebinding per FC-D2; SPA internals move at the SPA-refold
  step. All green incl. browser e2e 5/5.

- **2026-07-11 — core-hardening-spa-refold opened; condition/repeat semantics
  unified.** Slice card + tracker at
  `project-management/issues/iw-support-form-effort-3/`. Client-repo grep
  settled the inventory's open semantics: `IsValid` stays (production gates
  `complete_ares` on it), `NonEmpty` means blank-filtering (production
  declares `default: ""` behind a NonEmpty gate). Landed: shared pure
  `Condition.eval` + `Condition.references` with all five walkers evaluating
  through it (UIFormBuilder, RequiredValidation, LiveHtmlInterpreter via
  reactive snapshot, ReadOnlyHtmlInterpreter, FormRJsonEncoder), and shared
  `Repeated.instances` replacing three private __items expansions. Behavior
  changes for the migration guide: NonEmpty no longer counts blank values,
  empty AnyOf/AllOf no longer crash, read-only renderer no longer drops
  repeated rows past the first. Findings for Michal: FormRJsonEncoder is
  NOT zero-consumer (medeca-modul-poptavky uses it — retirement item 5
  re-sequenced to downstream); no generic visitor fold extracted (leaf
  rendering is the only per-walker code left) — deliberate narrowing of the
  plan letter, revisit at the SPA-refold step.

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
