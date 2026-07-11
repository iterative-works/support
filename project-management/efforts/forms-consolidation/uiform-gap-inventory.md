<!-- PURPOSE: Verified inventory of what UIForm/the fold loses or diverges on, found while building the SSR renderer -->
<!-- PURPOSE: Gates the core-hardening-spa-refold slice; every item carries evidence and a proposed disposition -->

# UIForm Gap Inventory (ssr-html-interpreter output)

Produced 2026-07-11 while building `UIFormHtmlRenderer`, `RequiredValidation`
and the `SsrFormScenario` GET/POST loop. Every gap below was hit in practice
during this slice, not speculated. Dispositions are proposals for
**core-hardening-spa-refold**; none are decided yet.

## Semantic drift between the four walkers

The same `Condition` vocabulary is evaluated four ways
(`UIFormBuilder.resolveCondition`, `LiveHtmlInterpreter`,
`ReadOnlyHtmlInterpreter`, `RequiredValidation.isSatisfied`):

- **IsValid**: `UIFormBuilder` reads `FormValidationState`
  (`UIFormBuilder.scala:189`); `LiveHtmlInterpreter.scala:515` reads live
  validation signals; `ReadOnlyHtmlInterpreter.scala:95` hardcodes `true`;
  `RequiredValidation` treats it as `true` (validation state cannot exist
  while being computed — a genuine chicken-and-egg in the vocabulary itself).
- **NonEmpty**: `UIFormBuilder.scala:192` tests `Option` presence — an empty
  string counts as non-empty (pinned in `UIFormBuilderSpec`);
  `LiveHtmlInterpreter.scala:519` filters blank strings. Same declaration,
  different visibility per platform.
- **Empty AnyOf/AllOf crash**: `UIFormBuilder.resolveCondition` uses
  `.reduce` (`UIFormBuilder.scala:185,187`) and throws on an empty
  combinator; `RequiredValidation` uses `exists`/`forall` (total).

**Disposition**: the planned shared pure `Condition.eval` must pick one
semantics per case and all walkers use it. `IsValid` needs a decision about
what it may reference (probably: earlier fields only, or drop it from the
vocabulary — grep of client repos for usage should decide).

## State that does not survive the fold (Form → UIForm)

- **Repeated grouping is erased.** Rows flatten into the parent's children
  (`UIFormBuilder.scala:52-65`); only `UIFormSection.repeatIndex` hints at
  membership. Consequences hit this slice: add/remove affordances cannot be
  derived by any UIForm consumer — the SSR scenario declares a `Button` in
  the row template and pattern-matches submitted button names server-side
  (`SsrFormScenario.respond`). Mitigated for state round-tripping by the
  builder now emitting hidden `__items` fields, but the affordance gap
  stands. **Disposition**: UIForm needs a repeated-group node (or the shared
  fold exposes group boundaries) so interpreters can render add/remove
  generically.
- **`FieldType.disabled` and `FieldType.context` are dropped.** The builder
  never emits `UIFieldDecoration.Disabled` and discards `context`
  (`UIFormBuilder.renderField`); the SPA reads them from the `FormSegment`
  ADT directly, so only the UIForm consumers (PDF, SSR) lose them.
- **`UIButton.buttonType` is hardcoded `"button"`**
  (`UIFormBuilder.scala:154`) and UIButton carries no behavior/intent; the
  SPA binds click handlers by convention (`ButtonHandler`), the XML renderer
  drops buttons entirely, and the SSR renderer overrides to `type="submit"`
  so the server sees which button fired. **Disposition**: buttons need a
  declared intent (submit / server-action / client-action) in the core ADT.
- **`UIBlock` (and everything else) carries String ids, not IdPath.** Both
  the XML renderer (`UIFormXMLRenderer.scala:86`) and the SSR renderer must
  reconstruct the path by splitting the html id on `"-"` — breaks if any
  segment ever contains a dash. `UIForm.scala:6` already carries the
  `TODO: use IdPath`. **Disposition**: ids become IdPath in slice 2's ADT
  hardening.

## Vocabulary gaps in the declaration itself

- **`Enum` has no `optional` flag** — enums can never be required; the
  builder gives them no `Required` decoration and `RequiredValidation`
  skips them.
- **`Date` is forced optional** (`UIFormBuilder.scala:44` hardcodes
  `optional = true`), so a required date cannot be declared.
- **No submit affordance exists in the declaration or UIForm** — the SSR
  renderer appends its own `__submit` chrome; the SPA synthesizes its own
  submit UI. Fine as interpreter chrome, but worth an explicit decision.
- **Form-level errors have nowhere to render.** A required-but-empty
  `Repeated` produces an error keyed at the repeated path
  (`RequiredValidation`), which no labeled field displays. UIForm has no
  form-level or section-level error slot (section `decorations` are always
  `Nil`, `UIFormBuilder.scala:97`).

## Platform inconsistencies noticed in passing

- **Message argument formatting differs by platform**:
  `InMemoryMessageCatalogue` (JVM) uses `String.format` (`%s`/`%d`) while
  the JS catalogue implementations should be checked for `{0}`-style —
  matters for FC-D7's resolved-message snapshots, which must be produced
  with one canonical formatter (server-side).
- **File handling SSR-side is unproven.** `UIFile = String | FileRef` on
  JVM; the SSR renderer emits `<input type="file">` and existing-file
  labels, but multipart upload → `FileRef` was out of slice scope. The
  serialize-pdf-roundtrip slice touches files and should close this.

## Fixed during this slice (no longer gaps)

- Validation state now surfaces as `UIFieldDecoration.ErrorMessage` on
  labeled fields (was: decoration cases existed, nothing populated them).
- `Repeated` item state round-trips through HTML forms via emitted hidden
  `__items` fields.
- `formsScenarios` compiles and runs again (moduleDir bug), with a test
  module and route-level integration tests.
