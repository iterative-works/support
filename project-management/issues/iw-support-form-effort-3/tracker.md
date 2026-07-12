---
issue: iw-support-form-effort-3
card: ./card.md
status: active
branch: iw-support-form-effort
updated: 2026-07-12
---

# Slice Tracker — core-hardening-spa-refold   (READ FIRST to resume)

## Card digest (frozen)
Goal: one shared core (fold + Condition.eval + validation + FieldKind); SPA and
SSR agree by construction · Fit: proof form behaves identically in SSR and SPA
scenarios, conformance kit green → full card: ./card.md

## Run-state
branch iw-support-form-effort · run/see:
`PORT=8391 ./mill formsScenarios.jvm.run` + `./mill formsScenarios.jvm.e2e` +
`./mill forms.jvm.test` + `./mill czech-support.jvm.test` · next: OPEN
(formsCore/formsHttp/FormComponents retirement + package rename)

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

## DONE (continued)
- [x] `FormData` (`Map[AbsolutePath, List[FieldValue]]`, `Text`/`File`)
      implements FormState, keeps `parse` + `__items`, `overrideWith`
      actually overrides (pinned red-green; FormR's was byte-identical to
      combineWith and has ZERO callers anywhere incl. client repos)
      (6559d617); SSR loop runs on it — SsrFormScenario ingests/rewrites/
      dumps via FormData, success page no longer stringifies through a wire
      codec (b9cddcc3). BOUNDARY: remaining FormR uses are edges only —
      repository stack (REST+MariaDB), FormRJsonEncoder (medeca), PDF
      interpreters (slice 3), persistence services; they rebind per FC-D2.
      SPA internals (LiveHtmlInterpreter, ButtonHandler, form fields) move
      at the SPA-refold step.

- [x] Declared `Validation` vocabulary lands end to end: relocation first —
      `ValidationRule`/`ValidationState` moved to forms/shared; CORRECTION
      to the plan's "no Laminar imports": ValidationState had an airstream
      import for a deprecated zero-caller helper, preserved verbatim
      JS-side as `ReactiveValidationState` (nothing deleted). Then `enum
      Validation` (Required/Email/Pattern/MinLength/MaxLength/Rule) on
      `Field.validations = Nil`, wire pins for legacy decode + new shape;
      RequiredValidation renamed `DeclaredValidation` and evaluates the
      vocabulary via pure `Validation.check` (format checks skip blank;
      Rule passes — binds at the edges when the registry lands, likely
      czech-support step); SSR scenario declares Email, POST re-renders
      its error. NOTE: `Validation.check` returns Option[UserMessage];
      the ValidationRule adapter for SPA reactive wiring comes at the
      SPA-refold step.

- [x] `FieldKind` closed vocabulary as the shared dispatch index (FC-D6)
      (aa019dba). DELIBERATE DEVIATION from the decision letter ("FieldType
      is replaced"): FieldType keeps `id: String` as the wire/stored truth —
      clients pattern-match `FieldType(t, _, disabled)` with string guards
      (medeca DetailPodaniFieldTypeFactories) and `text`/`string` both live
      in production, so byte-exact re-encode forbids a kind-first case class;
      the plan's core-shape text (forms-plan "FieldType stays an open string,
      interpreters dispatch through a shared index") prescribes exactly this.
      `FieldKind.of`/`wireId` are the hand-written codec, pinned over the
      full client-audit id list incl. the `medeca:mds_mdt` drift id;
      `FieldType.kind` derives, typed `FieldType(FieldKind.Email)` constructs.
      Exhaustive dispatch landed: SSR renderer (behavior fix: base:email →
      type=email, base:phone → tel; legacy raw "tel"/"password" preserved via
      Custom), XML renderer number normalization, JS FieldTypeResolver.empty.
      LiveFieldTypeResolver still string-matches — refolds at SPA-refold/
      czech-support (its cmi:* arms leave then). String-apply retirement
      (typo-proof construction) belongs to the package-rename step.

- [x] UIForm/ADT hardening — all seven gap-inventory items landed in five
      green commits (5d221bfb, 8029341d, f6ea65c0, 4a24a22a, bd546434,
      bda6510b):
      * Enum/Date declare required-ness via `optional` — default TRUE (wire
        preservation: stored declarations are never-required today; matches
        Repeated's default, deliberately differs from Field's false). Enum
        companion lost default args to explicit overloads (Scala allows one
        apply overload with defaults; the case-class default carries the
        wire decode).
      * FieldType disabled/context survive the fold as decorations
        (UIFieldDecoration.Context = resolver's sibling-value scope). SSR
        renders disabled inputs WITH a hidden mirror — disabled controls
        never submit, without it the POST loop drops their state.
      * ButtonIntent (Submit/ServerAction/ClientAction) on the Button
        segment, wire-pinned (legacy decodes ServerAction = current
        behavior everywhere). UIButton.buttonType (hardcoded, zero
        consumers) replaced by UIButtonIntent. SSR: declared Submit
        suppresses the appended chrome; ClientAction degrades inert.
      * UIRepeatedGroup keeps row boundaries (fieldName=__items, templates,
        optional, rows with item/type/index) — add/remove derivable by any
        interpreter. Builder no longer emits sibling __items hidden fields;
        SSR emits them per row; XML renderer flattens rows and drops the
        __items chrome (restores pre-slice PDF output). Group errors
        (required-but-empty Repeated) attach and render.
      * Form-level error slot (UIForm.errors) + section decorations now
        populated; SSR renders both.
      * UIFormId = IdPath (model TODO closed); dash-split path
        reconstruction gone, pinned by a dashed-segment Display test that
        was red before. JS component boundary stays String via toHtmlId.
      MIGRATION-GUIDE items for clients at upgrade: pattern-match arity on
      Enum/Date/Button changed; UIButton carries intent; custom UIForm
      walkers with `case _` fallbacks silently DROP repeated content unless
      they add a UIRepeatedGroup arm (cmi CmiLiveFormHooks is advice-only,
      unaffected).

- [x] `TypedForm[A]` + `InputSchema` relocation — the FormSchema salvage
      (2f377aa0, a930f826):
      * InputSchema moved VERBATIM ui/forms/js → ui/forms/shared, package
        unchanged (works.iterative.ui.components.laminar.forms) — so
        laminar.forms and medeca client code (InputCodecs,
        SharedRenderables) compile untouched; platform-neutrality proven
        by uiForms.jvm compiling it. forms declares its uiForms moduleDep
        explicitly (was transitive via autocomplete, flagged by the plan).
      * TypedForm[A]: applicative algebra (field/zip/*:/bimap/section/
        unit) emitting plain SectionSegments; field derives FieldType
        (FieldKind.of over InputSchema.inputType, Textarea → Prose) and
        optional = !schema.required. form(id, version) erases to the
        plain Form plus FormCodec[A] bound to the form root
        (IdPath.Root / id).
      * FormCodec decode takes FieldBuilder.Input's proven semantics, NOT
        FormSchema.Control's: non-blank value → InputSchema.decode,
        missing/blank → required error or None. (Control's decodeOptional
        path ignores the posted value in the base trait — always fails
        required fields — one more proof the lineage was unfinished.)
        Blank-is-missing matches DeclaredValidation. Errors accumulate
        across fields (zio.prelude Validation).
      * Pins: erased form case-class-EQUAL to its hand-written twin and
        runs UIFormBuilder + DeclaredValidation with the same required
        paths FormCodec fails on — the typed layer cannot fork the core.

- [x] czech-support extraction (FC-D3) (b131cfe9): the Czech/CMI domain
      leaves forms for the in-repo `czech-support` CrossModule, ALL packages
      unchanged — client-repo audit showed every piece live in at least one
      client (cmi consumes vendored source + keeps a partial submission-chain
      fork; medeca consumes the binary artifact), and both reference by
      package name, so same-package relocation + one new dependency at
      upgrade keeps them compiling.
      * Moved shared: SubmitResult, SubmissionService, DsSubmissionService,
        Ares, Vies(+ViesConfig), AresEndpoints, ViesEndpoints. jvm:
        AresService, ViesService, MongoConfig (beyond FC-D3's letter — zero
        client consumers, czech-named, and its move frees forms.jvm of
        filesMongo). js: Submission, SubmissionRepository, Endpoints,
        Codecs, User, LiveSubmissionService, LiveSubmissionRepository,
        BaseValidationResolver, LiveFieldTypeResolver, BaseButtonHandler.
      * The js Endpoints object moved WHOLE (incl. its generic autocomplete/
        file/forms groups): medeca passes the object itself into
        BaseValidationResolver.layer, so splitting it would break the
        client. ButtonHandler.scala split instead: trait + empty stay as
        the generic seam, BaseButtonHandler (complete_ares) moved.
      * Enum.yesno became a top-level `extension (e: Enum.type)` in package
        portaly.forms (czech-support shared) — resolves through the
        wildcard import client declarations already use; red-green pinned
        (companion removal went red, extension went green). Preserved
        verbatim incl. the oddity that default Some(true) encodes "true",
        not "ano".
      * forms.jvm dropped email, paygate, filesMongo: zero in-repo forms
        usage and both clients declare direct deps on them. czech-support
        .jvm carries sttp zio-json explicitly (previously rode in via
        paygate transitively).
      * FieldKind follow-up settled by relocation: LiveFieldTypeResolver's
        cmi:* string arms left the repo with the whole resolver — the
        SPA-refold no longer owes it a FieldKind refold (client-domain now).
        NOT here: the Validation.Rule registry binding — still SPA-refold/
        conformance material.
      * Tests: czech-support.jvm characterization suite (yesno call-site
        pins via `import portaly.forms.*` from a foreign package, ARES
        address accessors incl. street fallback chain, VIES EU country
        set). ARES/VIES live-network integration tests deliberately not
        added (cmi's it-suite covers them downstream).

## OPEN  (ordered; each traces to the fit test; the next step is marked)
- [ ] formsCore/formsHttp/FormComponents retirement (downstream grep first)  <-- NEXT
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
