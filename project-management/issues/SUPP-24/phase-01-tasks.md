# Phase 1: Build refactor + dual-publish workflow — Tasks

**Issue:** SUPP-24
**Phase:** 1 of 2
**Estimate:** 5–9 hours

## Setup

- [ ] [setup] Re-read `project-management/issues/SUPP-24/phase-01-context.md` end-to-end before starting.
- [ ] [setup] Open `build.mill` and confirm the line-numbered anchors still match the spec (directive 1-3, `mvnDeps:` 4-6, version 40, `BaseModule` 92, `BaseScalaJSModule` 115, `CrossModule.SharedModule` 152, `scenarios` 605, `formsScenarios` 921, `filesUIScenarios` 945, `scenariosUI` 972, `publishVersion = "0.0.0"` markers at 932/956/978).
- [ ] [setup] Capture a baseline `./mill resolve __.publishArtifacts` listing before any edits so it can be diffed against the post-refactor enumeration.

## Tests

- [ ] [test] Run `./mill __.compile` and confirm it passes against the current (pre-refactor) tree as the regression baseline.
- [ ] [test] Run `./mill __.test` and confirm it passes against the current tree as the regression baseline.

## Implementation

- [ ] [impl] Bump `CommonVersion.publishVersion` from `"0.1.13"` to `"0.1.14-SNAPSHOT"` at `build.mill:40`.
- [ ] [impl] Extract `BaseModuleCommon extends IWScalaModule` at `build.mill` near line 92, holding `scalaVersion = scala3LTSVersion`, the `scalacOptions` filter for `-Xkind-projector`, the `sources` / `resources` overrides, and the inner `BaseTests` trait — but NOT `IWPublishModule` and NOT `ScalafixModule`.
- [ ] [impl] Redefine `trait BaseModule extends BaseModuleCommon with IWPublishModule with ScalafixModule` at `build.mill:92`, keeping `override def publishVersion = CommonVersion.publishVersion` on it.
- [ ] [impl] Introduce `trait BaseModuleNoPublish extends BaseModuleCommon with ScalafixModule` next to `BaseModule` in `build.mill`.
- [ ] [impl] Introduce a non-publishing JS base trait `BaseScalaJSModuleNoPublish extends BaseModuleNoPublish with ScalaJSModule` paralleling `BaseScalaJSModule` (`build.mill:115`), with `scalaJSVersion = "1.20.2"` and a `BaseScalaJSTests` inner trait identical to the publishing variant.
- [ ] [impl] Introduce a non-publishing CrossModule helper paralleling `CrossModule` (`build.mill:148-159`) — implementer chooses between adding a `NoPublishCrossModule` trait (with `SharedModule extends BaseModuleNoPublish with FullCrossScalaModule`, `JvmModule extends SharedModule`, `JsModule extends SharedModule with BaseScalaJSModuleNoPublish`) or parameterizing the existing `CrossModule`.
- [ ] [impl] Migrate `scenarios` (`build.mill:605`) — both `jvm` and `js` — onto the non-publishing CrossModule analogue.
- [ ] [impl] Migrate `formsScenarios` (`build.mill:921`) — both `jvm` and `js` plus the `FormsScenariosModule` trait — onto the non-publishing CrossModule analogue.
- [ ] [impl] Migrate `filesUIScenarios` (`build.mill:945`) — both `jvm` and `js` plus the `FilesUIScenariosModule` trait — onto the non-publishing CrossModule analogue.
- [ ] [impl] Switch `scenariosUI` (`build.mill:972`) from `BaseScalaJSModule` to `BaseScalaJSModuleNoPublish`.
- [ ] [impl] Remove `override def publishVersion = "0.0.0"` from `formsScenarios`'s `FormsScenariosModule` (`build.mill:932`).
- [ ] [impl] Remove `override def publishVersion = "0.0.0"` from `filesUIScenarios`'s `FilesUIScenariosModule` (`build.mill:956`).
- [ ] [impl] Remove `override def publishVersion = "0.0.0"` from `scenariosUI` (`build.mill:978`).
- [ ] [impl] Create `.github/workflows/publish.yml` starting with two `# PURPOSE: ` comment lines describing the workflow's responsibility (mirror `ci.yml:1-2`).
- [ ] [impl] Add `name: Publish` and the trigger block `on: { push: { branches: [main], tags: ['v*'] } }` to `publish.yml`.
- [ ] [impl] Add job-level `permissions: { contents: read, packages: write }` to the publish job in `publish.yml`.
- [ ] [impl] Add the standard step prefix to `publish.yml`: `actions/checkout@v4`, then `actions/setup-java@v4` (`distribution: temurin`, `java-version: 21`), then the Coursier cache (`actions/cache@v4`, key pattern from `ci.yml:44-50`), then the Mill cache (`actions/cache@v4`, key pattern from `ci.yml:52-58`).
- [ ] [impl] Add the test-gate step `./mill __.test` to `publish.yml`, runs on every trigger before any publish step, with `COURSIER_CREDENTIALS` env mirroring `ci.yml:181`.
- [ ] [impl] Add the GitHub Packages publish step to `publish.yml`: runs on both triggers, env `IW_PUBLISH_RELEASE_URI=https://maven.pkg.github.com/iterative-works/support`, `IW_PUBLISH_SNAPSHOT_URI=https://maven.pkg.github.com/iterative-works/support`, `MILL_SONATYPE_USERNAME=${{ github.actor }}`, `MILL_SONATYPE_PASSWORD=${{ secrets.GITHUB_TOKEN }}`, command `./mill __.publish`.
- [ ] [impl] Add the e-BS Nexus publish step to `publish.yml`: gated `if: github.ref_type == 'tag'`, env `IW_PUBLISH_RELEASE_URI=https://nexus.e-bs.cz/repository/maven-releases/`, `IW_PUBLISH_SNAPSHOT_URI=https://nexus.e-bs.cz/repository/maven-snapshots/`, `MILL_SONATYPE_USERNAME=${{ secrets.EBS_NEXUS_USERNAME }}`, `MILL_SONATYPE_PASSWORD=${{ secrets.EBS_NEXUS_PASSWORD }}`, command `./mill __.publish`, ordered AFTER the GH Packages step (sequential, fail-fast, no `continue-on-error`).

## Integration / Verification

- [ ] [verify] Run `./mill __.compile` after the trait restructure and confirm it succeeds (regression check).
- [ ] [verify] Run `./mill __.test` after the trait restructure and confirm it succeeds (regression check).
- [ ] [verify] Run `./mill resolve __.publishArtifacts` and confirm the enumeration EXCLUDES `scenarios.jvm`, `scenarios.js`, `scenariosUI`, `filesUIScenarios.jvm`, `filesUIScenarios.js`, `formsScenarios.jvm`, and `formsScenarios.js`.
- [ ] [verify] Run `./mill __.publishLocal` and confirm POMs land in `~/.ivy2/local/works.iterative.support/iw-support-*/0.1.14-SNAPSHOT/` with correct organization, version, and dependency entries.
- [ ] [verify] Spot-check a representative published POM (e.g., `iw-support-core_3-0.1.14-SNAPSHOT.pom`) under `~/.ivy2/local` against the `CommonPomSettings` invocation to confirm description, license, VCS, and developer entries are unchanged.
- [ ] [verify] Delete `build.mill:1-3` (the `//| repositories:` directive plus its two Nexus URL lines, leaving the `//| mvnDeps:` block at lines 4-6 untouched), clear the Mill cache (`./mill clean` or `rm -rf out/`), run `./mill resolve __.compile`; if it succeeds, keep the deletion; if it fails, restore the directive verbatim and file a follow-up issue to migrate `mill-iw-support` to Central (do not block the phase).
- [ ] [verify] Read `publish.yml` end-to-end against the spec in `phase-01-context.md` ("Component Specifications" → ".github/workflows/publish.yml") and against `ci.yml`'s Java/cache/credentials patterns; confirm YAML parses (e.g., `actionlint` if available, or rely on GitHub PR validation).
- [ ] [verify] Confirm `IW_PUBLISH_RELEASE_URI` / `IW_PUBLISH_SNAPSHOT_URI` / `MILL_SONATYPE_USERNAME` / `MILL_SONATYPE_PASSWORD` env-var names exactly match the `IWPublishModule` contract by cross-checking against the plugin source at `~/Devel/iw/support-libs/iw-project-support`.

## Acceptance

- [ ] [accept] `./mill __.publishLocal` succeeds across the whole repo.
- [ ] [accept] `./mill resolve __.publishArtifacts` enumerates the publishable modules and does NOT include `scenarios` (jvm/js), `scenariosUI`, `filesUIScenarios` (jvm/js), or `formsScenarios` (jvm/js).
- [ ] [accept] `./mill __.compile` succeeds.
- [ ] [accept] `./mill __.test` succeeds (regression check; existing test corpus continues to pass).
- [ ] [accept] `//| repositories:` directive is removed from `build.mill:1-3` (the `//| mvnDeps:` block at lines 4-6 is preserved unchanged) AND a clean-cache `./mill resolve __.compile` succeeds; if directive-removal verification failed, the directive is restored verbatim and a follow-up issue is filed (the phase still completes).
- [ ] [accept] `CommonVersion.publishVersion = "0.1.14-SNAPSHOT"` at `build.mill:40`.
- [ ] [accept] Three `publishVersion = "0.0.0"` overrides (lines 932, 956, 978) are gone.
- [ ] [accept] `.github/workflows/publish.yml` exists with: triggers `push: branches: [main]` and `push: tags: ['v*']`; job-level `permissions: { contents: read, packages: write }`; test gate step running `./mill __.test` before any publish step; GH Packages publish step running on both triggers with the four destination-specific env vars; e-BS Nexus publish step gated on `github.ref_type == 'tag'` with the four destination-specific env vars; two `./mill __.publish` invocations on a tag-trigger run, one on a `main`-trigger run.
- [ ] [accept] All published modules' `pomSettings` remain intact — no diff to `CommonPomSettings` invocations or per-module `pomSettings` overrides.
- [ ] [accept] Each new file (`.github/workflows/publish.yml` and any new helper files the implementer adds) starts with two `# PURPOSE: ` comment lines per `CLAUDE.md`.
