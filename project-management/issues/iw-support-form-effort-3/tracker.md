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
`./mill forms.jvm.test` + `./mill czech-support.jvm.test` · next: OPEN list
is EMPTY — the slice's close gate (fit-test read) is Michal's call

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

- [x] Fourth mini-lineage retired + package rename (ad88ab8e, d4c9808c,
      b846d5cc):
      * formsCore (FormContext/FormErrors/FormField), formsHttp (http4s
        UrlForm bridge), ui/core FormComponents[T], ScalatagsFormComponents
        deleted. Grep evidence both directions: in-repo the chain was a
        closed island (formsHttp zero consumers; formsCore fed only
        FormComponents[T], implemented only by the ScalatagsFormComponents
        alias, used by nothing); client repos have NO dependency on the
        forms-core/forms-http artifacts and zero source references to any
        of the five types. uiCore's moduleDep drops to core (it used only
        core.Moment through the chain).
      * Package rename `portaly.forms` → `works.iterative.forms` across
        116 files (forms, czech-support, scenarios — uiForms/ui untouched;
        the render-neutral model stays put per plan). Wire format verified
        unaffected: zio-json sum types encode simple case names, resources
        load by absolute path, and the one `portaly` string literal
        (MongoConfig's `test_portaly` DB default) is deployment truth left
        alone. Client impact at upgrade is heavy but compile-time only
        (cmi 572 refs + 202 files declaring portaly.* incl. their OWN
        files extending package portaly.forms; medeca 205 imports + 4
        package-decl files); NEITHER repo has string/config/SQL package
        references, so nothing breaks silently in source. MIGRATION FLAG:
        both clients use akka-persistence — event journals may hold
        serialized `portaly.forms.*` FQNs; verify on their side before
        replay after upgrade.
      * FieldType string-apply retirement: the `given Conversion[String,
        FieldType]` and the redundant one-string apply overload are gone —
        typed `FieldType(FieldKind.X)` or explicit case-class apply (the
        wire escape hatch) are the only constructions. Zero in-repo users;
        clients DO use the conversion (`Field("x", "prose", ...)` sites in
        cmi scenarios + medeca tools) → MIGRATION item: wrap in
        FieldType(...). Pattern matching with string guards unaffected.
      * Debris noted, not fixed: forms/js `BaseIWFormElement.scala`
        declares package works.iterative.forms.scenarios (library file in
        the scenarios package) — the SPA-refold step touches that seam
        anyway.

- [x] SPA refold: LiveHtmlInterpreter on shared dispatch + validation, and
      the SPA custom-element scenario proves the proof form in a real
      browser next to SSR (078af4e0, 4410adc0, f740d263, 8ace893b, 2e089f12):
      * `Validation.rule` — the declared vocabulary as a composable
        ValidationRule (accumulates failing checks at the field path; blank
        stays the composing interpreter's concern). `FieldFactory.render`
        gains the declared-rule parameter; `fieldRule` composes required →
        declared → own rule; Hidden ignores it (parity with
        DeclaredValidation skipping hidden). MIGRATION FLAG: client custom
        FieldFactory implementations add the parameter; optional Selects no
        longer run their own rule on blank values (blank-skip unified with
        Text/TextArea — blank is only ever the required concern).
      * `Field.required` (=!optional || declared Required) is the one shared
        derivation — UIFormBuilder previously ignored a declared Required
        for the star decoration (SSR display drift, fixed red-green), the
        live interpreter now consumes it too. Enum/Date honor `optional` in
        SPA (was: enums hardcoded required=true, dates never required).
      * `Repeated.template` extracted as the shared row-template fallback;
        the live renderer uses it and a group without templates renders
        nothing instead of crashing on elems.head. NOTE degenerate-config
        drift for the conformance kit: SPA keeps unrenderable item DATA in
        __items while the SSR builder drops the rows entirely.
      * Live interpreter nests message lookups under the form key
        (f740d263) — SSR renderer and DeclaredValidation already did;
        without it dynamic row paths rendered raw message ids in SPA.
        Client note at upgrade: form-key-prefixed message keys that were
        unreachable in live forms now resolve (and win over bare suffixes).
      * BaseIWFormElement debris fixed (now package works.iterative.forms)
        and it gained the `formContent(form: LiveForm)` seam so elements
        can wrap the form with chrome.
      * Scenario proof: `InquiryProofForm` in formsScenarios shared is the
        one declaration + message catalogue + initial state for BOTH
        variants; the proof form gained Enum.bool("urgent") (checkbox in
        SPA, select in SSR — same submitted value). formCustomElement
        scenario became `spaForm`: serves the declaration, receives the
        posted FormR, echoes the same dump shape as the SSR success page;
        assets resolve via SCENARIOS_ASSETS which run+e2e forkEnv point at
        formsScenarios.js.fastLinkJS (the relative-path serving never
        worked under mill's sandbox).
      * e2e: spa-form.feature mirrors ssr-form.feature scenario for
        scenario — ONE steps class, wording identical, mechanics branch on
        the open page — and both features extended to the full surface:
        declared Email format error, and exact-value pins for hidden token,
        urgent checkbox, deadline date and prose note in the received dump.
        14 scenarios green. SPA submit chrome decides validity only after
        the 500ms field-validation throttle settles.
      * FINDING (evidence for the deferred FormR→FormData SPA-internals
        move): FormV never forgets unmounted values — after removing a row
        the SPA summary still counts it (verified in browser; SSR derives
        counts from __items and is correct). The internals move is
        behavioral, not hygiene. Also open: ButtonIntent is ignored by the
        live interpreter (all declared buttons go through ButtonHandler) —
        conformance kit decides what Submit/ClientAction mean in SPA.
      * NOT here: the Validation.Rule registry binding — Rule still passes
        everywhere; lands with the conformance kit.

- [x] Conformance kit — every FieldKind, Validation and Condition case through
      every interpreter, drift = red (995c7d69, 3deb3b36, 92b384a2, 9f8b7fde,
      93974f41, 50533edb, 6-commit chain ending with the browser pair):
      * `ConformanceCorpus` (formsScenarios shared): one sample per enum case;
        Mirror-derived case counts make a NEW enum case red automatically (no
        hand-maintained coverage list); condition expectations are proven
        against Condition.eval itself before any walker sees them.
      * Condition suite: UIFormBuilder (real validity view) agrees with
        DeclaredValidation and FormRJsonEncoder (always-valid — deliberate,
        pinned as separate expectations per sample). CAUGHT: the encoder's
        top-level reduceRight crashed on any form encoding to no data —
        now a total fold to an empty data object.
      * Validation suite: DeclaredValidation == Validation.rule per case —
        same verdict AND the same UserMessage; blank is only ever the
        required concern; declared Required overrides the optional flag;
        failures render as field errors through builder + SSR renderer.
      * FieldKind suite: per-interpreter dispatch pinned as exhaustive-match
        tables (builder passthrough, SSR HTML control table incl. the
        Custom tel/password passthrough, XML decimal-comma normalization
        exactly for number kinds); wire ids round-trip FieldKind.of.
      * `ValidationRuleRegistry` (the parked Rule binding): Rule(id, params)
        resolves by id; empty default everywhere = zero caller migration;
        threaded through Validation.check/rule, DeclaredValidation and
        LiveHtmlInterpreter (defaulted ctor param, passed at every
        copy-constructor site); unbound ids keep passing (pinned). The kit
        proves a registered rule fires identically in the POST loop and
        the reactive path — and in a real browser reactively (spaVocab).
      * Repeated degenerate-config drift killed red-green by making
        `Repeated.instances` TOTAL (Instance.segment: Option): SSR builder
        dropped template-less rows entirely — their __items entries left
        the POST loop, user data loss (SPA kept them); FormRJsonEncoder
        bypassed the shared expansion with an exact-match lookup crashing
        on both the fallback and the no-template case; a required group
        with items but no templates failed required validation. Normative:
        rows are never silently dropped (render nothing, keep __items);
        required means has-items; the JSON view of an unshapeable row is
        empty. RepeatedSpec's "expands to nothing" pin deliberately
        re-meant to "segment-less instances".
      * Browser pair: `vocabularyForm` (every kind + registry-bound field +
        a button of each intent) served as ssrVocab + spaVocab; SPA
        plumbing folded into one parametrized SpaScenario, submit chrome
        shared by both custom elements; ScenarioHtml extracted for the SSR
        shells. `FieldTypeResolver.empty` now types inputs by kind,
        mirroring the SSR control table (was: everything text — a
        default-vs-default drift). The feature pins the control table for
        BOTH variants — identical except ONE documented row: a
        checkbox-typed Field stays a text input in the SPA (Laminar
        forbids the text value controller on checkbox inputs — found live
        as an ObserverError that silently broke the entire reactive form
        graph; bool Enums are the working checkbox story; checkbox-Field
        value semantics remain an unresolved design corner in BOTH
        variants). 23 e2e scenarios green (14 proof form + 9 vocabulary).
      * ButtonIntent DECISION (was open): SSR — a declared Submit owns
        submission (chrome suppressed, pinned as the page's only submit
        control), ServerAction posts and re-renders, ClientAction is
        inert; SPA — declared buttons of every intent wait for the
        client's ButtonHandler (the intent is not yet exposed to the
        handler) while submission belongs to the element's chrome. Pinned
        in Gherkin as deliberately divergent scenarios, not hidden behind
        shared wording. Intent-aware SPA behavior stays YAGNI until a
        client needs it.
      * MIGRATION FLAGS: FieldTypeResolver.empty is now kind-aware (cmi
        passes it in two internal demo scenarios only — rendering there
        gains typed inputs); LiveHtmlInterpreter gained a defaulted
        trailing ruleRegistry ctor param.
      * Debris noted, NOT fixed (unrelated): LiveHtmlInterpreter's
        aroundTitle copy-constructor silently resets formMods to None
        (pre-existing; the registry threading preserves the behavior).

## OPEN  (ordered; each traces to the fit test; the next step is marked)
(empty — every planned step of the slice is DONE; close awaits the fit-test
read: run both proof-form pages + the vocabulary pair, review the kit)

## PARKED  (-> future slices; capture, do not do)
- Morph swaps for text-driven conditions (slice 2+ disposition, pinned by e2e)
- FormBundle + message snapshot, PDF default xsl, file upload (slice 3)
- Deletion of tailwind.form / laminar.forms beyond InputSchema salvage
  (retire-old-lineages, downstream)

## Fit log  (react -> reshape — the empirical residue)
- 2026-07-11 reaction: "run the implementation of the next step" (after
  reading the gap inventory) -> slice opened; dispositions taken as proposed;
  IsValid/NonEmpty settled by client-repo evidence, flagged for Michal's read
