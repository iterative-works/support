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

## Phase 2: Documentation + release execution — Stage C (2026-04-30)

**Layer:** Documentation (Stage C — in-PR work).

**What was built:**
- `PUBLISHING.md` — full rewrite (86 lines). Replaces the 82-line Nexus-only / `publish.sh`-centred document. Sections: Overview (dual-publish via GitHub Actions, `IWPublishModule`, no GPG), Release process (tag-driven, `./mill __.test` gate, sequential dual-publish, fail-fast), Snapshot publishing (asymmetric — GitHub Packages on `main` push; e-BS Nexus only on `v*` tag), Local publishing (`./mill __.publishLocal`, no credentials, `publish.sh` removal noted), Required GitHub Actions secrets (`EBS_NEXUS_USERNAME` / `EBS_NEXUS_PASSWORD` manual; `GITHUB_TOKEN` auto-provided; `permissions: { contents: read, packages: write }` rationale), Consumer setup (pointer to README), Partial-failure recovery (re-run workflow, both registries accept overwrites), Troubleshooting.
- `README.md` — refreshed (4 → 105 lines). New sections: title + one-line description; "Using iw-support" (Mill `mvn"works.iterative.support::iw-support-core::0.1.14"`, sbt `"works.iterative.support" %% "iw-support-core" % "0.1.14"`); "Resolver: e-BS Nexus" (Mill `repositoriesTask` snippet + sbt `resolvers` snippet, both with releases/snapshots URLs); "Resolver: GitHub Packages" (Mill snippet, sbt snippet with `Credentials`, plus the `~/.config/coursier/credentials.properties` `host=...\nusername=...\npassword=...` example); "Available artifacts" table. The pre-existing Czech UI scenarios section is preserved verbatim except `sbtn ~scenarios-ui/fastLinkJS` → `./mill -w scenariosUI.fastLinkJS`.
- `publish.sh` — deleted (65 lines, `git rm`). Nexus-shell-script-specific; obsoleted by `.github/workflows/publish.yml` from Phase 1.

**Dependencies on prior phases:**
- Phase 1: `.github/workflows/publish.yml`, `BaseModuleNoPublish` / `BaseScalaJSModuleNoPublish` trait names (referenced in PUBLISHING.md "Troubleshooting"), `CommonVersion.publishVersion = "0.1.14-SNAPSHOT"` at `build.mill:40`.

**Stage D (deliberately deferred — post-merge maintainer runbook):**
- All 8 `Stage D (post-merge):` `[verify]` items and 6 `Stage D (post-merge):` `[accept]` items in `phase-02-tasks.md` remain unchecked by design. They are: post-merge `./mill resolve __.publishArtifacts` and `./mill __.publishLocal` sanity checks; pre-tag CI sanity (snapshot run observation); `build.mill:40` version bump `"0.1.14-SNAPSHOT"` → `"0.1.14"` and commit on `main`; `git tag v0.1.14 && git push --tags`; tag-trigger workflow observation on both registries; consumer-side resolution checks for `iw-support-core` plus the representative artifact set (`iw-support-tapir`, `iw-support-mongo`, `iw-support-sqldb`, `iw-support-server-http`, `iw-support-all`) from both Nexus and GitHub Packages. Stage D's `EBS_NEXUS_USERNAME` / `EBS_NEXUS_PASSWORD` GitHub Actions secrets must be configured before the tag push.

**Verification (Stage C):**
1. `./mill __.compile` — passed (regression smoke check; docs changes did not touch any compilable file). 3643 targets, all green.
2. `git status` — `publish.sh` shown as deleted; `PUBLISHING.md` and `README.md` shown as modified.
3. `PUBLISHING.md` cross-checked against `.github/workflows/publish.yml`: Nexus step gated `if: github.ref_type == 'tag'`, GH Packages step ungated; documented behaviour matches workflow file.
4. `README.md` snippets manually validated for Mill `mvn"..."` (`::` cross-version) and sbt `%%` syntax; org/artifact names spelled `works.iterative.support` / `iw-support-core` consistently.
5. No "Nexus access required for plugin resolution" caveat in `PUBLISHING.md` (the Phase 1 directive removal verified successfully and that constraint is gone).

**Code review:** Skipped. The available code-review skills (`code-review-style`, `code-review-scala3`, `code-review-security`, etc.) all explicitly target Scala/code per their `DO NOT USE` clauses; none apply to Markdown documentation. Spec compliance was instead validated by reading `PUBLISHING.md` and `README.md` end-to-end against `phase-02-context.md` "Component Specifications" (sections 1–8 for `PUBLISHING.md`; sections 1–2 with all sub-sections for `README.md`). All required sections present in correct order; all hard constraints satisfied.

**Files changed (excluding workflow tracking):**
```
M  PUBLISHING.md
M  README.md
D  publish.sh
```

**For Stage D (post-merge maintainer execution):**
- Phase 2 PR description must call out the `EBS_NEXUS_USERNAME` / `EBS_NEXUS_PASSWORD` secret prerequisite — without them, the FIRST `v*` tag push will fail at the Nexus publish step. Snapshot path on `main` continues to work without those secrets.
- After this PR merges, run the post-merge sanity checks (`./mill resolve __.publishArtifacts`, `./mill __.publishLocal`) before bumping the version. Then bump `build.mill:40` `"0.1.14-SNAPSHOT"` → `"0.1.14"` in a small focused commit on `main`, push, observe the snapshot-path workflow run as a free pre-tag CI sanity check, then `git tag v0.1.14 && git push --tags`.

---
