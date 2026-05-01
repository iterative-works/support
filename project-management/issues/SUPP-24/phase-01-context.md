# Phase 1: Build refactor + dual-publish workflow

**Issue:** SUPP-24
**Phase:** 1 of 2
**Estimate:** 5–9 hours
**Covers:** Stage A (Build Configuration Layer) + Stage B (CI Layer) from `analysis.md`.

## Goals

- Make publishability a type-level property of every module so `__.publishArtifacts` is correct by construction. Stage A introduces a `BaseModuleCommon` parent and a `BaseModuleNoPublish` sibling, migrates the four internal-only modules onto the new trait, drops the `publishVersion = "0.0.0"` markers, bumps `CommonVersion.publishVersion` to `0.1.14-SNAPSHOT`, and removes the `//| repositories:` Nexus directive at `build.mill:1-3` (conditional verification — the `//| mvnDeps:` block at lines 4-6 stays).
- Wire the dual-publish primitive that `IWPublishModule` already exposes (env-var-configurable destination URIs + Mill Sonatype credentials env vars) into a new GitHub Actions workflow at `.github/workflows/publish.yml`. Stage B adds the workflow file with two triggers (`push: branches: [main]` for snapshots, `push: tags: ['v*']` for releases), a `./mill __.test` gate, snapshot publish to GitHub Packages only, and tag-driven dual-publish to e-BS Nexus AND GitHub Packages with sequential fail-fast steps.

## Scope

**IN SCOPE**

- `build.mill`: extract `BaseModuleCommon`; redefine `BaseModule = BaseModuleCommon with IWPublishModule with ScalafixModule`; introduce `BaseModuleNoPublish = BaseModuleCommon with ScalafixModule`; switch `scenarios` (jvm/js), `scenariosUI`, `filesUIScenarios` (jvm/js), `formsScenarios` (jvm/js) onto `BaseModuleNoPublish`; remove the three `publishVersion = "0.0.0"` markers; bump `CommonVersion.publishVersion` to `0.1.14-SNAPSHOT`; conditionally remove the `//| repositories:` directive at lines 1-3 (the `//| mvnDeps:` block at lines 4-6 stays).
- `.github/workflows/publish.yml` (new): two triggers, a `./mill __.test` gate, snapshot-on-`main` to GH Packages only, tag-on-`v*` dual-publish to e-BS Nexus + GH Packages.
- Local verification commands documented in the Testing Strategy below.

**OUT OF SCOPE**

- `PUBLISHING.md` rewrite — Phase 2 (Stage C).
- `README.md` resolver-coordinate / dependency-snippet refresh — Phase 2 (Stage C).
- `publish.sh` deletion — Phase 2 (Stage C).
- Version bump from `0.1.14-SNAPSHOT` to `0.1.14` (release version) — Phase 2 (Stage D).
- Tagging `v0.1.14` and pushing the tag — Phase 2 (Stage D).
- Configuring the `EBS_NEXUS_USERNAME` / `EBS_NEXUS_PASSWORD` GitHub Actions secrets — manual maintainer step, not in any PR. Must be done before the first `main` push after merge.
- Any change to `mill-iw-support` itself; `IWPublishModule` is consumed unchanged.
- Adding integration-test gating beyond `./mill __.test` — explicitly deferred (analysis "Decision: Tag-publish test gate is `./mill __.test` only").

## Dependencies

**Prior phases:** None — this is Phase 1.

**External prerequisites (NOT in-PR work, callout for the eventual PR description):**

- A maintainer with repo admin rights must add `EBS_NEXUS_USERNAME` and `EBS_NEXUS_PASSWORD` secrets in repo settings BEFORE the first `main` push lands after merge. Without them, the snapshot publish step would still succeed (snapshots go only to GH Packages, which uses `GITHUB_TOKEN`), but the FIRST tag publish would fail at the Nexus step. Set them before merge to avoid surprise.
- `GITHUB_TOKEN` is auto-provided; the workflow file must request `permissions: { contents: read, packages: write }` at the job level for it to push to GH Packages.
- The `mill-iw-support::0.1.4` plugin must be resolvable from the public Mill plugin chain (Central) once the `//|` directive is removed. This is the working assumption per analysis "Decision: Drop the `//| repositories:` Nexus directive"; verification is part of Stage A.

## Approach

The architectural design is described layer-by-layer in `analysis.md` ("Architecture Design" section, sub-sections "Build Configuration Layer (Stage A)" and "CI Layer (Stage B)"). Decisions and alternatives are recorded under "Technical Decisions" and "Risks & Uncertainties" — refer there for the rationale, not restated here.

### Stage A: Build configuration

The current `BaseModule` trait at `build.mill:92` mixes three concerns: shared Scala/compile/scalafix configuration, publish capability via `IWPublishModule`, and Scalafix integration. Stage A factors the shared concerns into a parent `BaseModuleCommon` and derives two siblings:

- `BaseModuleCommon extends IWScalaModule` — holds the current `BaseModule` body minus the `IWPublishModule` mixin: `scalaVersion = scala3LTSVersion`, the `scalacOptions` filter for `-Xkind-projector`, `sources` / `resources` overrides, and the inner `BaseTests` trait.
- `BaseModule extends BaseModuleCommon with IWPublishModule with ScalafixModule` — same effective superclass set as today; it adds `publishVersion = CommonVersion.publishVersion`. Every currently-published module continues to extend `BaseModule` (transitively through `CrossModule.SharedModule`, `JvmOnlyModule`, etc.) and gets identical settings.
- `BaseModuleNoPublish extends BaseModuleCommon with ScalafixModule` — no `PublishModule` in the linearization. The four internal modules switch to this trait by introducing `NoPublishCrossModule` / equivalent variants of the existing `CrossModule` / `BaseScalaJSModule` helpers, OR by duplicating the minimal helper structure each module needs. Implementation tasks decide the precise factoring; the analysis specifies only the trait relationships.

`BaseScalaJSModule` (`build.mill:115`) currently extends `BaseModule`. Stage A keeps the publishable JS variant exactly as is. The non-publishing JS modules (`scenarios.js`, `filesUIScenarios.js`, `formsScenarios.js`, `scenariosUI`) get a parallel non-publishing JS base — concretely a `BaseScalaJSModuleNoPublish extends BaseModuleNoPublish with ScalaJSModule` (or equivalent) so that `__.publishArtifacts` no longer enumerates them. The `scenariosUI` module (a direct `BaseScalaJSModule`) becomes a direct `BaseScalaJSModuleNoPublish`.

Once the four modules no longer extend `PublishModule`, the three `publishVersion = "0.0.0"` markers (`scenariosUI` line 978, `filesUIScenarios` line 956, `formsScenarios` line 932) become dead overrides and are removed. `scenarios` has no marker today — the trait change alone is sufficient there.

`CommonVersion.publishVersion` (`build.mill:40`) flips from `"0.1.13"` to `"0.1.14-SNAPSHOT"`. The non-snapshot bump to `0.1.14` is Phase 2 / Stage D.

The `//| repositories:` directive (`build.mill:1-3` — three lines: header + two Nexus URLs; the `//| mvnDeps:` block at lines 4-6 is unrelated and must NOT be touched) is conditionally removed: clear the Mill cache, run `./mill resolve __.compile`, confirm `mill-iw-support::0.1.4` resolves from the public chain. If it does, remove the directive. If it does not, restore the directive verbatim and file a follow-up issue to migrate the plugin to Central — do NOT block the phase on this.

### Stage B: CI workflow

`.github/workflows/publish.yml` is a new file modeled on the existing `ci.yml` (Java 21 setup-java action, Coursier + Mill cache actions, `COURSIER_CREDENTIALS` env for plugin resolution). Differences from `ci.yml`:

- Two triggers: `push: branches: [main]` (snapshot path) and `push: tags: ['v*']` (release path).
- Job-level `permissions: { contents: read, packages: write }`.
- A test gate step `./mill __.test` runs before any publish step on both triggers, per analysis "Decision: Snapshot path runs `__.test` and publishes to GitHub Packages only".
- Publish steps gate on `${{ github.ref_type }}` (or equivalent expression):
  - The GitHub Packages publish step runs on BOTH triggers. Env: `IW_PUBLISH_RELEASE_URI` and `IW_PUBLISH_SNAPSHOT_URI` set to `https://maven.pkg.github.com/iterative-works/support`; `MILL_SONATYPE_USERNAME = ${{ github.actor }}`; `MILL_SONATYPE_PASSWORD = ${{ secrets.GITHUB_TOKEN }}`.
  - The e-BS Nexus publish step runs ONLY on the tag trigger (`github.ref_type == 'tag'`). Env: `IW_PUBLISH_RELEASE_URI = https://nexus.e-bs.cz/repository/maven-releases/`; `IW_PUBLISH_SNAPSHOT_URI = https://nexus.e-bs.cz/repository/maven-snapshots/`; `MILL_SONATYPE_USERNAME = ${{ secrets.EBS_NEXUS_USERNAME }}`; `MILL_SONATYPE_PASSWORD = ${{ secrets.EBS_NEXUS_PASSWORD }}`.
- Both publish steps invoke `./mill __.publish`. Sequential, fail-fast: if either step fails the workflow goes red and the tag must be re-run after the fix (Nexus and GH Packages both accept overwrites).

The two-step structure is asymmetric on snapshots (GH Packages only) and symmetric on tags (both). This matches the analysis decision and keeps the e-BS Nexus snapshot repo tidy.

## Files to Modify

- `build.mill` — trait restructure (`BaseModuleCommon` extraction, `BaseModuleNoPublish` introduction, JS-flavored variant for non-publishing JS modules), four module migrations (`scenarios` jvm/js at line 605, `scenariosUI` at line 972, `filesUIScenarios` jvm/js at line 945, `formsScenarios` jvm/js at line 921), three `publishVersion = "0.0.0"` marker removals (lines 932, 956, 978), version bump at line 40, conditional `//| repositories:` directive removal at lines 1-3 (lines 4-6 are the `//| mvnDeps:` block — leave it alone).
- `.github/workflows/publish.yml` — NEW. Two triggers, Java 21 + Mill caching reused from `ci.yml`, test gate, branched publish steps.

## Component Specifications

### `BaseModuleCommon` (new trait in `build.mill`)

- Extends: `IWScalaModule`.
- Holds: `scalaVersion = scala3LTSVersion`; the `scalacOptions` override that filters `-Xkind-projector`; `sources = Task.Sources("src/main/scala")`; `resources = Task.Sources("src/main/resources")`; the inner `BaseTests extends ScalaTests with TestModule.ZioTest` trait with its `moduleDir` / `sources` / `resources` / `mvnDeps` overrides.
- Does NOT mix in: `IWPublishModule`, `PublishModule`, `ScalafixModule`. The intent is "Scala compile concerns, nothing else". Scalafix is added by both child traits to keep the linting story uniform.

### `BaseModule` (existing trait, redefined)

- Spec: `BaseModuleCommon with IWPublishModule with ScalafixModule`.
- Adds: `publishVersion = CommonVersion.publishVersion`.
- Effective superclass set vs. today: identical. Every module currently extending `BaseModule` continues to compile and publish unchanged.

### `BaseModuleNoPublish` (new trait)

- Spec: `BaseModuleCommon with ScalafixModule`.
- No `PublishModule` in the linearization → `__.publishArtifacts` does not enumerate it.

### `BaseScalaJSModuleNoPublish` (new trait, name TBD by implementer)

- Spec: parallels `BaseScalaJSModule` (`build.mill:115`) but extends `BaseModuleNoPublish` instead of `BaseModule`. Same `scalaJSVersion = "1.20.2"` and `BaseScalaJSTests` inner trait.
- Reason it must exist: `BaseScalaJSModule` mixes in `BaseModule`, which transitively brings in `IWPublishModule`. To prevent JS scenarios from publishing, the JS base needs a non-publishing parent.

### Non-publishing CrossModule helper (form TBD)

- The current `CrossModule.SharedModule extends BaseModule with FullCrossScalaModule` (`build.mill:152`). For non-publishing cross modules, an equivalent that extends `BaseModuleNoPublish with FullCrossScalaModule` is needed, with parallel `JvmModule` / `JsModule` traits whose JS variant uses `BaseScalaJSModuleNoPublish`.
- Implementation may either add a `NoPublishCrossModule` trait or refactor `CrossModule` to be parameterizable. Implementer's call; both satisfy the architecture.

### Module migrations

| Module | Current location | Current trait basis | Target basis |
| --- | --- | --- | --- |
| `scenarios.jvm`, `scenarios.js` | `build.mill:605` | `CrossModule.JvmModule` / `CrossModule.JsModule` | non-publishing CrossModule analogue |
| `scenariosUI` | `build.mill:972` | `BaseScalaJSModule` | `BaseScalaJSModuleNoPublish` |
| `filesUIScenarios.jvm`, `filesUIScenarios.js` | `build.mill:945` | `CrossModule.JvmModule with FilesUIScenariosModule` / `JsModule with ...` | non-publishing CrossModule analogue |
| `formsScenarios.jvm`, `formsScenarios.js` | `build.mill:921` | `CrossModule.JvmModule with FormsScenariosModule` / `JsModule with ...` | non-publishing CrossModule analogue |

After migration: remove `override def publishVersion = "0.0.0"` at line 932 (`formsScenarios`), line 956 (`filesUIScenarios`), and line 978 (`scenariosUI`).

### `CommonVersion.publishVersion` (`build.mill:40`)

- Before: `val publishVersion = "0.1.13"`
- After: `val publishVersion = "0.1.14-SNAPSHOT"`

### `//| repositories:` directive (`build.mill:1-3`)

Conditional removal. Procedure:

1. Delete lines 1-3 (the `//| repositories:` header plus the two Nexus URL lines). The `//| mvnDeps:` block at lines 4-6 (`mill-iw-support` and `mill-scalafix` plugins) is a separate directive — leave it untouched.
2. Clear Mill cache: `./mill clean` (or `rm -rf out/`) and the user-level cache for the relevant plugin coordinates if needed.
3. Run `./mill resolve __.compile`.
4. If success → keep deletion. If failure → restore the directive verbatim and file a follow-up issue to migrate `mill-iw-support` to Central.

### `.github/workflows/publish.yml` (new)

- File header: two `# PURPOSE: ` comment lines describing the workflow's responsibilities (matches `ci.yml` convention at lines 1-2 of that file).
- `name: Publish`
- Triggers:
  ```yaml
  on:
    push:
      branches: [main]
      tags: ['v*']
  ```
- Single `publish` job (or split into `test` + `publish` with `needs:` if the implementer prefers; either satisfies the gate). Job-level:
  ```yaml
  permissions:
    contents: read
    packages: write
  ```
- Steps (in order):
  1. `actions/checkout@v4`.
  2. `actions/setup-java@v4` with `distribution: temurin`, `java-version: 21`.
  3. Coursier cache (`actions/cache@v4`, key pattern from `ci.yml:44-50`).
  4. Mill cache (`actions/cache@v4`, key pattern from `ci.yml:52-58`).
  5. Test gate: `./mill __.test`, with the same `COURSIER_CREDENTIALS` env as `ci.yml:181`.
  6. GH Packages publish step: runs on both triggers. Env block sets `IW_PUBLISH_RELEASE_URI` and `IW_PUBLISH_SNAPSHOT_URI` to `https://maven.pkg.github.com/iterative-works/support`, `MILL_SONATYPE_USERNAME` to `${{ github.actor }}`, `MILL_SONATYPE_PASSWORD` to `${{ secrets.GITHUB_TOKEN }}`. Command: `./mill __.publish`.
  7. e-BS Nexus publish step: `if: github.ref_type == 'tag'`. Env block sets `IW_PUBLISH_RELEASE_URI = https://nexus.e-bs.cz/repository/maven-releases/`, `IW_PUBLISH_SNAPSHOT_URI = https://nexus.e-bs.cz/repository/maven-snapshots/`, `MILL_SONATYPE_USERNAME = ${{ secrets.EBS_NEXUS_USERNAME }}`, `MILL_SONATYPE_PASSWORD = ${{ secrets.EBS_NEXUS_PASSWORD }}`. Command: `./mill __.publish`.
- Sequence: GH Packages first, Nexus second on tags. Sequential, fail-fast (no `continue-on-error`).

## API Contracts / Interfaces

- Mill task surface consumed by CI: `__.publishArtifacts` (resolution), `__.publish` (execution), `__.test` (gate), `__.compile` (regression check). All four are inherited from `IWPublishModule` / `ScalaModule` and need no in-build overrides.
- `IWPublishModule` env-var contract (defined in `mill-iw-support`, source at `~/Devel/iw/support-libs/iw-project-support` if confirmation is needed):
  - `IW_PUBLISH_RELEASE_URI` — release destination root URI.
  - `IW_PUBLISH_SNAPSHOT_URI` — snapshot destination root URI.
  - `MILL_SONATYPE_USERNAME` — username (despite the name, used for any destination).
  - `MILL_SONATYPE_PASSWORD` — password / token.
  - GPG signing is disabled by default in `IWPublishModule`; no toggle needed.
- Consumer-facing artifact coordinates are PRESERVED: `works.iterative.support` organization, `iw-support-*` artifact names, MIT license, `iterative-works/iw-support` VCS, `mprihoda` developer entry. `pomSettings` already encode all of these (see `CommonPomSettings`, `build.mill:25`); no change.

## Testing Strategy

### Stage A — local verification

Run from the repo root, in order:

1. `./mill __.compile` — regression check that the trait restructure changes nothing for compile.
2. `./mill __.test` — regression check that the trait restructure changes nothing for tests.
3. `./mill resolve __.publishArtifacts` — confirm enumerated set EXCLUDES `scenarios.jvm`, `scenarios.js`, `scenariosUI`, `filesUIScenarios.jvm`, `filesUIScenarios.js`, `formsScenarios.jvm`, `formsScenarios.js`.
4. `./mill __.publishLocal` — confirm POMs land in `~/.ivy2/local/works.iterative.support/iw-support-*/0.1.14-SNAPSHOT/` and look correct (organization, version, dependencies).
5. Clean-cache `./mill resolve __.compile` after `//|` directive removal — verifies `mill-iw-support::0.1.4` resolves from public chain. If this fails, restore the directive (do not block the phase).

### Stage B — pre-merge (limited)

The workflow file CANNOT be exercised in a PR — pushes to `main` and tag pushes are the trigger surface. Pre-merge verification is limited to:

- YAML syntax check (e.g., `actionlint` if available, or rely on GitHub's own validation in the PR diff).
- Read-through cross-check against `ci.yml` for the Java/cache/credentials pattern.
- Confirm the env-var names match the `IWPublishModule` contract above (compare against the plugin's source if uncertain).

### Stage B — post-merge (live, observational)

These run after Phase 1 lands, BEFORE Phase 2 tag push, and they are the live validation:

- Push a small commit to `main`. Observe: `./mill __.test` runs, GH Packages publish step succeeds, Nexus publish step is skipped. `0.1.14-SNAPSHOT` artifacts appear under `https://github.com/iterative-works/support/packages`.
- Negative case (observational, document but do not script): if either secret is missing or wrong, the publish step output should fail with an auth error, not silently succeed. No mock simulation is necessary; the first live run validates this.

## Acceptance Criteria

- [ ] `./mill __.publishLocal` succeeds across the whole repo.
- [ ] `./mill resolve __.publishArtifacts` enumerates the publishable modules and does NOT include `scenarios` (jvm/js), `scenariosUI`, `filesUIScenarios` (jvm/js), or `formsScenarios` (jvm/js).
- [ ] `./mill __.compile` succeeds.
- [ ] `./mill __.test` succeeds (regression check; the existing test corpus must continue to pass).
- [ ] `//| repositories:` directive is removed from `build.mill:1-3` (the `//| mvnDeps:` block at lines 4-6 is preserved unchanged) AND a clean-cache `./mill resolve __.compile` succeeds. If the directive removal verification fails, the directive is restored verbatim and a follow-up issue is filed (the phase still completes).
- [ ] `CommonVersion.publishVersion = "0.1.14-SNAPSHOT"` at `build.mill:40`.
- [ ] Three `publishVersion = "0.0.0"` overrides (lines 932, 956, 978) are gone.
- [ ] `.github/workflows/publish.yml` exists with:
  - Triggers: `push: branches: [main]` and `push: tags: ['v*']`.
  - Job-level `permissions: { contents: read, packages: write }`.
  - Test gate step running `./mill __.test` before any publish step.
  - GH Packages publish step running on both triggers with the four destination-specific env vars per the spec above.
  - e-BS Nexus publish step gated on `github.ref_type == 'tag'` with the four destination-specific env vars per the spec above.
  - Two `./mill __.publish` invocations on a tag-trigger run; one on a `main`-trigger run.
- [ ] All published modules' `pomSettings` remain intact — no diff to `CommonPomSettings` invocations or per-module `pomSettings` overrides.
- [ ] Each new file (`.github/workflows/publish.yml` and any new helper files if the implementer adds them) starts with two `# PURPOSE: ` comment lines per `CLAUDE.md`.

## Risks for This Phase

- **Trait restructure changes inherited settings unintentionally.** Likelihood low, impact medium. Mitigated by `BaseModuleCommon` being the single shared parent of both `BaseModule` and `BaseModuleNoPublish` (no drift by construction) and by the `./mill __.compile` + `./mill __.test` regression checks. See analysis "Risk 2".
- **`mill-iw-support::0.1.4` does not resolve from public chain after `//|` directive removal.** Likelihood low, impact medium. Mitigated by the conditional-removal procedure (restore + follow-up issue if verification fails). See analysis "Risk 3" and "Decision: Drop the `//| repositories:` Nexus directive".
- **YAML correctness for the publish workflow.** Likelihood low, impact medium. The workflow can only be fully validated by pushing — same constraint that applies to every GitHub Actions change. Mitigated by close mirroring of `ci.yml` patterns (Java setup, cache keys, env wiring) and a careful pre-merge read-through.
- **First-run auth / permission failures on either destination.** Likelihood low to medium, impact low to medium. NOT a Phase 1 in-PR risk — it surfaces only on the first `main` push or tag push after merge. Mitigated by the maintainer secret-config callout (see Notes), `permissions: { packages: write }` at the job level for `GITHUB_TOKEN`, and the fail-fast structure that surfaces auth errors loudly. See analysis "Risk 5".
- **Snapshot-on-`main` lands a broken artifact.** Likelihood low to medium, impact low (snapshots are mutable). Mitigated by the `./mill __.test` gate per analysis "Decision: Snapshot path runs `__.test` and publishes to GitHub Packages only". See analysis "Risk 4".

## Notes

- The `IW_PUBLISH_RELEASE_URI` / `IW_PUBLISH_SNAPSHOT_URI` / `MILL_SONATYPE_USERNAME` / `MILL_SONATYPE_PASSWORD` env-var contract is owned by `IWPublishModule` in the `mill-iw-support` plugin. If the implementation hits any uncertainty about the exact var names or precedence, confirm against the plugin source at `~/Devel/iw/support-libs/iw-project-support` rather than guessing.
- The `//|` directive removal is conditional. If `./mill resolve __.compile` fails on a clean cache after deletion, restore the directive verbatim and file a follow-up to migrate the plugin to Central. Do NOT block the phase on this — keep the phase shippable with the directive in place.
- GH Actions secrets `EBS_NEXUS_USERNAME` and `EBS_NEXUS_PASSWORD` are NOT in-PR work. Configuring them is a manual maintainer step that must happen before the FIRST tag push after merge (snapshot path uses only `GITHUB_TOKEN` and works without them). Surface this prominently in the eventual PR description.
- The "no GPG signing" decision is enforced by `IWPublishModule` already — no explicit toggle in `build.mill` or the workflow is needed.
- Per analysis "Decision: Snapshot path runs `__.test` and publishes to GitHub Packages only", the snapshot path is intentionally asymmetric (GH Packages only); the tag path is dual. Implementation note: the cleanest expression is a per-step `if: github.ref_type == 'tag'` on the Nexus block.
- The existing `ci.yml` runs on `pull_request: branches: [main]`. The new `publish.yml` runs on `push:` events to `main` and `v*` tags. The two workflows do not overlap in trigger surface — no race or duplicate-run concern.
- `BaseScalaJSModule` mixes in `BaseModule`, so naively migrating a JS scenario module by changing only its parent will still pull `IWPublishModule` in. The non-publishing JS base trait is therefore mandatory; do not skip it.
