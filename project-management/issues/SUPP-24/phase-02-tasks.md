# Phase 2: Documentation + release execution — Tasks

**Issue:** SUPP-24
**Phase:** 2 of 2
**Estimate:** 2–5 hours

## Setup

- [x] [setup] Re-read `project-management/issues/SUPP-24/phase-02-context.md` end-to-end before starting.
- [x] [setup] Read the current `PUBLISHING.md` (82 lines, Nexus-only flow centered on `publish.sh`) once for awareness — full rewrite follows; preserve nothing verbatim.
- [x] [setup] Read the current `README.md` (4 lines, Czech UI scenarios section referencing obsolete `sbtn`) — Stage C adds a new "Using iw-support" section ahead of it and corrects the `sbtn` reference.
- [x] [setup] Confirm `.github/workflows/publish.yml` (Phase 1 artifact) so `PUBLISHING.md` accurately describes its triggers, test gate, asymmetric snapshot behavior, and credential plumbing — cross-checking docs against the workflow file is part of the review checklist.
- [x] [setup] Confirm the watch-mode Mill task name for ScalaJS in `scenariosUI` by inspecting `build.mill` (`object scenariosUI extends BaseScalaJSModuleNoPublish` exposes `fastLinkJS`; the `sbtn ~scenarios-ui/fastLinkJS` equivalent is `./mill -w scenariosUI.fastLinkJS`).

## Tests

- [x] [test] Run `./mill __.compile` once before any Stage C edits as a regression baseline (docs changes should not affect compilation, but the smoke check guards against accidental edits to other files).

## Implementation

- [x] [impl] Rewrite `PUBLISHING.md` "Overview" section per phase-02-context.md "Component Specifications" → "PUBLISHING.md (full rewrite)" → section 1 (one paragraph: dual-publish to e-BS Nexus and GitHub Packages via GitHub Actions; `IWPublishModule` from `mill-iw-support` is the publish trait, env-var-configurable; no GPG signing).
- [x] [impl] Write the `PUBLISHING.md` "Release process (sanctioned path)" section per spec section 2 (tag-driven release: bump `CommonVersion.publishVersion` to a non-SNAPSHOT version on `main`, push, then `git tag vX.Y.Z && git push --tags`; `publish.yml` runs `./mill __.test` then publishes to BOTH destinations sequentially; mention fail-fast).
- [x] [impl] Write the `PUBLISHING.md` "Snapshot publishing" section per spec section 3 (asymmetric: every push to `main` runs the test gate and publishes to GitHub Packages only; e-BS Nexus does NOT receive snapshots — internal consumers see only tagged releases).
- [x] [impl] Write the `PUBLISHING.md` "Local publishing for development" section per spec section 4 (`./mill __.publishLocal` lands artifacts in `~/.ivy2/local`; no credentials needed; mention that the previous `publish.sh` wrapper has been removed because GitHub Actions is the sanctioned release path).
- [x] [impl] Write the `PUBLISHING.md` "Required GitHub Actions secrets" section per spec section 5 (`EBS_NEXUS_USERNAME` and `EBS_NEXUS_PASSWORD` configured manually by a maintainer with repo admin rights; `GITHUB_TOKEN` auto-provided; the job-level `permissions: { contents: read, packages: write }` declaration in `publish.yml` is what lets `GITHUB_TOKEN` write to GitHub Packages).
- [x] [impl] Write the `PUBLISHING.md` "Consumer setup" section per spec section 6 (brief pointer to `README.md` for resolver coordinates).
- [x] [impl] Write the `PUBLISHING.md` "Partial-failure recovery" section per spec section 7 (re-run the workflow from the GitHub Actions UI; both registries accept overwrites of the same coordinates; do NOT bump to `0.1.15` to fix a transient upload failure).
- [x] [impl] Write the `PUBLISHING.md` "Troubleshooting" section per spec section 8 (auth failure → check secrets; module not enumerated → `./mill resolve __.publishArtifacts`; workflow not triggering → confirm tag matches `v*`).
- [x] [impl] Confirm `PUBLISHING.md` does NOT include any "Nexus access required for plugin resolution" caveat — the `//| repositories:` directive removal verified successfully in Phase 1 and the constraint no longer exists (per phase-02-context.md Notes and Phase 1 implementation log).
- [x] [impl] In `README.md`, add the title and one-line description per phase-02-context.md "Component Specifications" → "README.md (refresh)" → section 1 (e.g., `# iw-support` + a one-sentence summary).
- [x] [impl] Add the "Using iw-support" section to `README.md` per spec section 2, ahead of the existing UI scenarios section, with these sub-sections in order: Mill, sbt, Resolver: e-BS Nexus, Resolver: GitHub Packages, Available artifacts.
- [x] [impl] In the README "Mill" sub-section, show `mvn"works.iterative.support::iw-support-core::0.1.14"` with a short note that `_3` cross-version suffixing is automatic via `::` (use the original ticket text form verbatim per phase-02-context.md "Stage C — documentation rewrite strategy").
- [x] [impl] In the README "sbt" sub-section, show `"works.iterative.support" %% "iw-support-core" % "0.1.14"`.
- [x] [impl] In the README "Resolver: e-BS Nexus" sub-section, show Mill and sbt resolver snippets pointing at `https://nexus.e-bs.cz/repository/maven-releases/` (releases) and `https://nexus.e-bs.cz/repository/maven-snapshots/` (snapshots), with a note that internal e-BS projects already have these configured.
- [x] [impl] In the README "Resolver: GitHub Packages" sub-section, show Mill and sbt resolver snippets pointing at `https://maven.pkg.github.com/iterative-works/support`, with a note that consumers need a GitHub PAT (`read:packages` scope) configured in their coursier credentials (`~/.config/coursier/credentials.properties`) or sbt `credentials`. Include a brief example of the coursier credential file shape (`host=maven.pkg.github.com`, `username=<github-user>`, `password=<pat>`) per phase-02-context.md Notes.
- [x] [impl] In the README "Available artifacts" sub-section, list the published artifact names (`iw-support-core`, `iw-support-tapir`, `iw-support-mongo`, `iw-support-sqldb`, `iw-support-server-http`, `iw-support-all`, etc.); confirm the full inventory by running `./mill resolve __.publishArtifacts`.
- [x] [impl] In the existing `README.md` "UI scenarios development" section, replace the obsolete `sbtn ~scenarios-ui/fastLinkJS` line with `./mill -w scenariosUI.fastLinkJS` (confirmed by checking `build.mill` — `scenariosUI` is a `BaseScalaJSModuleNoPublish` exposing `fastLinkJS`). Preserve the rest of the section (`cd ui/scenarios/`, `yarn`, `yarn vite .`).
- [x] [impl] `git rm publish.sh` (Nexus-shell-script-specific, obsoleted by Phase 1's `publish.yml`).

## Integration / Verification

- [x] [verify] Run `./mill __.compile` after Stage C edits as a smoke check that nothing outside the doc files was inadvertently modified.
- [x] [verify] Re-read `PUBLISHING.md` end-to-end against `.github/workflows/publish.yml` and confirm no drift: the Nexus step has `if: github.ref_type == 'tag'`; the GH Packages step does not; docs match.
- [x] [verify] Re-read `README.md` and confirm Mill `mvn"..."` and sbt `%%` snippet syntaxes match current Mill 1.x and sbt conventions (`::` cross-version for Mill, `%%` cross-version for sbt) and the `works.iterative.support` org / `iw-support-core` artifact names are spelled correctly.
- [x] [verify] Confirm `publish.sh` is gone from the repo root (`ls publish.sh` should fail; `git status` should show it as deleted).
- [ ] [verify] Stage D (post-merge): on merged `main`, run `./mill resolve __.publishArtifacts` and confirm the publishable set excludes `scenarios.{jvm,js}`, `scenariosUI`, `filesUIScenarios.{jvm,js}`, `formsScenarios.{jvm,js}` (sanity check after merge — should match Phase 1 verification).
- [ ] [verify] Stage D (post-merge): on merged `main`, run `./mill __.publishLocal` and confirm POMs land at `~/.ivy2/local/works.iterative.support/iw-support-*/0.1.14-SNAPSHOT/`. Spot-check `iw-support-core_3.pom` for organization, version, license, VCS, developer fields.
- [ ] [verify] Stage D (post-merge, optional but recommended): inspect the most recent `publish.yml` run triggered by the docs PR merge into `main`. Confirm `./mill __.test` passed, the GitHub Packages publish step succeeded, the e-BS Nexus step was skipped (correct — snapshots are GH-Packages-only). Confirm `0.1.14-SNAPSHOT` artifacts appear at `https://github.com/iterative-works/support/packages`.
- [ ] [verify] Stage D (post-merge): edit `build.mill:40` from `val publishVersion = "0.1.14-SNAPSHOT"` to `val publishVersion = "0.1.14"` and commit on `main` with a small focused message (e.g., `release: bump version to 0.1.14`). Push.
- [ ] [verify] Stage D (post-merge): tag and push — `git tag v0.1.14 && git push --tags`.
- [ ] [verify] Stage D (post-merge): watch the `publish.yml` run triggered by the tag and confirm: `./mill __.test` gate passes; "Publish to GitHub Packages" step succeeds and artifacts appear at `https://github.com/iterative-works/support/packages` for `0.1.14`; "Publish to e-BS Nexus" step runs (gated on `github.ref_type == 'tag'`) and succeeds with artifacts at `https://nexus.e-bs.cz/repository/maven-releases/works/iterative/support/iw-support-core_3/0.1.14/` (and similar paths for the other published artifacts); both steps green, workflow overall green.
- [ ] [verify] Stage D (post-merge): from an existing e-BS project (no resolver-config change), resolve `iw-support-core::0.1.14` from Nexus. Confirm classpath wiring works. Repeat for the representative artifact set: `iw-support-tapir`, `iw-support-mongo`, `iw-support-sqldb`, `iw-support-server-http`, `iw-support-all`.
- [ ] [verify] Stage D (post-merge): from a clean repo with a GitHub PAT (`read:packages` scope) in coursier credentials, add a resolver pointing at `https://maven.pkg.github.com/iterative-works/support` and resolve `iw-support-core::0.1.14`. Repeat for the same five representative artifacts.

## Acceptance

- [x] [accept] `PUBLISHING.md` reflects the dual-publish flow: GitHub Actions as the sanctioned release path, the tag-driven release process, the snapshot-asymmetric behavior (snapshots → GH Packages only; tags → both registries), the two `EBS_NEXUS_*` secrets and auto-provided `GITHUB_TOKEN`, the contributor `./mill __.publishLocal` path with no credentials needed, partial-failure recovery via workflow re-run.
- [x] [accept] `PUBLISHING.md` does NOT include any "Nexus access required for plugin resolution" caveat — the `//| repositories:` directive removal verified successfully in Phase 1.
- [x] [accept] `README.md` documents BOTH consumer paths: Mill `mvn"works.iterative.support::iw-support-core::0.1.14"` literal AND sbt `"works.iterative.support" %% "iw-support-core" % "0.1.14"` snippet, with a per-destination resolver block (e-BS Nexus on one side, GitHub Packages with PAT on the other) presented as a distinct concern from the dependency snippet.
- [x] [accept] The existing UI scenarios development section in `README.md` is corrected: `sbtn` reference is replaced with the equivalent `./mill` command. Other content in that section (`cd ui/scenarios/`, `yarn`, `yarn vite .`) is preserved.
- [x] [accept] `publish.sh` is deleted from the repo root.
- [ ] [accept] Stage D (post-merge): `./mill resolve __.publishArtifacts` on merged `main` enumerates the publishable set correctly (sanity check after merge; should match Phase 1 verification).
- [ ] [accept] Stage D (post-merge): `./mill __.publishLocal` on merged `main` succeeds; `0.1.14-SNAPSHOT` POMs land at `~/.ivy2/local/works.iterative.support/iw-support-*/0.1.14-SNAPSHOT/`.
- [ ] [accept] Stage D (post-merge): `CommonVersion.publishVersion` at `build.mill:40` is bumped to `"0.1.14"` and committed to `main`.
- [ ] [accept] Stage D (post-merge): `v0.1.14` tag is pushed.
- [ ] [accept] Stage D (post-merge): pushing the `v0.1.14` tag triggers `publish.yml` and uploads to BOTH e-BS Nexus AND GitHub Packages.
- [ ] [accept] Stage D (post-merge): artifacts at version `0.1.14` are resolvable from BOTH destinations, verified against the representative set: `iw-support-tapir`, `iw-support-mongo`, `iw-support-sqldb`, `iw-support-server-http`, `iw-support-all` (plus `iw-support-core`).
**Phase Status:** Complete
