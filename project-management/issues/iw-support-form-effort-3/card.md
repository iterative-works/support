---
slice: iw-support-form-effort-3
status: frozen
created: 2026-07-11
fit_target: "Michal runs the proof form in both the SSR scenario and the SPA scenario off one shared core and sees identical condition/validation behavior; drift is a failing conformance test"
---

# Slice Card — core-hardening-spa-refold

## Goal (the use)
I can declare a form once and trust every interpreter reads it the same way:
conditions, validation, and field types are evaluated by one shared core, so
SPA and SSR agree by construction — and when someone adds a field type or
rule, forgetting an interpreter is a failing test, not a production surprise.

## Fit test (close gate)
I run the scenarios server, drive the proof form through the SSR page and the
SPA custom-element page, and both behave identically on the behaviors the gap
inventory flagged (NonEmpty gating, condition re-evaluation, required
validation). I would build the next client form on this core:
- nothing the gap inventory promised for this slice is missing
- nothing foreign is present (no client-domain code left in the library)
- "do the walkers still disagree anywhere?" is answered by a green
  conformance kit, not by reading four evaluators

## Invariants (must hold)
- No wire-format break: stored form definitions decode unchanged (zio-json
  default-value pins), `ui:*` XML stays byte-stable, `toHtmlId`/`toHtmlName`/
  `__items` naming unchanged.
- Characterization tests before every refactor; each deliberate behavior
  change (NonEmpty blank-filtering, empty-combinator totality, `overrideWith`)
  is pinned red-green and named for the migration guide.
- Nothing deleted without a downstream-client grep (FC-D2 standing rule).
- Nothing leaves a mark (push, publish, client-repo change) without approval.

## Scope (settled forks)
- IsValid stays in the vocabulary: production gates `complete_ares` on
  `AllOf(IsEqual("stat","CZ"), IsValid("ico"))` (cmi-portaly, 5 declarations
  + stored JSON). The shared evaluator takes a validity view as input;
  callers without one supply `always-valid` (RequiredValidation's
  chicken-and-egg, PDF-of-submitted-data).
- NonEmpty means blank-filtering: production declares `kategorie_meridla`
  with `default: ""` gated by `NonEmpty` — presence semantics would show the
  section immediately; the SPA (what users see) filters blanks. UIFormBuilder
  behavior is the bug and flips.
- Empty AnyOf/AllOf are total: `exists`/`forall` (AnyOf() = false,
  AllOf() = true); the `.reduce` crash goes.
- FieldKind: closed enum + `Custom(id)`, hand-written wire-stable codec
  (FC-D6). Czech/CMI extraction to `czech-support` (FC-D3). Package rename
  `portaly.forms` → `works.iterative.forms` after `formsCore` retires.

## Deliverable + reaction surface
One shared core (fold + Condition.eval + validation + FieldKind) with SSR and
SPA refolded onto it, plus the conformance kit · react through:
`PORT=8391 ./mill formsScenarios.jvm.run` (SSR at /ssrForm/page, SPA
custom-element scenario) and `./mill formsScenarios.jvm.e2e` +
`./mill forms.jvm.test`.

## Don't-care / parked at definition
- Morph swaps (idiomorph) for text-driven conditions — noted, next slice+.
- FormBundle / message snapshot / PDF — slice 3 (serialize-pdf-roundtrip).
- SSR file upload → FileRef — slice 3.
- Mirror-based TypedForm.derive — deferred (FC-D1).
- Actual deletion of retired lineages beyond what the package rename forces —
  downstream horizon (retire-old-lineages).
