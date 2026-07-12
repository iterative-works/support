<!-- PURPOSE: Work map for the forms-consolidation effort — candidates, active, done, parked -->
<!-- PURPOSE: Items reference issues, never restate them; no unmet after: + no shared surface = parallel-eligible -->

# Forms Consolidation — Map

Item metadata: `type:` ag | wf | dx | sl | manual · `after:` dependency item
slugs · `repo:` when the work lands in a sibling repository · issue reference
once active.

Order is hypothesis, not a roadmap — the carving from intent to slices is
discovered by running it.

## Items

- [ ] **forms-plan** (type: sl) — analyze the three lineages, pick the
  declarative spine, decide the reconcile-vs-retire plan and which consumers
  migrate. Deliverable the team reads and signs off: the consolidation
  decision + plan. *(first usable increment — settles the deferred spine
  decision the intent leaves open)*. issue: <once active>
- [ ] **ssr-html-interpreter** (type: sl, after: forms-plan) — the missing
  capability: render one rich form as server-side browser HTML off the chosen
  core, alongside the existing SPA render. First point a form is run-and-seen
  both ways off one declaration. issue: <once active>
- [ ] **serialize-pdf-roundtrip** (type: sl, after: ssr-html-interpreter) —
  gather submitted data, serialize form+data together, rebuild faithfully, and
  render the data to PDF; draft the client migration guide. Closes the
  intent's how-we'll-know gate. issue: <once active>

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
