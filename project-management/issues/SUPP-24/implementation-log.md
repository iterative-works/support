# Implementation Log: Dual-publish to e-BS Nexus + GitHub Packages

Issue: SUPP-24

This log tracks the evolution of implementation across phases.

---

## Phase 1: Build refactor + dual-publish workflow (2026-04-29)

**Layer:** Build configuration + CI

**What was built:**
- `build.mill` — Trait restructure: `BaseModuleCommon` extracted as the shared compile-only parent of `BaseModule` (publishable) and `BaseModuleNoPublish` (internal). New `BaseScalaJSModuleNoPublish` parallels `BaseScalaJSModule` for non-publishing JS modules. New `NoPublishCrossModule` mirrors `CrossModule` with non-publishing JVM/JS variants. Migrated `scenarios`, `formsScenarios`, `filesUIScenarios` (all jvm/js) onto `NoPublishCrossModule` and `scenariosUI` onto `BaseScalaJSModuleNoPublish`. Removed three dead `publishVersion = "0.0.0"` markers (lines 932/956/978 in pre-refactor numbering). Bumped `CommonVersion.publishVersion` from `0.1.13` to `0.1.14-SNAPSHOT`. Removed the `//| repositories:` Nexus directive at lines 1-3 (verified `mill-iw-support::0.1.4` resolves from public Maven Central chain on a clean cache). Added two `// PURPOSE: ` comment lines after the `//|` YAML header per CLAUDE.md.
- `.github/workflows/publish.yml` — NEW. Two triggers (`push: branches: [main]`, `push: tags: ['v*']`). Job-level `permissions: { contents: read, packages: write }`. Test gate (`./mill __.test`). GH Packages publish step on both triggers (uses `${{ github.actor }}` + `${{ secrets.GITHUB_TOKEN }}`). e-BS Nexus publish step gated on `github.ref_type == 'tag'` (uses `${{ secrets.EBS_NEXUS_USERNAME }}` + `${{ secrets.EBS_NEXUS_PASSWORD }}`). Sequential, fail-fast.

**Dependencies on other layers:** None — Phase 1 of 2.

**Decisions:**
- The `//| repositories:` directive removal verified successfully on a clean cache; the directive is gone for good. No follow-up issue needed.
- `root.moduleDeps` previously included the four scenario/scenariosUI modules. Since `root extends BaseModule` (a `PublishModule`), `moduleDeps` requires `Seq[PublishModule]`. Removed the non-publishing modules from `root.moduleDeps` — they should not be in a published aggregate anyway. Architecturally correct outcome, not explicitly called out in the spec.
- `scenariosUI` previously had both `override def publishVersion = "0.0.0"` and `def pomSettings = ...`. Both became dead code after switching to `BaseScalaJSModuleNoPublish` (no `PublishModule` in scope) and were removed.

**Patterns applied:**
- **Trait factoring**: shared parent + sibling-with-extra-mixin (`BaseModuleCommon` ← `BaseModule` / `BaseModuleNoPublish`). Same shape applied to JS (`BaseScalaJSModule` / `BaseScalaJSModuleNoPublish`). Avoids drift between published and internal compile settings by construction.
- **Type-level publishability**: `__.publishArtifacts` is correct by construction — the trait hierarchy enforces which modules are published. No more `publishVersion = "0.0.0"` markers to maintain.

**Verification (all 5 gates passed):**
1. `./mill __.compile` — 3643/3643 SUCCESS.
2. `./mill __.test` — 1560/1560 SUCCESS.
3. `./mill resolve __.publishArtifacts` — `scenarios.{jvm,js}`, `formsScenarios.{jvm,js}`, `filesUIScenarios.{jvm,js}`, `scenariosUI` all excluded.
4. `./mill __.publishLocal` — POMs land at `~/.ivy2/local/works.iterative.support/iw-support-*/0.1.14-SNAPSHOT/`. Spot-checked `iw-support-core_3.pom`: org/license/VCS/developer fields unchanged.
5. Clean-cache `./mill resolve __.compile` after `//|` directive removal — SUCCESS.

**Code review:**
- Iterations: 2
- Skills applied: `code-review-style`, `code-review-security`
- Iteration 1 critical: missing `// PURPOSE: ` comments in `build.mill` — fixed by adding two lines at `build.mill:4-5` (after the `//|` YAML header, the only Mill-parser-valid position).
- Iteration 2 warnings (terminal periods on Scaladoc) fixed for newly-introduced traits.
- Deferred: trait naming (`NoPublish*` → `Internal*`) — spec uses `NoPublish*` explicitly; deferred to a future refactor if preferred.
- Review file: `review-phase-01-20260429-204452.md`

**Files changed:**
```
M  build.mill
A  .github/workflows/publish.yml
```

**For next phase (Phase 2 — documentation + release execution):**
- `CommonVersion.publishVersion` is now `"0.1.14-SNAPSHOT"`. Phase 2 / Stage D bumps it to `"0.1.14"` (release).
- The publish workflow is in place but UNTESTED until the first `main` push lands. The maintainer must configure `EBS_NEXUS_USERNAME` and `EBS_NEXUS_PASSWORD` secrets BEFORE the first tag push (snapshot path uses only `GITHUB_TOKEN` and works without them).
- The `//| repositories:` directive removal succeeded — no follow-up needed; `mill-iw-support` resolves from public chain.
- Phase 2's documentation rewrites (`PUBLISHING.md`, `README.md`, deletion of `publish.sh`) and release execution (tag/push/observe) are clear of any build-config dependencies introduced here.

---
