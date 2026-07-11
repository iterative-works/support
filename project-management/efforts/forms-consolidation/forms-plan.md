<!-- PURPOSE: The forms-plan deliverable — the consolidation decision and plan for the forms subsystem, written for team sign-off -->
<!-- PURPOSE: Settles the spine question the intent left open; the retirement list and open questions below are the sign-off surface -->

# Forms Consolidation — Decision & Plan

Status: **accepted 2026-07-11** for the spine, retirement list, wire posture,
czech-support extraction, scalatags+HTMX SSR substrate, module layout, and
default `uiform.xsl` (decisions FC-D1…FC-D5). Still in discussion: labels &
faithful rebuild (open question 3) and the field-type vocabulary shape (open
question 4).

How this was produced: five parallel code mappers over the lineages, three
independent architecture proposals under opposing stances (portaly-spine,
formschema-spine, neutral-seam), a three-lens judge panel (maintainer, client
project, delivery risk), and six adversarial verifiers checking the winner's
load-bearing claims against source. The judges voted **3–0 for the portaly
spine**; every verified claim below carries a file reference.

## The decision

**Consolidate onto the `portaly.forms` spine — `FormSegment` → `UIFormBuilder`
→ `UIForm` → `Interpreter[I,O]` — renamed to `works.iterative.forms`.**
`FormSchema[A]`'s type-safety is folded in as an author-side typed layer that
compiles down to the spine; it does not remain a rival core. The
`laminar.forms` and `tailwind.form` lineages retire, as does the skeletal
fourth mini-lineage (`forms/core` + `forms/http` + `ui/core FormComponents[T]`)
that currently squats on the `works.iterative.forms` package name.

### Why the portaly spine wins

1. **It is the only lineage that satisfies the intent's structural
   requirements today.** Its `Form` is a pure, closure-free ADT with working
   zio-json codecs and Tapir schemas (`FormPersistenceCodecs.scala:48-64`,
   verified `DeriveJsonCodec.gen` straight off the ADT), stored versioned in
   MariaDB (`MariaDBFormReadRepository`) and rebuilt at runtime by a live
   custom element (`BaseIWFormElement.scala:28-32` fetches definition JSON and
   renders it). Serialize/rebuild and dynamism — the intent's hardest targets —
   are already proven on this spine.

2. **`FormSchema[A]` cannot be the spine.** `FormSchema.Control` captures
   closures (`decode: A => String`, `validation: Option[String] =>
   Validated[A]` — `FormSchema.scala:19-25`), making it unserializable by
   construction, which structurally kills serialize/rebuild, runtime-loaded
   forms, and the future form editor. It is JS-only, its interpreter handles
   only text inputs, there is no derivation (the `Form.derive` test is
   commented out), and its own code admits the merge stalled
   (`FormBuilderModule.scala:68`: "replace Form builder").

3. **Everything proven multi-target sits below the declaration ADT and is
   type-agnostic.** `UIForm`/`FormState`/`IdPath`, the pure `ZPure` fold
   (`UIFormBuilder`, cross-compiled, could run on the JVM today), `Condition`,
   the FOP pipeline, the Laminar `FormPart` machinery — all survive intact.
   What FormSchema has that portaly lacks (typed `A` end-to-end) is exactly
   the part that does *not* need to be the spine: typed codecs are code, not
   data, and only the data half must serialize.

4. **Migration cost decides the tie-breaks.** A portaly-style production app
   (DB-loaded definitions, PDF) migrates via package rename + additive ADT
   extension + converters; under the formschema spine the same app would ride
   a rewritten wire format and a ported 663-line SPA interpreter — re-running
   the half-merge that already stalled once.

### Corrected mental model (mapper finding worth internalizing)

The intent describes UIForm as the shared interpreter seam. In reality **the
live SPA path bypasses UIForm**: `LiveHtmlInterpreter` walks the `FormSegment`
ADT directly (`LiveHtmlInterpreter.scala:95-147`, own condition evaluator at
`:501-539`). UIForm is consumed only by the PDF path and a read-only renderer;
the editable `UIFormRenderer` over UIForm is a dead stub. Drift between the
duplicated walkers is not hypothetical — `IsValid` is hardcoded `true` in
`FormRJsonEncoder` while evaluated for real elsewhere. The plan therefore does
**not** force the SPA onto static UIForm; it forces all walkers onto one
shared fold and one pure condition evaluator, which is the honest anti-drift
mechanism (verified as the right call by the adversarial pass).

## The consolidated shape

One pure core in `forms/shared`, package `works.iterative.forms` (FCIS: the
core is data + pure folds; interpreters are the effectful edges).

1. **Declaration ADT stays** (`Form`/`Section`/`Field`/`File`/`Date`/
   `Display`/`Button`/`Enum`/`ShowIf`/`Repeated` + `Condition`), extended
   additively:
   - `Field.validations: List[Validation] = Nil` where `enum Validation
     { Required; Email; Pattern(regex); MinLength(n); MaxLength(n);
     Rule(id, params) }` — closed common cases, open `Rule` for client/async
     extension (VIES etc. register against `Rule` ids).
   - A canonical **field-type index**: `FieldType` stays an open string (no
     wire break), but interpreters dispatch through a shared index, and a
     **conformance test kit** runs every indexed field type and validation
     rule through every interpreter — "added once, every implementation gets
     it" becomes a failing test instead of a hope. (A closed `FieldKind` enum
     + `Custom(id)` is stronger; adopting it is gated on the client
     field-type audit — open question 4.)

2. **`FormData` replaces both `FormR` and `FormContent`**:
   `Map[AbsolutePath, List[FieldValue]]`, `enum FieldValue { Text(String);
   File(FileRef) }`. Implements the existing `FormState` read interface, so
   `UIFormBuilder` is untouched. Kills the acknowledged-wrong `Any`, the
   `overrideWith` bug (`FormR.scala:118-126` is byte-identical to
   `combineWith` — it never overrides), the lossy `toString` JSON encoding of
   file values, and `FormContent`'s magic `___form_id` keys (a `FormBundle`
   envelope carries identity instead). Keeps `FormR.parse`'s
   `Map[String, Seq[String]]` ingestion and the `__items` repeat convention
   so SSR/SPA data and e2e selectors stay compatible.

3. **One traversal.** Segment dispatch and `Condition.eval` extract into a
   shared fold; `UIFormBuilder` becomes its canonical instance producing
   UIForm for static targets; the SPA refolds onto the same dispatch and the
   same pure condition evaluator (wrapped reactively). Fixes ride along as
   red-green steps under characterization tests: empty `AnyOf`/`AllOf` reduce
   crash, `NonEmpty`-on-`Option`-presence semantics, `Repeated` empty-elems
   head crash.

4. **Validation moves to shared.** `ValidationRule`/`ValidationState` relocate
   verbatim from `forms/js` (verified: no Laminar imports), plus a pure
   compiler from declared `Validation` to rules — so SSR POST validation (JVM)
   and reactive validation (JS) run the same functions. Async/remote rules
   stay at the edges behind a registry.

5. **Type-safety folds in as `TypedForm[A]`** — the FormSchema salvage:
   `InputSchema[A]` moves to shared verbatim (verified platform-neutral);
   applicative builders (`field`/`zip`/`bimap`, FormSchema's exact algebra)
   emit plain `FormSegment`s plus a `FormCodec[A]` (`FormData ⇄ Validated[A]`
   with accumulating errors). **Interpreters only ever see the erased
   `Form`** — a runtime-loaded JSON form (no codec) and a compile-time
   `TypedForm[A]` run through identical machinery, so the typed layer cannot
   fork the core. Mirror-based derivation is explicitly out of scope (YAGNI;
   open question 8).

### The five implementations

| Target | Status | Approach |
|---|---|---|
| SSR HTML | **new** (verified: zero server HTML rendering exists) | JVM renderer walking UIForm exactly as `UIFormXMLRenderer` (~150 lines, the proven template) walks it for XML; emits `toHtmlId`/`toHtmlName` so posted bodies round-trip and Playwright steps work. GET → build → render; POST → `FormData.parse` → validate → re-render with error decorations. SSR semantics spelled out, not discovered: conditions evaluate-at-render with POST-cycle re-evaluation; repeated-group add/remove are ordinary submit buttons rewriting `__items`; async rules run server-side in the POST handler. |
| SPA HTML | exists | `LiveHtmlInterpreter` stays the live target, refolded onto the shared dispatch/evaluator/index. `FormPart` algebra, `FormCtx`, `Components` theming, draft autosave all survive. CMI/Czech hardwiring extracts to client repo or a czech extras module. |
| Data capture | mostly exists | `FormData` is the single currency on both platforms; typed capture via `FormCodec.decode` gives SSR handlers and SPA observers a real `A` with accumulated errors. |
| Serialize/rebuild | compose existing parts | `FormBundle(form, data, messages)` — one zio-json artifact: proven `JsonCodec[Form]` + new lossless `FormData` codec + a resolved-message snapshot (labels live in `MessageCatalogue`, not the ADT — without a snapshot a rebuilt form renders raw keys; open question 3). Close gate defines "faithful" operationally: byte-equal render under the bundle's referenced/snapshotted catalogue. |
| PDF | exists, on the spine already | FOP path mechanically unchanged; inherits every core addition because it consumes UIForm. Extracted to its own `formsPdf` module so FOP/scala-xml stop taxing every consumer; `forms.jvm` also sheds email/paygate/filesMongo (they exist only for `SubmitResult` payment fields and a Mongo shim). `ui:*` XML vocabulary stays byte-stable for invisible client XSLT. |

### Package and modules

`portaly.forms` → `works.iterative.forms` everywhere (85 files whose
directories already say `works/iterative/forms`; mechanical, wire-format
unaffected). Prerequisite: retiring `formsCore`, which occupies the package
with unrelated types. The render-neutral model (`works.iterative.ui.model.forms`)
**stays in `uiForms/shared` for now** — verified not to be a hard cycle, but
moving it is downstream-client import churn for zero capability; it merges
into the core as post-migration cleanup. `forms` gets an explicit `uiForms`
moduleDep (today it arrives only transitively via autocomplete —
`build.mill:937/:947` — fragile and undeclared). New satellite modules:
`formsPdf`; `formsDb` optional.

## What retires (the sign-off list)

Nothing is deleted silently; items marked *(downstream)* execute only in the
deferred consumer-migration horizon, kept compiling until then.

1. **`tailwind.form`** (`ui/js/.../components/tailwind/form/*`, 608 lines) —
   deleted outright. Only in-repo consumer is an unregistered scenario in a
   showcase that crashes at startup (`ScenarioMain` has `pattern = ???`).
   Downstream-client grep before deletion.
2. **`laminar.forms`** (`ui/forms/js`) — `Form[A]` GADT, both
   `FormBuilderModule` builders, `FormSchema[A]` as ADT, `FieldBuilder`,
   `FormUIFactory` (no implementation exists anywhere), `Validations`,
   vacuous tests, formpage module. `InputSchema` is salvaged to shared first.
   *(downstream — autocomplete.js consumes `FieldId`/`FieldDescriptor`/
   `FormComponent`/`FieldBuilder` and must be rebound before deletion.)*
3. **Fourth mini-lineage** — `formsCore` (`FormContext`/`FormErrors`/
   `FormField`), `formsHttp` (http4s UrlForm bridge), `ui/core
   FormComponents[T]`, empty `ScalatagsFormComponents` — retired to free the
   package name; the real SSR interpreter supersedes its role. `uiCore`
   depends on `formsCore` — client usage check required.
4. **Portaly data model** — `FormR` and `FormContent` replaced by `FormData`;
   one-time converters for persisted data, no silent compat decoders.
5. **Portaly dead/broken code** — `UIFormRenderer` stub,
   `ReadOnlyHtmlInterpreter` (duplicate walker), `FormRJsonEncoder` (zero
   consumers, crash paths), `FormCodecs` experiment, `Mutation.scala`, dead
   `PdfInterpreter` trait, `MongoConfig` shim.
6. **Client-domain leaks out of the library** — `cmi:*` field/section/rule
   hardwiring in `Base*` classes, `complete_ares` button handler,
   `Submission` aggregate, `DsSubmissionService`, `SubmitResult.paymentUrl/
   redirectUrl`, `Enum.yesno("ano"/"ne")` — destination per open question 5;
   ARES/VIES stay as optional czech-registry extras.
7. **Scenario debris** — broken Laminar `ScenarioMain` showcase, sbt-era
   leftovers; the `formsScenarios` build bug (moduleDir misses the `jvm`/`js`
   segment, so it compiles **zero sources** — verified) is *fixed*, not
   retired, because `FormCustomElementScenario` seeds the rich proof form.

## Wire compatibility posture

- **Stored form definitions**: verified that zio-json 0.7.44 applies default
  parameter values for missing fields — `Field.validations: … = Nil` decodes
  existing `form_descriptors` rows unchanged. The ADT extension is additive;
  no migration expected. Characterization tests pin this before the change.
- **Drafts** (`formr.v1+json`) and **submissions** (`FormContent` JSON): new
  encoding `formdata.v1+json` with a one-time offline converter, rehearsed
  against a production dump before any client migrates. No runtime fallback
  decoders without explicit approval (open question 2).
- **HTML field naming** (`toHtmlId`/`toHtmlName`, `__items`): unchanged —
  drafts, captured data, and e2e selectors stay compatible across SSR/SPA.
- **`ui:*` XML for PDF**: byte-stable; client XSLT we cannot see keeps working.
- **Fixed bugs are behavior changes** (`overrideWith`, empty-`AnyOf`,
  `NonEmpty`): each gets a named line in the migration guide.

## Slice sequence (proposed map update)

The judges unanimously grafted the neutral-seam slice discipline onto the
winner: prove the seam before touching the wire-format-bearing ADT, and put
the SPA refold second so seam insufficiency surfaces while the hardening
slice is still open.

1. **ssr-html-interpreter** — *zero ADT changes.* (a) Characterization tests
   for `UIFormBuilder` + `FormPersistenceCodecs` — the lineage's first tests
   ever (build change: `forms` has no test sub-module in `build.mill`; add
   using existing `BaseTests`/`BaseScalaJSTests`). (b) JVM
   `UIForm → HTML` renderer templated on `UIFormXMLRenderer`, GET/POST loop
   with `Required`-only validation computed from the `optional` flag.
   (c) Hosted in the zio-http scenarios server on a moderately rich form,
   fixing the `formsScenarios` build bug (the reaction surface must be real).
   (d) Dynamism proof rides along free: serialize the proof form via existing
   codecs, reload, render the same SSR page from the reloaded definition.
   Output: working SSR + a **verified inventory of UIForm gaps** (dropped
   `Disabled`/context decorations, String ids, `UIButton` handling) as
   evidence for slice 2's hardening.
2. **core-hardening + spa-refold** *(new item)* — `FormData`, declared
   `Validation` vocabulary, shared fold + `Condition.eval`, `TypedForm[A]` +
   `InputSchema` relocation, package rename; `LiveHtmlInterpreter` refolds
   onto the shared pieces; conformance test kit stands up.
3. **serialize-pdf-roundtrip** — `FormBundle` + label snapshot, PDF module
   extraction and cleanups, rich-form close-gate demo through all five
   targets, migration guide (seed of `docs/FORMS_GUIDE.md`) written by
   actually migrating the proof form.

## Verified risks

All six adversarial verifications returned **holds** on the plan's claims;
the live risks and their mitigations:

| Risk | Sev | Mitigation |
|---|---|---|
| SPA cannot consume static UIForm; two render paths persist and drift again | high | Shared fold + pure `Condition.eval` + index are the *only* home of dispatch/condition semantics; conformance kit makes drift a failing test. The fold must make normative calls where walkers currently disagree (enum→checkbox special case, `hidden` handling) — enumerated in slice 2. |
| Zero tests across the lineage; refactors fly blind | high | Characterization-tests-first is a standing rule of every slice; known bugs pinned before fixing. |
| Unknown downstream usage of retired pieces | medium | Per-item client-project grep before each deletion; persisted-data contracts get converters, never silent format changes. |
| autocomplete on the critical path both directions | medium | Model stays put; explicit `forms→uiForms` dep; `laminar.forms` deletion sequenced after autocomplete rebinding *(downstream)*. |
| Faithful rebuild needs labels the ADT doesn't carry | medium | `FormBundle` carries message snapshot + `catalogueRef`; "faithful" defined operationally at the close gate. |
| PDF/SSR not demonstrable in-repo (no `.xsl` anywhere; scenarios build bug) | medium | Slice 1 fixes the build bug; PDF slice ships a default `uiform.xsl` or renders the proof form through the example chain (open question 9). |

## Open questions — resolution status (2026-07-11)

1. **Retirement sign-off** — ✅ approved (FC-D2).
2. **Wire posture** — ✅ one-time offline converters approved (FC-D2).
3. **Labels & faithful rebuild** — 🔶 in discussion. Requirement stated by
   Michal: the form display must be reconstructable exactly as the client
   saw it when filling it out; evolving messages must never change the
   meaning of already-filled fields (currently unenforced). Leading option:
   server-side resolved-message snapshot at submission time (including
   resolved Display-block texts — consent texts are the highest-stakes
   case), with language + catalogueRef recorded as provenance.
4. **Field-type vocabulary** — 🔶 in discussion (open-string+index vs closed
   `FieldKind`+`Custom(id)` with a hand-written wire-stable codec).
5. **Czech/CMI remnants** — ✅ `czech-support` extras module in-repo first,
   off-repo later (FC-D3).
6. **SSR substrate** — ✅ scalatags with HTMX support (FC-D4): condition
   re-evaluation and repeat add/remove as HTMX fragment swaps, degrading to
   full-page POST without JS.
7. **Module layout** — ✅ as proposed (FC-D1).
8. **`TypedForm.derive`** — ✅ deferred as future possible development (FC-D1).
9. **PDF layout** — ✅ ship a default `uiform.xsl` genericized from the client
   implementations (reference: `~/Devel/projects/cmi-portaly/module/forms/jvm/
   src/main/resources/uiform.xsl` and `~/Devel/projects/medeca-modul-poptavky/
   .../uiform.xsl`, `uiform2.xsl`); branded wrappers stay app-side (FC-D5).

---

Decisions FC-D1…FC-D5 are recorded in `decisions.md`. `ssr-html-interpreter`
becomes the active slice once questions 3 and 4 close (neither blocks the
slice's zero-ADT-change first cut).
