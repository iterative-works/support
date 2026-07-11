<!-- PURPOSE: Work map for the forms-consolidation effort — candidates, active, done, parked -->
<!-- PURPOSE: Items reference issues, never restate them; no unmet after: + no shared surface = parallel-eligible -->

# Forms Consolidation — Map

Item metadata: `type:` ag | wf | dx | sl | manual · `after:` dependency item
slugs · `repo:` when the work lands in a sibling repository · issue reference
once active.

Order is hypothesis, not a roadmap — the carving from intent to slices is
discovered by running it.

## Items

- [x] **forms-plan** (type: sl) — analyze the three lineages, pick the
  declarative spine, decide the reconcile-vs-retire plan and which consumers
  migrate. Deliverable the team reads and signs off: the consolidation
  decision + plan. **Done 2026-07-11**: `forms-plan.md` accepted in full,
  decisions FC-D1…FC-D7. issue: <run outside process — no issue>
- [ ] **core-hardening-spa-refold** (type: sl, after: ssr-html-interpreter)
  — FormData, declared Validation vocabulary, shared fold + Condition.eval,
  FieldKind + wire-stable codec (FC-D6), TypedForm[A] + InputSchema
  relocation, package rename, czech-support extraction (FC-D3);
  LiveHtmlInterpreter refolds onto the shared pieces; conformance test kit.
  issue: <once active>
- [x] **ssr-html-interpreter** (type: sl, after: forms-plan) — the missing
  capability: render one rich form as server-side browser HTML off the chosen
  core, alongside the existing SPA render. First point a form is run-and-seen
  both ways off one declaration. **Done 2026-07-11**: `UIFormHtmlRenderer` +
  `RequiredValidation` + `SsrFormScenario` (GET/POST loop, HTMX, add/remove,
  serialize/reload proof); output `uiform-gap-inventory.md` gates the next
  slice. issue: <run outside process — no issue>
- [ ] **serialize-pdf-roundtrip** (type: sl, after: core-hardening-spa-refold)
  — gather submitted data, serialize form+data together (FormBundle with
  submission-time message snapshot, FC-D7), rebuild faithfully (byte-equal
  render from bundle only), render the data to PDF (ship default uiform.xsl,
  FC-D5), retroactive snapshot converter, draft the client migration guide.
  Closes the intent's how-we'll-know gate. issue: <once active>

## Parked

- **form-editor** — a tool for the team, then clients, to build forms
  declaratively. A distinct future horizon standing on the consolidated core;
  gets its own frame/intent, not this one.
- **retire-old-lineages** — actual deletion of `tailwind.form` /
  `portaly.forms` / `laminar.forms` and migration of in-repo consumers
  (`autocomplete`, scenarios). This effort proves the core and writes the
  guide; executing the migration is downstream.
- **fix-stringly-data-model** — replacing the `FormR`/`Any`-typed data model
  (flagged wrong in-code). Only pulled in if the spine decision forces it.
