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
branch iw-support-form-effort · last sha 1aa15acd · run/see:
`PORT=8391 ./mill formsScenarios.jvm.run` + `./mill formsScenarios.jvm.e2e` +
`./mill forms.jvm.test` · next: OPEN 4 (FormData)

## DONE
- [x] Client-repo grep settling IsValid/NonEmpty (evidence in card scope)
- [x] Shared pure `Condition.eval` + `Condition.references`; UIFormBuilder +
      RequiredValidation refold; NonEmpty pin flipped, empty-combinator
      crash gone (650761e8)
- [x] All walkers evaluate through Condition.eval: LiveHtmlInterpreter
      (reactive snapshot over referenced paths), ReadOnlyHtmlInterpreter,
      FormRJsonEncoder (1603d16e). FINDING: FormRJsonEncoder is NOT
      zero-consumer — medeca-modul-poptavky uses it (SubmissionLikeService,
      ZmenaZadostiWorkflow); retirement item 5 needs replanning for it —
      it stays compiling on FormR until medeca rebinds (FC-D2 sequencing)
- [x] Shared structural semantics: ShowIf gating = Condition.eval (prev
      step) + `Repeated.instances` (template fallback, total on empty
      templates) replacing three private expansions (1aa15acd). Fixed:
      read-only renderer dropped all rows but the first (comma-split of
      the first __items entry). DELIBERATE NARROWING vs plan letter: no
      generic visitor fold — remaining per-walker code is leaf rendering;
      all drift-prone semantics now shared. Revisit at the SPA-refold step
      if LiveHtmlInterpreter needs more; Michal may push back.

## OPEN  (ordered; each traces to the fit test; the next step is marked)
- [ ] `FormData` (`Map[AbsolutePath, List[FieldValue]]`, `Text`/`File`)   <-- NEXT
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
