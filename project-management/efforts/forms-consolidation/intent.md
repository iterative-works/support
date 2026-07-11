<!-- PURPOSE: The value-axis compass for the forms-consolidation effort — one declarative form core with a family of implementations, and what must hold while we get there -->
<!-- PURPOSE: Judges every slice. References the existing FormSegment/UIForm/Interpreter architecture; which declarative spine wins is hypothesis, tested by slices -->

# Forms — Standing Intent: One declarative core, many implementations

*Sits within iw-support's forms subsystem, which today carries three competing form lineages (`portaly.forms`, `ui...laminar.forms`, `ui...tailwind.form`). This intent consolidates them onto one declarative core. It references the existing declarative-interpreter architecture (`FormSegment`/`Form` → render-neutral `UIForm` → `Interpreter[I,O]`) rather than restating it. Which declarative spine wins, and how the two survivors are reconciled, is solution-shape — hypothesis, tested by slices, decided by the first one.*

## Goal — the measurable change

Today, building a form in an iw-support app means picking among three half-finished lineages: the mature-but-stringly `portaly.forms` (declarative, renders to SPA and to PDF, but has **no** browser-HTML/SSR path), the elegant-but-unfinished type-safe `FormSchema[A]` (SPA-only, two half-merged builders), and deprecated imperative widgets. There is no single answer, and no way to declare a form once and get browser HTML on both client and server.

After this effort, **one declarative core** describes a form's structure and behavior, and a **family of implementations** runs against that same core: render HTML on the **client (SPA)** and on the **server (SSR)**, **gather** the submitted data, **serialize the form and its data together** so a form can be rebuilt later faithfully, and **render the data to PDF**. A new field type or validation rule is added once, in the core, and every implementation gets it.

**How we'll know it worked** *(this is Michal's stated close gate)*: we take **one rich form**, declare it once, and drive it through the core's implementations — rendered as SSR HTML, rendered as SPA, its data captured and serialized alongside its definition and rebuilt faithfully, and rendered to PDF — and there is a **written migration guide** a client project can follow to move off the old lineages.

## Before → after

- **Before:** three competing form lineages; no browser-HTML/SSR render at all (the server side only makes PDFs); no single declarative spine — picking one means inheriting its limitations.
- **After:** one declarative core; a rich form declared once drives SSR HTML, SPA HTML, data capture, form+data serialization/faithful-rebuild, and PDF — each a distinct implementation of the same core; and a migration guide exists.

The delta above *is* the intent. Anything it doesn't name is out of scope by default.

## Invariants (must hold)

- **Trust boundary** *(stated by Michal, C):* the rival lineages are migrated or retired only with **explicit sign-off**, decided as the consolidation plan is formed up front — never silently broken, and **no backward-compatibility shim** without approval. Once the plan is approved, execution proceeds without re-gating each step.
- The reaction surface stays real: every implementation must be demonstrable on an actual rich form — we only close on something the human can run and see.

## Preferences (matter, but negotiable)

- Lean toward folding the type-safety of `FormSchema[A]`/`InputSchema` into the proven multi-target spine of `portaly.forms` (`FormSegment` → `UIForm` → `Interpreter`), rather than the reverse — but **which spine wins is settled by the first slice's analysis, not here.** *(interpolated from the codebase map — react)*
- Lean toward the existing render-neutral `UIForm` tree as the shared interpreter seam the new SSR target plugs into. *(interpolated)*
- Prefer shedding the leaked `portaly` package name (a client name) during consolidation. *(interpolated — minor)*

## Deliberately don't-care (delegated / deferred to slices)

- Which declarative ADT becomes the spine and how the two are reconciled — solution-shape, decided by the analysis slice.
- Interpreter/type-class structure, module layout, and whether the stringly `FormR`/`Any` data model is replaced, wrapped, or left as-is.
- The actual **deletion** of the old lineages and **migration of in-repo consumers** (`autocomplete`, scenarios) — this horizon proves the core and writes the guide; executing the migration is downstream.
- A **form editor** — a tool for the team, then clients, to build forms declaratively. A distinct future horizon that stands *on* this consolidated core and will get its own intent. Deliberately out of this goal. *(flagged to Michal at framing — D)*

These are solution-shape: discovered by building, not decided here.

## Fit — what we're listening for (found by reacting, not specifiable)

**Helpful** = when I reach for a form there is one obvious thing, and declaring it once gives me every path — SSR, SPA, capture, serialize/rebuild, PDF — without fighting me; and a client could follow the guide to move over. **Fragmented** = still juggling lineages, or the "one core" really only does one target well and the rest are bolted on. The tuning — how much type-safety vs. dynamism, what "rich enough" means for the proof form, how thin the core stays — is fit, found by reacting to real slices, not written down now.

## Standing on (referenced, not restated)

- **FCIS / functional-core** (body of knowledge): the declarative core is pure; the implementations are the effectful edges.
- **The existing declarative-interpreter architecture** in `portaly.forms` — `FormSegment`/`Form` → render-neutral `UIForm`/`FormState`/`IdPath` → `Interpreter[I,O]`, with a live SPA (`LiveHtmlInterpreter`) and a PDF (`UIFormXMLRenderer` → FOP) target already coexisting. The consolidation builds on this seam; it does not reinvent it.
- **Exception line (the guide is thin):** there is no forms guide in `docs/` yet. The migration guide this effort produces is the seed of one — a queue item by which the body of knowledge grows.
