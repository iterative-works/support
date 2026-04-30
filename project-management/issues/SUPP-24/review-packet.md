---
generated_from: 29dc38f21e53f2c39509121fc4d774a966a45c33
generated_at: 2026-04-30T07:42:21Z
branch: SUPP-24
issue_id: SUPP-24
phase: "1+2 (complete)"
files_analyzed:
  - build.mill
  - .github/workflows/publish.yml
  - PUBLISHING.md
  - README.md
  - publish.sh
---

# Review Packet: SUPP-24 — Dual-publish to e-BS Nexus + GitHub Packages

## Goals

This branch makes `iw-support` publishable to two Maven registries simultaneously — e-BS Nexus
(preserving existing consumer workflows unchanged) and GitHub Packages (enabling non-e-BS consumers
to resolve artifacts using a GitHub PAT). The full change set spans two phases, both now merged into
this branch.

Key objectives:

- **Phase 1 (build + CI):** Restructure `build.mill` so publishability is enforced at the type
  level; add `.github/workflows/publish.yml` that automates snapshot and release publishing.
- **Phase 2 (docs + cleanup):** Rewrite `PUBLISHING.md` for the dual-publish flow, refresh
  `README.md` with both consumer paths and resolver snippets, and delete the now-obsolete
  `publish.sh`.
- **Stage D (post-merge runbook, NOT in this PR):** Version bump `0.1.14-SNAPSHOT` → `0.1.14`,
  tag `v0.1.14`, push, observe both registries. Deferred by design — see notes below.

## Scenarios

### Phase 1 — Build + CI

- [ ] `./mill __.publishLocal` succeeds for the whole repo.
- [ ] `./mill resolve __.publishArtifacts` does NOT include `scenarios.{jvm,js}`,
  `formsScenarios.{jvm,js}`, `filesUIScenarios.{jvm,js}`, or `scenariosUI`.
- [ ] `./mill __.compile` passes (regression — trait restructure must not alter compile settings).
- [ ] `./mill __.test` passes (regression — 1560 targets pass per implementation log).
- [ ] `build.mill` has no `//| repositories:` Nexus directive (removed and verified).
- [ ] `CommonVersion.publishVersion` is `"0.1.14-SNAPSHOT"`.
- [ ] No `publishVersion = "0.0.0"` markers remain.
- [ ] `.github/workflows/publish.yml` exists with: two triggers (`push: branches: [main]` and
  `push: tags: ['v*']`), job-level `permissions: { contents: read, packages: write }`, a test gate,
  a GH Packages step on both triggers, an e-BS Nexus step gated on `github.ref_type == 'tag'`.

### Phase 2 — Documentation

- [ ] `PUBLISHING.md` covers: dual-publish via GitHub Actions, tag-driven release process,
  asymmetric snapshot behavior (GH Packages only on `main`; both registries on tags), required
  secrets, contributor `./mill __.publishLocal` path, partial-failure recovery.
- [ ] `PUBLISHING.md` contains no "Nexus access required for plugin resolution" caveat.
- [ ] `README.md` documents both consumer paths: Mill `mvn"..."` and sbt `%%` dependency snippets,
  plus per-destination resolver blocks (e-BS Nexus / GitHub Packages with PAT) presented as separate
  concerns from the dependency snippet.
- [ ] `README.md` corrects `sbtn ~scenarios-ui/fastLinkJS` to `./mill -w scenariosUI.fastLinkJS`.
- [ ] `publish.sh` is deleted.

## Entry Points

| File | Element | Why Start Here |
|------|---------|----------------|
| `build.mill` (line ~87) | `trait BaseModuleCommon` | New shared parent — verify it captures all compile/scalafix settings from the old `BaseModule` body |
| `build.mill` (line ~97) | `trait BaseModule` | Now `BaseModuleCommon with IWPublishModule with ScalafixModule` — check the trait linearization is identical to before |
| `build.mill` (line ~101) | `trait BaseModuleNoPublish` | New internal trait — spot-check that it has no `PublishModule` in its linearization |
| `build.mill` (line ~120) | `trait BaseScalaJSModuleNoPublish` | Parallel non-publishing JS base — verify no accidental `IWPublishModule` pull-through |
| `build.mill` (line ~199) | `trait NoPublishCrossModule` | Helper for cross-built internal modules — verify `JsModule` extends `BaseScalaJSModuleNoPublish` |
| `build.mill` (line ~947) | `object formsScenarios` | One of four migrated modules — verify `NoPublishCrossModule` used, `publishVersion = "0.0.0"` removed |
| `build.mill` (line ~990) | `object scenariosUI` | Migrated to `BaseScalaJSModuleNoPublish` — also verify `pomSettings` dead code removed |
| `build.mill` (line ~1151) | `object root` | Verify the four scenario modules are absent from `moduleDeps` (a `PublishModule` cannot depend on non-publish modules) |
| `.github/workflows/publish.yml` | Full file (67 lines) | New CI entrypoint — verify trigger surface, step ordering, env-var names, conditional gate |
| `PUBLISHING.md` | Full file | Full rewrite — check all 8 required sections, accuracy of asymmetric snapshot description |
| `README.md` | "Using iw-support" section | New section — check snippet syntax, resolver URL accuracy, PAT credential format |

## Diagrams

### Trait Hierarchy (build.mill)

```
IWScalaModule (from mill-iw-support)
└── BaseModuleCommon           ← new shared parent (compile + scalafix settings)
    ├── BaseModule             ← + IWPublishModule + ScalafixModule  (published modules)
    │   └── BaseScalaJSModule  ← + ScalaJSModule                     (published JS modules)
    └── BaseModuleNoPublish    ← + ScalafixModule                    (internal modules)
        └── BaseScalaJSModuleNoPublish  ← + ScalaJSModule            (internal JS modules)
```

### Cross-module Helpers

```
CrossModule (published cross-compiled pairs)
    SharedModule extends BaseModule with FullCrossScalaModule
    JvmModule extends SharedModule
    JsModule extends SharedModule with BaseScalaJSModule

NoPublishCrossModule (internal cross-compiled pairs)   ← new
    SharedModule extends BaseModuleNoPublish with FullCrossScalaModule
    JvmModule extends SharedModule
    JsModule extends SharedModule with BaseScalaJSModuleNoPublish
```

### Module Classification

```
Published (BaseModule / CrossModule):
  core.{jvm,js}, coreCats.{jvm,js}, zio.{jvm,js}
  http, server, tapir, ui.{jvm,js}, uiForms.{jvm,js}
  mongo, filesMongo, sqldb, sqldbPostgresql, sqldbMysql
  files, filesHttp, filesUI.{jvm,js}
  forms.{jvm,js}, formsHttp
  e2eTesting

Internal (NoPublishCrossModule / BaseScalaJSModuleNoPublish):
  scenarios.{jvm,js}          ← migrated from CrossModule
  formsScenarios.{jvm,js}     ← migrated from CrossModule
  filesUIScenarios.{jvm,js}   ← migrated from CrossModule
  scenariosUI                 ← migrated from BaseScalaJSModule
```

### Publish Workflow (publish.yml)

```
push: branches: [main]
│
├── Run tests (./mill __.test)
├── Publish to GitHub Packages  ← runs on both triggers
└── (Nexus step skipped — if: github.ref_type == 'tag' is FALSE)

push: tags: [v*]
│
├── Run tests (./mill __.test)
├── Publish to GitHub Packages
└── Publish to e-BS Nexus       ← runs only on tag pushes
```

### Consumer Paths

```
e-BS internal project
  resolver: nexus.e-bs.cz/maven-releases/  (existing, no change required)
  sees: tagged releases only

non-e-BS / Michal-only project
  resolver: maven.pkg.github.com/iterative-works/support
  auth: GH PAT (read:packages) in coursier credentials
  sees: tagged releases + snapshots on main
```

## Test Summary

This change is build/CI/documentation infrastructure — there are no application source files and
therefore no unit, integration, or e2e tests in the traditional sense. Verification was performed
as a set of local Mill task runs.

| Verification Step | Type | Status |
|---|---|---|
| `./mill __.compile` (3643 targets) | Regression / compile | PASSED (Phase 1) |
| `./mill __.test` (1560 targets) | Regression / unit | PASSED (Phase 1) |
| `./mill resolve __.publishArtifacts` | Structural / publish set | PASSED (Phase 1) — scenario modules excluded |
| `./mill __.publishLocal` | Integration / local publish | PASSED (Phase 1) — POMs at `~/.ivy2/local/works.iterative.support/iw-support-*/0.1.14-SNAPSHOT/` |
| Clean-cache `./mill resolve __.compile` | Integration / plugin resolution | PASSED (Phase 1) — `mill-iw-support::0.1.4` resolves from public Maven Central chain |
| `./mill __.compile` (smoke, Phase 2) | Regression (docs only) | PASSED (Phase 2) |
| `PUBLISHING.md` cross-check vs `publish.yml` | Manual spec compliance | PASSED (Phase 2) |
| `README.md` snippet syntax review | Manual spec compliance | PASSED (Phase 2) |

Code review for Phase 1 ran two iterations (`code-review-style`, `code-review-security`). Iteration
1 added missing `// PURPOSE:` comments; iteration 2 fixed Scaladoc terminal periods on new traits.
Code review for Phase 2 was skipped because the available skills target Scala, not Markdown; spec
compliance was validated by reading the produced docs end-to-end against `phase-02-context.md`.

## Verification Results

No webapp verification (this is a build/CI/documentation change). See Test Summary above for the
Mill task run results recorded in `implementation-log.md`.

## Files Changed

| File | Change | Notes |
|------|--------|-------|
| `build.mill` | Modified | Trait restructure + version bump + directive removal |
| `.github/workflows/publish.yml` | Added | New dual-publish CI workflow |
| `PUBLISHING.md` | Modified | Full rewrite (82 → 86 lines) |
| `README.md` | Modified | Expanded (4 → 105 lines) |
| `publish.sh` | Deleted | Obsoleted by `publish.yml` |

<details>
<summary>build.mill changes (summary)</summary>

- Removed `//| repositories:` Nexus directive (lines 1-3); `//| mvnDeps:` block untouched.
- Added two `// PURPOSE:` lines per `CLAUDE.md` convention.
- Extracted `BaseModuleCommon` from `BaseModule` body (compile + scalafix, no publish concerns).
- Redefined `BaseModule` as `BaseModuleCommon with IWPublishModule with ScalafixModule`.
- Added `BaseModuleNoPublish extends BaseModuleCommon with ScalafixModule`.
- Added `BaseScalaJSModuleNoPublish` paralleling `BaseScalaJSModule`.
- Added `NoPublishCrossModule` paralleling `CrossModule`.
- Migrated `scenarios`, `formsScenarios`, `filesUIScenarios` to `NoPublishCrossModule`.
- Migrated `scenariosUI` to `BaseScalaJSModuleNoPublish`; removed `pomSettings` dead code.
- Removed three `publishVersion = "0.0.0"` markers.
- Removed the four scenario/internal modules from `root.moduleDeps`.
- Bumped `CommonVersion.publishVersion`: `"0.1.13"` → `"0.1.14-SNAPSHOT"`.

</details>

<details>
<summary>publish.yml (new file, 67 lines)</summary>

Two triggers, Java 21 (Temurin), Coursier + Mill cache steps, test gate (`./mill __.test`),
GH Packages publish step (both triggers), e-BS Nexus publish step (tag trigger only,
`if: github.ref_type == 'tag'`). Sequential, fail-fast. Job-level `permissions: { contents: read,
packages: write }`.

</details>

<details>
<summary>PUBLISHING.md (full rewrite, 86 lines)</summary>

Sections: Overview, Release process, Snapshot publishing, Local publishing, Required GitHub Actions
secrets, Consumer setup (pointer to README), Partial-failure recovery, Troubleshooting. Previous
content was Nexus-only / `publish.sh`-centred and is fully replaced.

</details>

<details>
<summary>README.md (4 → 105 lines)</summary>

Added: title, one-line description, "Using iw-support" section (Mill + sbt dependency snippets,
e-BS Nexus resolver block, GitHub Packages resolver block with PAT credential format, available
artifacts table). Existing Czech UI scenarios section preserved; `sbtn` reference corrected to
`./mill -w scenariosUI.fastLinkJS`.

</details>

---

## Important Notes for Reviewers

**Stage D is intentionally absent from this PR.** The post-merge release execution — version bump
`0.1.14-SNAPSHOT` → `0.1.14`, `git tag v0.1.14`, `git push --tags`, and observation of both
registries — is a maintainer runbook executed on `main` after merge. It is not part of any PR diff.

**GitHub Actions secrets must be configured before the first tag push.** `EBS_NEXUS_USERNAME` and
`EBS_NEXUS_PASSWORD` must be added to **Settings → Secrets and variables → Actions** by a maintainer
with repo admin rights. The snapshot path on `main` works without those secrets (uses `GITHUB_TOKEN`
only). The e-BS Nexus publish step runs only on `v*` tag triggers and will fail at auth if the
secrets are absent when the first tag is pushed.

**The live workflow has not been exercised yet.** Phase 1 was merged to the `SUPP-24` branch (not
`main`). The first `main` push after this PR merges will trigger the snapshot path for the first
time — that is the live validation of the workflow's correctness.
