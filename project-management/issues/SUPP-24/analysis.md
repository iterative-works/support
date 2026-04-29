# Technical Analysis: Dual-publish to e-BS Nexus + GitHub Packages

**Issue:** SUPP-24
**Created:** 2026-04-28 (revised 2026-04-29)
**Status:** Draft

## Problem Statement

`iw-support` is built and consumed exclusively through e-BS Nexus today. The build is wired for it via `IWPublishModule` (from `mill-iw-support`), which configures Nexus as the publish target. This couples the project's distribution lifecycle to a specific employer's infrastructure — fine for as long as that arrangement holds, but a problem if it ever doesn't, and not a fit for any consumer outside the e-BS VPN.

The original ticket framed this as "publish to Sonatype Maven Central, drop Nexus", but Central is heavier than what this project warrants:

- This project is for *our* applications, not a public framework. Discoverability through Maven Central isn't a goal.
- Central enforces ceremony — GPG signing, namespace verification, irrevocable releases, evolving Portal vs. legacy publish APIs — that pays off only when those rules' benefits matter to your audience. They don't here.
- We also want existing e-BS projects to keep resolving `iw-support` from e-BS Nexus *without changing their resolver config*. Central's "internal Nexus proxies Central" indirection works in principle but introduces a real lag and a new external dependency.

**The decision:** **dual-publish** to **e-BS Nexus** AND **GitHub Packages** (Maven), under `iterative-works/support`. e-BS workflows continue to work unchanged. New / non-e-BS use cases (including future Michal-only projects) resolve from GitHub Packages with a GH PAT in their resolver config. No Sonatype Central, no GPG.

`IWPublishModule` is **kept** in this repo: it was written for env-var-configurable publish URIs, which is exactly the pattern that makes dual-publish trivial — same Mill task invoked twice in CI with different env vars per destination.

## Proposed Solution

### High-Level Approach

Restructure the build so publishability is type-level, then drive two CI publish steps from one Mill task:

1. **Trait split.** Extract a `BaseModuleCommon` trait holding the shared Scala/compile/scalafix configuration. `BaseModule` becomes `BaseModuleCommon with IWPublishModule with ScalafixModule` (publishable). New `BaseModuleNoPublish` is `BaseModuleCommon with ScalafixModule` (no `PublishModule`). The four internal-only modules — `scenarios` (jvm/js), `scenariosUI`, `filesUIScenarios` (jvm/js), `formsScenarios` (jvm/js) — switch to `BaseModuleNoPublish`. The `publishVersion = "0.0.0"` markers go away.
2. **Keep `IWPublishModule`** as the publish trait. No URL overrides in `build.mill`. The trait already reads `IW_PUBLISH_RELEASE_URI` / `IW_PUBLISH_SNAPSHOT_URI` from env vars and routes through `MILL_SONATYPE_USERNAME` / `MILL_SONATYPE_PASSWORD` for credentials — that's the dual-publish primitive.
3. **Drop the `//| repositories:` Nexus directive** at `build.mill:1-5`, conditional on verifying that `mill-iw-support::0.1.4` is resolvable from public Mill plugin sources (Central). If verification fails in Stage A, restore the directive and file a plugin-migration follow-up.
4. **Bump version** to `0.1.14-SNAPSHOT` (build.mill:40, `CommonVersion.publishVersion`).
5. **Add `.github/workflows/publish.yml`** with snapshots from `main`, releases on `v*` tag with a test gate. The publish job runs `./mill __.publish` *twice* — once with Nexus env vars, once with GH Packages env vars. Sequential, fail-fast.
6. **Rewrite `PUBLISHING.md`** for the dual-publish flow. **Update `README.md`** with both sets of resolver coordinates. **Delete `publish.sh`** (CI is the sanctioned publish path; `./mill __.publishLocal` is the contributor path).
7. **Verify locally** (`./mill resolve __.publishArtifacts`, `./mill __.publishLocal`), then bump to `0.1.14`, tag, observe the workflow.

### Why This Approach

Keeping Nexus in the publish set preserves the e-BS consumer story exactly: those projects don't change their resolver config, don't experience proxy lag, and don't depend on a third-party registry. Adding GitHub Packages decouples *future* consumption from e-BS infrastructure: anyone with a GH PAT can resolve, no e-BS VPN required. The cost is two upload phases per release and one extra CI job step pair — small relative to the durability benefit.

`IWPublishModule` is kept because its env-var configurability is the dual-publish primitive. It also keeps GPG signing disabled, which is what we want for both destinations. The trait's `sonatypeLegacyOssrhUri` / `sonatypeCentralSnapshotUri` overrides are inert for our case (only consumed by `SonatypeCentralPublishModule.publishAll`, which we don't invoke) — cosmetic noise but harmless and outside this repo.

## Architecture Design

**Purpose:** Define WHAT each stage-as-layer needs, not HOW each line is written. This is build/CI infrastructure, so the conventional domain/application/infrastructure/presentation layering does not apply — we use the issue's Stage A–D as layers.

### Build Configuration Layer (Stage A)

**Components:**
- `build.mill` trait restructure:
  - New `BaseModuleCommon` trait holding shared Scala/compile/scalafix settings (current `BaseModule` body, minus the publish concerns).
  - `BaseModule` becomes `BaseModuleCommon with IWPublishModule with ScalafixModule`. No URL overrides.
  - New `BaseModuleNoPublish` becomes `BaseModuleCommon with ScalafixModule` (no `PublishModule`).
- Migrate to `BaseModuleNoPublish`: `scenarios` (jvm/js, build.mill:605), `scenariosUI` (build.mill:972), `filesUIScenarios` (jvm/js, build.mill:945), `formsScenarios` (jvm/js, build.mill:921).
- Remove `publishVersion = "0.0.0"` markers from `scenariosUI` (line 978), `filesUIScenarios` (line 956), `formsScenarios` (line 932). `scenarios` has no marker today; the trait change is what stops it publishing.
- `CommonVersion.publishVersion` bump: `0.1.13` → `0.1.14-SNAPSHOT` (build.mill:40).
- Verify `pomSettings` for every published module (description, MIT license, `works.iterative.support` org, GitHub VCS, `mprihoda` developer entry). Project context says these are already complete — re-read pass, not a rewrite.
- Drop the `//| repositories:` directive at `build.mill:1-5`. Verify with a clean-cache `./mill resolve __.compile`. If resolution fails, restore and file a plugin-migration follow-up.
- Confirm Mill's `cleanAll` / cache eviction works as expected in CI before the verification.

**Responsibilities:**
- Make `./mill __.publishLocal` succeed across the whole repo.
- Make `./mill resolve __.publishArtifacts` enumerate exactly the publishable modules — none of `scenarios`, `scenariosUI`, `files-ui-scenarios`, `forms-scenarios`.
- Preserve all current Scalafix/compile/scaladoc settings on every module (no behavior change from the trait restructure).
- Preserve `works.iterative.support` organization and existing artifact names (`iw-support-*`).
- Keep `IWPublishModule` as the publish trait (no diff to `mill-iw-support`).

**Acceptance criteria mapping:**
- "`./mill __.publishLocal` succeeds for the whole repo" → this layer.
- "`./mill resolve __.publishArtifacts` does not include the four internal modules" → this layer.

**Estimated Effort:** 3–6 hours
**Complexity:** Moderate

The shape uncertainty is in the `BaseModuleCommon` extraction: it touches the four non-publishing module sites (each with jvm/js variants in some cases) plus `BaseModule` itself, and we need to verify no inherited setting silently changes when we split the trait. The version bump and directive removal are mechanical.

---

### CI Layer (Stage B)

**Components:**
- `.github/workflows/publish.yml` — new file. Triggers and publish strategy:
  - Trigger 1: `push: branches: [main]` → snapshot publish (test gate per CLARIFY #4).
  - Trigger 2: `push: tags: ['v*']` → run test gate (CLARIFY #3), then publish.
  - Java 21 + Mill caching pattern reused from existing `.github/workflows/ci.yml`.
- Publish job runs `./mill __.publish` **twice**, once per destination, sequential and fail-fast:
  - Step 1 — e-BS Nexus: env `IW_PUBLISH_RELEASE_URI=https://nexus.e-bs.cz/repository/maven-releases/`, `IW_PUBLISH_SNAPSHOT_URI=https://nexus.e-bs.cz/repository/maven-snapshots/`, `MILL_SONATYPE_USERNAME=${{ secrets.EBS_NEXUS_USERNAME }}`, `MILL_SONATYPE_PASSWORD=${{ secrets.EBS_NEXUS_PASSWORD }}`.
  - Step 2 — GitHub Packages: env `IW_PUBLISH_RELEASE_URI=https://maven.pkg.github.com/iterative-works/support`, `IW_PUBLISH_SNAPSHOT_URI=https://maven.pkg.github.com/iterative-works/support`, `MILL_SONATYPE_USERNAME=${{ github.actor }}`, `MILL_SONATYPE_PASSWORD=${{ secrets.GITHUB_TOKEN }}`.
- Job-level `permissions: { contents: read, packages: write }` so `GITHUB_TOKEN` can publish to GH Packages.
- GH Actions secrets required (manual maintainer step, not in PR): `EBS_NEXUS_USERNAME`, `EBS_NEXUS_PASSWORD`. `GITHUB_TOKEN` is auto-provided.
- Test gate: at minimum `./mill __.test` before publish. Whether integration suites (`mongo.it`, `filesMongo.it`, `sqldb-postgresql.test`, `sqldb-mysql.test`) also run is CLARIFY #3.
- Snapshot-path test gate is CLARIFY #4.

**Responsibilities:**
- Automate snapshot publish on every `main` commit (per CLARIFY #4).
- Automate release publish on every `v*` tag, gated on the test target(s) chosen.
- Publish to BOTH destinations on every workflow run; fail-fast if either upload fails.
- Do NOT manage secret values (added manually by a maintainer with repo admin rights).

**Acceptance criteria mapping:**
- "Pushing a `v0.1.14` tag triggers the workflow and publishes successfully to both e-BS Nexus and GitHub Packages" → this layer.

**Estimated Effort:** 2–3 hours
**Complexity:** Straightforward

The shape is two near-identical publish blocks. Cost is in deciding the test gates (CLARIFY #3, #4), getting the YAML right on the first run (you can only validate by pushing), and confirming `GITHUB_TOKEN` has the right permissions for GH Packages writes from this repo's settings.

---

### Documentation Layer (Stage C)

**Components:**
- `PUBLISHING.md` — full rewrite. Documents:
  - Dual-publish flow (e-BS Nexus + GitHub Packages) with the workflow as the sanctioned path.
  - Tag-driven release process (`vX.Y.Z`).
  - Local snapshot publishing for development (`./mill __.publishLocal` — no credentials needed).
  - The two GitHub Actions secrets needed (`EBS_NEXUS_USERNAME`, `EBS_NEXUS_PASSWORD`); note `GITHUB_TOKEN` is automatic.
  - Consumer setup: how internal e-BS projects continue unchanged, and how non-e-BS consumers configure GH Packages with a PAT.
- `README.md` — currently 4 lines about UI scenarios dev (still references `sbtn`). Add:
  - Both sets of resolver coordinates (e-BS Nexus URLs + GitHub Packages URL) with consumer-side resolver-config snippets.
  - Mill and sbt dependency snippets (per CLARIFY #5).
- `publish.sh` — delete (root of repo, Nexus-shell-script-specific; CI is the sanctioned path now).

**Responsibilities:**
- Make the dual-publish flow self-documenting for future maintainers.
- Make consumer onboarding obvious from the README for both e-BS and non-e-BS audiences.

**Acceptance criteria mapping:**
- "`PUBLISHING.md` reflects the dual-publish flow; `publish.sh` is gone; `README.md` documents both consumer paths" → this layer.

**Estimated Effort:** 1–2 hours
**Complexity:** Straightforward

---

### Verification & Release Layer (Stage D)

**Components:**
- Local verification:
  - `./mill resolve __.publishArtifacts` — confirm the publishable set is correct (no `scenarios*`, `files-ui-scenarios`, `forms-scenarios`).
  - `./mill __.publishLocal` — confirm valid POMs land in `~/.ivy2/local`.
- Pre-tag CI sanity: push a small commit to `main`, observe the workflow uploads `0.1.14-SNAPSHOT` to BOTH destinations.
- Version bump: `0.1.14-SNAPSHOT` → `0.1.14`.
- Tag: `git tag v0.1.14 && git push --tags`.
- Workflow observation: watch the `publish.yml` run, confirm BOTH publish steps complete and artifacts appear at:
  - e-BS Nexus: `https://nexus.e-bs.cz/repository/maven-releases/works/iterative/support/iw-support-core_3/0.1.14/`
  - GitHub Packages: `https://github.com/iterative-works/support/packages`
- Consumer verification:
  - From an e-BS project (existing resolver config): resolve `0.1.14` from Nexus.
  - From a clean repo with a GH PAT in coursier credentials: resolve `0.1.14` from GH Packages.
  - Repeat for representative artifacts: `iw-support-tapir`, `iw-support-mongo`, `iw-support-sqldb`, `iw-support-server-http`, `iw-support-all`.

**Responsibilities:**
- Prove the dual-publish flow works end-to-end against both real registries.
- Mostly post-merge execution by a maintainer, not code-in-PR work.

**Acceptance criteria mapping:**
- "Artifacts at `0.1.14` are resolvable from BOTH e-BS Nexus AND GitHub Packages" → this layer.
- "Pushing a `v0.1.14` tag triggers the workflow and publishes successfully to both destinations" → this layer (jointly with CI layer).

**Estimated Effort:** 1–3 hours
**Complexity:** Moderate (uncertainty is in first-run debugging if either destination rejects something — Nexus credentials, GH Packages permission, or POM validation)

---

## Technical Decisions

### Patterns

- Dual-publish via env-var-configured `IWPublishModule` invoked twice in CI; same Mill task (`__.publish`), different env vars per step.
- Trait segregation for non-publishing modules: `BaseModuleCommon` (shared) → `BaseModule` (publish) and `BaseModuleNoPublish` (no publish).
- Tag-driven release with snapshot-on-main, fail-fast across both destinations.

### Technology Choices

- **Build tool:** Mill 1.1.2 (existing).
- **Publish trait:** `IWPublishModule` (kept; env-var configurable).
- **Destinations:** e-BS Nexus + GitHub Packages (Maven layout).
- **CI:** GitHub Actions, Java 21, Mill caching (pattern reused from existing `ci.yml`).
- **Signing:** GPG NOT used. Both destinations accept unsigned artifacts.

### Integration Points

- `build.mill` `BaseModule` ← consumed by every concrete published module. Trait restructure is cosmetic for consumers (same effective superclass after `BaseModuleCommon` factoring).
- `mill-iw-support::0.1.4` plugin ← still resolved at build-script load time. Working assumption: resolvable from Central / public Mill chain (per resolved decision on the `//|` directive).
- GitHub Actions ← reads two `EBS_NEXUS_*` secrets and the auto-provided `GITHUB_TOKEN`.

### Alternatives Considered (for stopping internal modules from publishing)

- **Option A (chosen): `BaseModuleNoPublish` trait via shared `BaseModuleCommon` parent.** Type-level intent; no markers; no drift between traits.
- **Option B: keep `publishVersion = "0.0.0"` markers.** Smallest diff, but smelly — Mill still treats the module as a `PublishModule` and the `0.0.0` artifact spec leaks into resolution.
- **Option C: exclude from `__.publishArtifacts` task pattern in CI.** Pushes the exclusion list into YAML where it'll silently drift from `build.mill` reality.

### Alternatives Considered (for destination strategy)

- **Option A (chosen): Dual-publish to e-BS Nexus + GitHub Packages.** Preserves e-BS workflows unchanged; decouples future consumption from e-BS infrastructure.
- **Option B: GitHub Packages only.** Cleaner, single destination — but forces every existing e-BS consumer to migrate their resolver config. Premature for the project's current state.
- **Option C: Sonatype Maven Central (original ticket framing).** Heavier ceremony (GPG, irrevocable releases, namespace verification) without matching value for a non-public-framework project.
- **Option D: Self-hosted Nexus / Artifactory on `iterative-works` infra.** Solves the "not e-BS" concern but trades it for ops work. Not warranted at this scale.

## Technical Risks & Uncertainties

### Decision: Stop internal modules from publishing via trait split with shared parent

**Resolved:** Introduce a `BaseModuleCommon` trait that holds the shared Scala/compile/scalafix configuration. `BaseModule` extends `BaseModuleCommon with IWPublishModule with ScalafixModule` (publishable). `BaseModuleNoPublish` extends `BaseModuleCommon with ScalafixModule` (no `PublishModule`). The four internal modules — `scenarios` (jvm/js), `scenariosUI`, `filesUIScenarios` (jvm/js), and `formsScenarios` (jvm/js) — switch to `BaseModuleNoPublish`. The `publishVersion = "0.0.0"` markers go away. This makes `__.publishArtifacts` resolve cleanly without exclusion patterns and prevents drift between the two traits.

---

### Decision: `formsScenarios` is internal-only and migrates to `BaseModuleNoPublish`

**Resolved:** `formsScenarios` (jvm/js) is treated identically to `scenariosUI` and `filesUIScenarios` — it migrates to `BaseModuleNoPublish` and the `publishVersion = "0.0.0"` marker (build.mill:932) is dropped. The full set of internal-only modules in Stage A is: `scenarios` (jvm/js), `scenariosUI`, `filesUIScenarios` (jvm/js), `formsScenarios` (jvm/js).

---

### Decision: Drop the `//| repositories:` Nexus directive

**Resolved:** `mill-iw-support::0.1.4` is believed to be on Central. As part of Stage A, verify this (e.g., with a clean-cache `./mill resolve __.compile` after deleting the directive) and drop lines 1-5 of `build.mill` entirely. If verification fails, fall back to keeping the directive and file a follow-up to migrate the plugin — but the working assumption is removal succeeds. After this change, contributor builds need only Central + the public Mill resolver chain; no Nexus access required.

---

### Decision: Dual-publish to e-BS Nexus + GitHub Packages

**Resolved:** Replace the original ticket's "Sonatype Central, drop Nexus" framing with **dual-publish**: same Mill task (`__.publish`) invoked twice in CI with destination-specific env vars. Existing e-BS consumers see no change. New / non-e-BS consumers resolve from `https://maven.pkg.github.com/iterative-works/support` with a GH PAT. `IWPublishModule` is kept (its env-var configurability is the dual-publish primitive). No GPG signing.

---

### Decision: Tag-publish test gate is `./mill __.test` only

**Resolved:** The publish workflow runs `./mill __.test` as the gate before either publish step on a `v*` tag. Integration suites (`mongo.it`, `filesMongo.it`, `sqldb-postgresql.test`, `sqldb-mysql.test`) are NOT gated by this workflow. The immediate goal is unblocking a consuming project that can't reach e-BS Nexus; richer CI gating can be added later without changing the publish flow's shape. Accept the small risk that an integration-only regression could ship at `0.1.14`; if that happens, fix-forward to `0.1.15`.

---

### Decision: Snapshot path runs `__.test` and publishes to GitHub Packages only

**Resolved:** On `push: branches: [main]`, the workflow runs `./mill __.test` as the gate, then publishes the `-SNAPSHOT` artifact to **GitHub Packages only**. Nexus does not receive snapshots — e-BS consumers only see tagged releases. This keeps the e-BS Nexus snapshot repo tidy, gives the GH Packages audience a fresh feedback loop on every gated `main` commit, and makes the dual-publish behavior asymmetric only on the snapshot path (tag path remains dual). Implementation note for `publish.yml`: branch the publish steps on `${{ github.ref_type }}` (or equivalent) so the Nexus step runs only on tag triggers.

---

### Decision: README uses `mvn"..."` Mill literal + sbt snippet + per-destination resolver block

**Resolved:** The `README.md` shows dependency snippets in two forms — Mill `mvn"works.iterative.support::iw-support-core::0.1.14"` (matching the original ticket text) and sbt `"works.iterative.support" %% "iw-support-core" % "0.1.14"`. Resolver configuration is a separate block per destination (e-BS Nexus on one side, GitHub Packages with PAT on the other) so consumers see the dependency snippet and the resolver requirement as distinct concerns.

---

## Total Estimates

**Per-Layer Breakdown:**
- Build Configuration Layer (Stage A): 3–6 hours
- CI Layer (Stage B): 2–3 hours
- Documentation Layer (Stage C): 1–2 hours
- Verification & Release Layer (Stage D): 1–3 hours

**Total Range:** 7–14 hours

**Confidence:** Medium

**Reasoning:**
- The build.mill refactor (`BaseModuleCommon` extraction + four-module migration) is the dominant uncertainty.
- The CI workflow nudged up vs. the Sonatype-only plan because dual-publish adds a second publish step pair and the secrets plumbing for both destinations.
- The verification layer's range is wide because first contact with two destinations can surface different failure modes (Nexus credential issues, GH Packages permission scope, POM validation).
- POM settings are already complete per project context — saves verification time.

## Recommended Phase Plan

Per the phase-size policy (3h floor, 7–14h total fits "12-24h → 2-5 phases" loosely; 7–14h is right at the boundary), we keep two phases — same shape as before, slightly larger Phase 1.

- **Phase 1: Dual-publish-ready build + workflow** (Stage A + Stage B)
  - Includes: `build.mill` refactor (`BaseModuleCommon` extraction, `BaseModuleNoPublish` introduction, version bump to `0.1.14-SNAPSHOT`, removal of `publishVersion = "0.0.0"` markers, removal of `//|` Nexus directive), `.github/workflows/publish.yml` with both publish steps.
  - Estimate: 5–9 hours
  - Rationale: Stage B alone (2–3h) is at the 3h floor and is tightly coupled to Stage A — the workflow's `__.publish` semantics depend on Stage A's module classification. One PR, one review, one merge.

- **Phase 2: Docs + release execution** (Stage C + Stage D)
  - Includes: `PUBLISHING.md` rewrite, `README.md` resolver coordinates and snippets, delete `publish.sh`. Then post-merge: `./mill resolve __.publishArtifacts` sanity check, `./mill __.publishLocal` dry run, version bump to `0.1.14`, tag `v0.1.14`, push, observe both publish destinations.
  - Estimate: 2–5 hours
  - Rationale: Stage C alone (1–2h) is below the floor and merges with Stage D. Stage D is largely post-merge maintainer execution; bundling it with the docs PR makes the docs land just before the tag, which is the right moment for them to be authoritative. `EBS_NEXUS_USERNAME` / `EBS_NEXUS_PASSWORD` secrets must be configured manually before the tag is pushed — call this out in the PR description.

**Total phases:** 2 (for total estimate 7–14 hours)

## Testing Strategy

### Per-Layer Testing

**Build Configuration Layer:**
- Local: `./mill __.publishLocal` succeeds across the whole repo. Verify POMs in `~/.ivy2/local/works.iterative.support/iw-support-*/0.1.14-SNAPSHOT/` look correct.
- Local: `./mill resolve __.publishArtifacts` enumerates only the publishable modules. Compare against the inventory.
- Local: `./mill __.compile` and `./mill __.test` still pass (regression check that the trait restructure didn't change Scala/compile settings).
- Local: clean-cache `./mill resolve __.compile` succeeds after the `//|` directive removal (verifies plugin resolves from public Mill chain).

**CI Layer:**
- First snapshot run: push to a feature branch, then `main`, observe the workflow runs both publish steps and both destinations accept the artifact.
- First tag run: tag `v0.1.14`, observe the test gate runs, then both publish steps land on both destinations.
- Negative case: push a `main` commit that fails the test gate (if Option B/C in CLARIFY #4), confirm publish is skipped.
- Negative case: simulate one destination's auth failure (locally), confirm the workflow reports it and does not silently succeed.

**Documentation Layer:**
- No automated tests. Manual review for accuracy: the snippets in `README.md` actually resolve from each destination, the `PUBLISHING.md` flow can be followed by someone unfamiliar with the repo.

**Verification & Release Layer:**
- e-BS path: from an existing e-BS project (no resolver-config change), resolve `iw-support-core::0.1.14` after the tag publish; confirm classpath wiring works.
- GH Packages path: from a clean repo with a GH PAT in coursier credentials, resolve the same artifact from GH Packages.
- Repeat for representative artifacts across the inventory: `iw-support-tapir`, `iw-support-mongo`, `iw-support-sqldb`, `iw-support-server-http`, `iw-support-all`.

**Test Data Strategy:**
- Not applicable (no application data).

**Regression Coverage:**
- Existing `ci.yml` tests must continue to pass on `main` after the build.mill changes.
- e-BS internal consumers continue to resolve `iw-support` from Nexus without resolver-config changes — the dual-publish design preserves this property by construction.

## Deployment Considerations

### Database Changes
None.

### Configuration Changes
- Two GitHub Actions secrets must be configured in repo settings before the first tag push: `EBS_NEXUS_USERNAME`, `EBS_NEXUS_PASSWORD`. Manual maintainer step, not part of any PR.
- `GITHUB_TOKEN` is automatic; `permissions: { packages: write }` at the job level is required so it can push to GH Packages.

### Rollout Strategy
- Phase 1 PR merge does not publish anything externally — the new workflow only activates on the next `main` push or `v*` tag.
- Maintainers should ensure `EBS_NEXUS_USERNAME` / `EBS_NEXUS_PASSWORD` secrets are configured before merging Phase 1 (otherwise the first `main` push after merge will fail at the Nexus publish step).
- Phase 2 PR merge does not publish anything externally either. Publication happens when a maintainer pushes the `v0.1.14` tag.

### Rollback Plan
- If the snapshot path malfunctions on either destination: revert the workflow file or disable it in Actions settings. Snapshots are mutable on both Nexus and GH Packages; a bad snapshot doesn't pollute permanent state.
- If the tag publish fails on Nexus only: re-run the workflow after fixing the credential issue. Nexus accepts overwrites of the same coordinates by default.
- If the tag publish fails on GH Packages only: re-run the workflow after fixing the permission/auth issue. GH Packages allows re-publishing or deletion of the same coordinates.
- If the build.mill changes break internal consumers: revert the PR; e-BS Nexus still has `0.1.13` and earlier intact.
- Version skew between destinations after a partial-failure release: re-run the workflow until both succeed for the same tag. Avoid bumping to `0.1.15` to fix a transient upload failure.

## Dependencies

### Prerequisites
- A maintainer with repo admin rights available to add the two `EBS_NEXUS_*` secrets before the first `main` push after Phase 1 merge.
- The existing e-BS Nexus credentials (already used by `publish.sh` locally) are available for transfer to GH Actions.

### Layer Dependencies
- Stage A (build) → Stage B (CI) — workflow's publish steps depend on Stage A's module classification.
- Stage A → Stage D — local `publishLocal` / `resolve` depend on the build changes.
- Stage B → Stage D — tag-driven release depends on the workflow.
- Stage C (docs) is parallelizable with Stage A/B but bundled into Phase 2 by phase-sizing policy.

### External Blockers
- Nothing external is blocking. Both destinations are operational; `iterative-works/support` repo exists; namespace and credentials are already in maintainer hands.

## Risks & Mitigations

### Risk 1: Version skew between destinations on a partial publish failure
**Likelihood:** Low to Medium
**Impact:** Low (recoverable by re-running the workflow)
**Mitigation:** Sequential publish steps with fail-fast — workflow goes red on any failure, maintainer re-runs after fixing. Both destinations accept overwrites of the same coordinates, so re-publish is safe. Document the recovery procedure in `PUBLISHING.md`.

### Risk 2: Build.mill refactor changes inherited settings unintentionally
**Likelihood:** Low
**Impact:** Medium
**Mitigation:** Run `./mill __.compile` and `./mill __.test` after the trait restructure. The `BaseModuleCommon` parent shared between `BaseModule` and `BaseModuleNoPublish` prevents drift by construction.

### Risk 3: Verification of `mill-iw-support::0.1.4` on Central fails after `//|` directive removal
**Likelihood:** Low (working assumption is the plugin is on Central)
**Impact:** Medium (would force restoring the directive and filing a plugin-migration follow-up)
**Mitigation:** Verify with a clean-cache `./mill resolve __.compile` after deleting the directive in Stage A. If resolution fails, restore the directive, document the constraint in `PUBLISHING.md`, and file a follow-up to migrate the plugin to Central.

### Risk 4: Snapshot publish from `main` lands a broken artifact on one or both destinations
**Likelihood:** Low to Medium (depends on CLARIFY #4)
**Impact:** Low (snapshots are mutable on both destinations)
**Mitigation:** Choose Option B or C in CLARIFY #4. Otherwise, accept the small risk for snapshot speed.

### Risk 5: `GITHUB_TOKEN` lacks `packages: write` scope
**Likelihood:** Low
**Impact:** Medium (GH Packages publish would fail; Nexus still works)
**Mitigation:** Set `permissions: { contents: read, packages: write }` at the job level in `publish.yml`. Verify on the first run by inspecting the publish step output.

---

## Implementation Sequence

**Recommended Layer Order:**

1. **Build Configuration Layer (Stage A)** — first, because every other layer depends on `__.publishArtifacts` and `BaseModule` being correct.
2. **CI Layer (Stage B)** — second, because the workflow's task semantics depend on Stage A's module classification.
3. **Documentation Layer (Stage C)** — third, but parallelizable with Stage B inside Phase 2's PR. Docs reference the workflow that Stage B introduces, so docs must land at or after Stage B.
4. **Verification & Release Layer (Stage D)** — fourth and final, executed mostly post-merge by a maintainer. The version bump to `0.1.14` and the tag push happen here, not in any PR.

**Ordering Rationale:**
- Stages A and B are tightly coupled; one PR is the right shape.
- Stages C and D both happen outside the dev loop (docs are reviewable, release is mechanical), so they share Phase 2.
- No two layers are truly parallelizable in time, but Stage C can be drafted in parallel with Stage A's review.

## Documentation Requirements

- [ ] Inline comments in `build.mill` explaining the `BaseModuleCommon` / `BaseModule` / `BaseModuleNoPublish` split.
- [ ] `PUBLISHING.md` rewritten for the dual-publish flow (Nexus + GH Packages), the two GH Actions secrets, partial-failure recovery, and the contributor `publishLocal` path.
- [ ] `README.md` updated with both destinations' coordinates, resolver-config snippets (especially for GH Packages consumers needing PAT auth), and Mill/sbt usage snippets.
- [ ] No architecture decision record needed (this is an infra/CI change, not a domain pattern shift).
- [ ] No user-facing application documentation needed.
- [ ] No migration guide needed for internal consumers (dual-publish preserves their resolver config by construction).
- [ ] Workflow file (`.github/workflows/publish.yml`) self-documents via comments where the test gate composition is non-obvious.

---

**Analysis Status:** Ready for Review

**Next Steps:**
1. All CLARIFY markers resolved. Seven decisions recorded inline.
2. Run **wf-create-tasks** with the issue ID.
3. Run **wf-implement** for layer-by-layer implementation.
