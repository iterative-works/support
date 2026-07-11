<!-- PURPOSE: Numbered decisions for the forms-consolidation effort — append-only, superseded never edited -->
<!-- PURPOSE: Each decision carries a propagation checklist; unchecked boxes are visible drift -->

# Forms Consolidation — Decisions

IDs are stable (`FC-Dn`). Change a decision by appending a superseding one and
marking the old `SUPERSEDED by FC-Dn`; never edit an accepted entry.

## FC-D1 — The portaly spine wins (2026-07-11)

Consolidate onto `FormSegment` → `UIFormBuilder` → `UIForm` →
`Interpreter[I,O]`, renamed `works.iterative.forms`. `FormSchema[A]`'s
type-safety folds in as `TypedForm[A]` compiling down to the spine
(interpreters only see the erased `Form`). Module layout: `forms` +
`formsPdf` (+ `formsDb` optional); render-neutral model stays in
`uiForms/shared` until post-migration cleanup. Mirror-based
`TypedForm.derive` deferred as future possible development. Full rationale
and shape: `forms-plan.md`.

Propagation:
- [x] map — slice sequence updated (ssr-html-interpreter zero-ADT-change
  first cut; core-hardening+spa-refold new item)
- [x] forms-plan.md marked accepted for the decided points
- [ ] intent preferences section — spine preference now settled (note on
  next intent touch)

## FC-D2 — Retirement list approved; one-time converters (2026-07-11)

The per-lineage retirement list in `forms-plan.md` ("What retires") is
approved as written, including sequencing (downstream items stay compiling
until consumer migration). Persisted-data wire changes are handled by
one-time offline converters, rehearsed against a production dump — no
runtime fallback decoders.

Propagation:
- [x] forms-plan.md wire-posture section reflects this
- [ ] slice 3 scope — converter rehearsal against production dump
- [ ] each deletion preceded by downstream-client grep (standing rule)

## FC-D3 — Client-domain code moves to czech-support extras (2026-07-11)

CMI/Czech-specific code (`cmi:*` field/section/rule hardwiring,
`complete_ares` handler, `Submission` aggregate, `DsSubmissionService`,
`SubmitResult` payment fields, `Enum.yesno`) extracts to a separate
in-repo `czech-support` extras module first, and moves off-repo later.
ARES/VIES services live there too.

Propagation:
- [ ] slice 2 (core-hardening + spa-refold) includes the extraction
- [x] forms-plan.md updated

## FC-D4 — SSR substrate: scalatags with HTMX support (2026-07-11)

The SSR HTML interpreter renders scalatags and is designed for HTMX-driven
interaction: condition re-evaluation and repeated-group add/remove as
server-rendered fragment swaps (hx-post + partial re-render), degrading to
full-page POST without JS. Precedent: the custom-element demo page already
pulls the htmx CDN.

Propagation:
- [ ] slice 1 scope — renderer emits stable fragment ids for hx-swap targets
- [x] forms-plan.md updated

## FC-D5 — Ship a default uiform.xsl (2026-07-11)

The PDF target becomes demonstrable in-repo: genericize the client
`uiform.xsl` (reference implementations:
`~/Devel/projects/cmi-portaly/module/forms/jvm/src/main/resources/uiform.xsl`,
`~/Devel/projects/medeca-modul-poptavky/.../uiform.xsl` + `uiform2.xsl`)
into a shipped default with neutral styling plus a minimal `form.xsl`
example; branded headers/signatures stay app-side. The `ui:*` XML
vocabulary stays byte-stable.

Propagation:
- [ ] slice 3 (PDF) scope
- [x] forms-plan.md updated
