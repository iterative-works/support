---
issue: iw-support-form-effort-3
card: ./card.md
status: active
branch: iw-support-form-effort
updated: 2026-07-11
---

# Slice Tracker — core-hardening-spa-refold   (READ FIRST to resume)

## Card digest (frozen)
Goal: one shared core (fold + Condition.eval + validation + FieldKind); SPA and
SSR agree by construction · Fit: proof form behaves identically in SSR and SPA
scenarios, conformance kit green → full card: ./card.md

## Run-state
branch iw-support-form-effort · last sha c4dd683e · run/see:
`PORT=8391 ./mill formsScenarios.jvm.run` + `./mill formsScenarios.jvm.e2e` +
`./mill forms.jvm.test` · next: OPEN 1

## DONE
- [x] Client-repo grep settling IsValid/NonEmpty (evidence in card scope)

## OPEN  (ordered; each traces to the fit test; the next step is marked)
- [ ] Shared pure `Condition.eval` (TDD), UIFormBuilder + RequiredValidation
      refold onto it; pinned NonEmpty test flips; empty-combinator crash goes
      — needed for fit because one evaluator is the anti-drift mechanism   <-- NEXT
- [ ] JS walkers (LiveHtmlInterpreter reactive wrap, ReadOnlyHtmlInterpreter)
      evaluate through `Condition.eval` — kills the ×4 semantics drift
- [ ] Shared segment fold (dispatch + ShowIf gating + Repeated expansion)
      extracted; UIFormBuilder and RequiredValidation become instances
- [ ] `FormData` (`Map[AbsolutePath, List[FieldValue]]`, `Text`/`File`)
      implements FormState, keeps `parse` + `__items`; replaces FormR
      internally; `overrideWith` bug pinned red-green
- [ ] Declared `Validation` vocabulary: `Field.validations: List[Validation]
      = Nil` (zio-json default-decode pinned first); pure compiler to
      validation rules; RequiredValidation folds in; SSR POST uses it
- [ ] `ValidationRule`/`ValidationState` relocate forms/js → forms/shared
      (verified no Laminar imports); SPA reactive validation wraps the same
      pure rules
- [ ] `FieldKind` enum + `Custom(id)` + hand-written wire-stable codec
      (FC-D6); interpreters dispatch exhaustively; codec round-trip tests
      over the client-audit id list
- [ ] UIForm/ADT hardening from the gap inventory: Enum optional flag, Date
      required-able, button intent (submit/server-action/client-action),
      repeated-group node (add/remove derivable), IdPath ids, form-level
      error slot, Disabled/context decorations survive the fold
- [ ] `TypedForm[A]` + `InputSchema` relocation (FormSchema salvage;
      interpreters see only erased Form)
- [ ] czech-support extraction (FC-D3): cmi/czech field types, ARES/VIES,
      `complete_ares`, `Enum.yesno`, Submission/DsSubmission/paymentUrl
- [ ] formsCore/formsHttp/FormComponents retirement (downstream grep first)
      + package rename `portaly.forms` → `works.iterative.forms`
- [ ] SPA refold completes: LiveHtmlInterpreter on shared dispatch +
      validation; SPA custom-element scenario proves the proof form
- [ ] Conformance kit: every FieldKind + validation rule + condition case
      through every interpreter; drift = red

## PARKED  (-> future slices; capture, do not do)
- Morph swaps for text-driven conditions (slice 2+ disposition, pinned by e2e)
- FormBundle + message snapshot, PDF default xsl, file upload (slice 3)
- Deletion of tailwind.form / laminar.forms beyond InputSchema salvage
  (retire-old-lineages, downstream)

## Fit log  (react -> reshape — the empirical residue)
- 2026-07-11 reaction: "run the implementation of the next step" (after
  reading the gap inventory) -> slice opened; dispositions taken as proposed;
  IsValid/NonEmpty settled by client-repo evidence, flagged for Michal's read
