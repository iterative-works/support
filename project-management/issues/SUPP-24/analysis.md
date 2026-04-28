# Technical Analysis: Publish to Sonatype Maven Central (drop Nexus from this repo)

**Issue:** SUPP-24
**Created:** 2026-04-28
**Status:** Draft

## Problem Statement

Downstream projects without access to e-BS Nexus cannot consume `iw-support` artifacts today. The current build is wired exclusively for Nexus through `IWPublishModule` (from `mill-iw-support`), which hard-codes Nexus URLs by overriding `sonatypeLegacyOssrhUri` and `sonatypeCentralSnapshotUri` and disables GPG signing. Even when Sonatype credentials are pointed at the build, artifacts end up on the wrong host.

The value of fixing this: open-source consumers and any internal project that doesn't live behind the e-BS Nexus VPN can resolve `works.iterative.support` artifacts directly from Maven Central. Internal projects keep working unchanged because the e-BS Nexus already proxies Central.

## Proposed Solution

### High-Level Approach

Switch this repository's publish path from Nexus to Sonatype Maven Central. Concretely:

1. Replace `IWPublishModule` with plain `PublishModule` in `BaseModule` and let Mill 1.x's bundled `SonatypeCentralPublishModule/publishAll` route artifacts to Central via the already-verified `works.iterative` namespace.
2. Stop the three internal-only modules (`scenarios`, `scenariosUI`, `files-ui-scenarios`, and possibly `formsScenarios`) from publishing — cleanly, not via the `publishVersion = "0.0.0"` hack.
3. Add a `.github/workflows/publish.yml` that mirrors the `claude-code-query` style — snapshots from `main`, releases on `v*` tag with a test gate.
4. Rewrite `PUBLISHING.md`, refresh `README.md` with Sonatype coordinates, delete `publish.sh`.
5. Verify locally (`./mill __.publishLocal`, `./mill resolve __.publishArtifacts`), bump version to `0.1.14`, tag, and observe the workflow publish to Central.

`IWPublishModule` itself stays in `mill-iw-support` for downstream projects that still want Nexus — we just stop using it here.

### Why This Approach

The issue's decisions are locked: Central only, no Nexus, snapshots from `main`, releases on tag, organization stays `works.iterative.support`, initial Central version `0.1.14`. The technical mechanism — plain `PublishModule` plus Mill's bundled `SonatypeCentralPublishModule/publishAll` task — is the supported, documented path in Mill 1.x. There's no value in inventing a custom publish module here when Mill's defaults already do the right thing once the `IWPublishModule` overrides are removed.

The non-trivial design question is how to stop internal-only modules from publishing. Three options exist (see Technical Decisions); we recommend introducing a `BaseModuleNoPublish` trait that does not mix in `PublishModule`, because it (a) makes intent explicit in the type hierarchy, (b) eliminates the `publishVersion = "0.0.0"` smell, and (c) makes `__.publishArtifacts` resolve cleanly without exclusion patterns.

## Architecture Design

**Purpose:** Define WHAT each stage-as-layer needs, not HOW each line is written. This is build/CI infrastructure, so the conventional domain/application/infrastructure/presentation layering does not apply — we use the issue's Stage A–D as layers.

### Build Configuration Layer (Stage A)

**Components:**
- `build.mill` `BaseModule` trait — swap `IWPublishModule with ScalafixModule` for `PublishModule with ScalafixModule` (ScalafixModule must be preserved).
- New `BaseModuleNoPublish` trait (recommended) — the same Scala/compile/options surface as `BaseModule`, but without `PublishModule`. Used by `scenarios`, `scenariosUI`, `filesUIScenarios`, and (pending CLARIFY) `formsScenarios`.
- Removal of `sonatypeLegacyOssrhUri` / `sonatypeCentralSnapshotUri` / signing overrides (these live in `IWPublishModule`, so dropping the trait is sufficient — no per-module changes).
- Removal of `publishVersion = "0.0.0"` markers in `scenariosUI` (line 978), `filesUIScenarios` (line 956), and `formsScenarios` (line 932 — pending CLARIFY).
- Add explicit non-publishing for `scenarios` (jvm/js, build.mill:605), which currently has no marker and would publish today.
- `CommonVersion.publishVersion` bump: `0.1.13` → `0.1.14-SNAPSHOT` (build.mill:40).
- Verification of `pomSettings` for every published module: description, MIT license, `works.iterative.support` org, GitHub VCS, `mprihoda` developer entry. Project context says these are already complete — this is a re-read pass, not a rewrite.
- `//| repositories:` directive at `build.mill:1-5` — CLARIFY whether to keep nexus.e-bs.cz (used to fetch the `mill-iw-support` plugin itself, which is likely not on Central yet) or migrate.

**Responsibilities:**
- Make `./mill __.publishLocal` succeed across the repo with no Nexus configured.
- Make `./mill resolve __.publishArtifacts` enumerate exactly the publishable modules — no `scenarios`, `scenariosUI`, `files-ui-scenarios` (and `formsScenarios` if confirmed).
- Preserve all current Scalafix/scaladoc/compile settings on every module.
- Preserve `works.iterative.support` organization and existing artifact names (`iw-support-*`).

**Acceptance criteria mapping:**
- "`./mill __.publishLocal` succeeds for the whole repo" → this layer.
- "`./mill resolve __.publishArtifacts` does not include the three internal modules" → this layer.

**Estimated Effort:** 3–6 hours
**Complexity:** Moderate

The shape uncertainty is in the `BaseModuleNoPublish` refactor: it touches four module sites (each with jvm/js variants in some cases), and we need to verify that no inherited setting silently changes when we drop `PublishModule`. The version bump and the `BaseModule` swap are mechanical.

---

### CI Layer (Stage B)

**Components:**
- `.github/workflows/publish.yml` — new file, mirrors `claude-code-query/.github/workflows/publish.yml`.
  - Trigger 1: `push: branches: [main]` → snapshot publish via `mill.javalib.SonatypeCentralPublishModule/publishAll --publishArtifacts __.publishArtifacts` against the `-SNAPSHOT` version.
  - Trigger 2: `push: tags: ['v*']` → run test gate, then `publishAll`.
  - Java 21 + Mill caching pattern reused from existing `.github/workflows/ci.yml`.
- Secrets used in workflow: `MILL_PGP_SECRET_BASE64`, `MILL_SONATYPE_USERNAME`, `MILL_SONATYPE_PASSWORD`. The workflow file references them; the actual secret values are configured manually in repo settings (out-of-PR, maintainer step).
- Test gate: at minimum `./mill __.test` before tag publish. Whether `__.itest` (or this repo's integration suites: `mongo.it`, `filesMongo.it`, `sqldb-postgresql.test`, etc.) also runs is a CLARIFY because those suites need Testcontainers/DBs.
- Snapshot-path test gate: CLARIFY whether `main` push runs tests or skips for speed.

**Responsibilities:**
- Automate snapshot publish on every `main` commit.
- Automate release publish on every `v*` tag, gated on the test target(s) chosen.
- Do NOT manage secret values (those are added manually by a maintainer with repo admin rights).

**Acceptance criteria mapping:**
- "Pushing a `v0.1.14` tag triggers the workflow and publishes successfully to Sonatype Central" → this layer.

**Estimated Effort:** 1–2 hours
**Complexity:** Straightforward

The workflow shape is a near-direct copy of the reference. The cost is in deciding the test gate composition (a CLARIFY) and getting the YAML right on the first run, since you can only test it by pushing.

---

### Documentation Layer (Stage C)

**Components:**
- `PUBLISHING.md` — full rewrite. Documents:
  - Sonatype Central + GPG flow.
  - Tag-driven release process (`vX.Y.Z`).
  - Local snapshot publishing for development (`./mill __.publishLocal` — does NOT require GPG).
  - The three required GitHub Actions secrets (names + how to obtain).
- `README.md` — currently 4 lines about UI scenarios dev (still references `sbtn`). Add:
  - Sonatype Maven Central coordinates.
  - Mill usage snippet (`ivy"works.iterative.support::iw-support-core::0.1.14"` — note: issue text uses `mvn"..."`; CLARIFY which dependency-spec syntax we want in docs).
  - sbt usage snippet.
- `publish.sh` — delete (root of repo, Nexus-specific).

**Responsibilities:**
- Make the Central + tag flow self-documenting for future maintainers.
- Make consumer onboarding obvious from the README.

**Acceptance criteria mapping:**
- "`PUBLISHING.md` reflects the new flow; `publish.sh` is gone" → this layer.

**Estimated Effort:** 1–2 hours
**Complexity:** Straightforward

---

### Verification & Release Layer (Stage D)

**Components:**
- Local verification:
  - `./mill resolve __.publishArtifacts` — confirm the publishable set is correct (no `scenarios*`, no `files-ui-scenarios`, possibly no `formsScenarios`).
  - `./mill __.publishLocal` — confirm valid POMs land in `~/.ivy2/local`.
- Version bump: `0.1.14-SNAPSHOT` → `0.1.14`.
- Tag: `git tag v0.1.14 && git push --tags`.
- Workflow observation: watch the `publish.yml` run, confirm Central reflects the artifacts.
- Consumer verification: from a clean repo with NO Nexus configured, run `mvn`/Mill resolve against `works.iterative.support::iw-support-core::0.1.14` and confirm it resolves.

**Responsibilities:**
- Prove the build+CI work end-to-end against the real Central infrastructure.
- This is mostly post-merge execution by a maintainer, not code-in-PR work.

**Acceptance criteria mapping:**
- "Artifacts are resolvable via `mvn"works.iterative.support::iw-support-core::0.1.14"` (and equivalent for every published module) from a clean repo with **no Nexus configured**" → this layer.
- "Pushing a `v0.1.14` tag triggers the workflow and publishes successfully to Sonatype Central" → this layer (jointly with CI layer).

**Estimated Effort:** 1–3 hours
**Complexity:** Moderate (uncertainty is in the first-run debugging if Central rejects something — namespace, signing, or POM validation)

---

## Technical Decisions

### Patterns

- Plain Mill `PublishModule` + `mill.javalib.SonatypeCentralPublishModule/publishAll` (built-in Mill 1.x publish path).
- Trait segregation for non-publishing modules: separate `BaseModule` (publish) from `BaseModuleNoPublish` (no publish), rather than overloading a single trait with skip flags.
- Tag-driven release with snapshot-on-main, mirroring `claude-code-query`.

### Technology Choices

- **Build tool:** Mill 1.1.2 (existing).
- **Publish path:** Mill's bundled `SonatypeCentralPublishModule` (no third-party plugin).
- **CI:** GitHub Actions, Java 21, Mill caching (pattern reused from existing `ci.yml`).
- **Signing:** GPG via `MILL_PGP_SECRET_BASE64` secret — required for Central releases, NOT required for `publishLocal`.

### Integration Points

- `build.mill` `BaseModule` ← consumed by every concrete module. Changing the trait is a one-line swap but ripples to every module's compiled output (no behavior change expected, but worth a clean rebuild).
- `mill-iw-support::0.1.4` plugin ← still resolved at build-script time. If it's not on Central, the `//| repositories:` directive must continue to point at Nexus to fetch it (CLARIFY).
- GitHub Actions ← reads three secrets that are configured out-of-band.

### Alternatives Considered (for stopping internal modules from publishing)

- **Option A (recommended): `BaseModuleNoPublish` trait.** Make non-publishability a type-level concern. Pros: explicit, no markers, `__.publishArtifacts` resolves cleanly. Cons: introduces a second trait that must stay in sync with `BaseModule`'s Scala/compile settings.
- **Option B: keep `publishVersion = "0.0.0"` markers.** Continue the current hack on every non-publishing module. Pros: zero refactor. Cons: smelly — Mill still treats the module as a `PublishModule`, the `0.0.0` artifact may still appear in `__.publishArtifacts` resolution depending on Mill's filter behavior, and it's easy to miss when adding new internal modules.
- **Option C: exclude from `__.publishArtifacts` task pattern.** Use a more targeted Mill selector in `publish.yml` instead of `__.publishArtifacts`. Pros: no `build.mill` refactor. Cons: pushes the exclusion list into CI, where it will silently drift from `build.mill` reality.

Recommendation: **Option A**, document the trait pairing in `build.mill`.

## Technical Risks & Uncertainties

### CLARIFY: How do we stop internal modules from publishing — trait split, marker version, or task-pattern exclusion?

The issue says "Stop internal modules from extending `PublishModule`: `scenarios`, `scenariosUI`, `files-ui-scenarios`. Drop the `publishVersion = "0.0.0"` 'don't publish' markers." That language points at Option A (trait split), but the implementer should confirm before refactoring four module sites.

**Questions to answer:**
1. Are we comfortable introducing a `BaseModuleNoPublish` trait that duplicates the Scala/compile config of `BaseModule` minus `PublishModule`?
2. Do we want the two traits to share a common `BaseModuleCommon` parent that holds the shared settings, to avoid drift?
3. Is there a Mill-idiomatic way to do this that we're missing (e.g., `skipIdea`-style flags)?

**Options:**
- **Option A**: `BaseModuleNoPublish` trait. Explicit, requires touching four module sites.
- **Option B**: Keep `publishVersion = "0.0.0"` markers (status quo for three of four modules).
- **Option C**: Exclude in CI via a narrower Mill selector.

**Impact:** Affects `build.mill` shape and the size of Stage A.

---

### CLARIFY: Does `formsScenarios` follow the same non-publishing treatment?

`formsScenarios` (build.mill:921) currently has `publishVersion = "0.0.0"` (line 932). The issue does not list it among the modules to stop publishing, but its current state is identical to `scenariosUI` and `filesUIScenarios`.

**Questions to answer:**
1. Is `formsScenarios` an internal-only module like the others, or is it intended to publish from `0.1.14` onward?
2. If it's internal-only, should it also be migrated to `BaseModuleNoPublish` in this issue?

**Options:**
- **Option A**: Treat `formsScenarios` exactly like `scenariosUI` (move to `BaseModuleNoPublish`).
- **Option B**: Leave `formsScenarios` with the `publishVersion = "0.0.0"` marker (out of scope for this issue).
- **Option C**: Promote `formsScenarios` to publishing (set a real version, add to acceptance criteria).

**Impact:** Adds one more module site to Stage A's refactor if Option A; otherwise none.

---

### CLARIFY: Keep `nexus.e-bs.cz` in the `//| repositories:` directive at build.mill:1-5?

That directive is how Mill fetches the `mill-iw-support::0.1.4` plugin itself at build-script load time. If the plugin is not on Central, removing the directive will break the build before any module compiles.

**Questions to answer:**
1. Is `works.iterative::mill-iw-support::0.1.4` published to Central, or only to e-BS Nexus?
2. If only Nexus: do we keep the directive (since this repo's build still depends on it), even though we're moving the repo's own publishing off Nexus?
3. Do we add a follow-up issue to migrate `mill-iw-support` to Central?

**Options:**
- **Option A**: Keep the Nexus repository directive — accept that the build script still pulls the plugin from Nexus, even though this repo's outputs go to Central. Document this in `PUBLISHING.md`.
- **Option B**: Verify the plugin is on Central; if so, remove the directive entirely.
- **Option C**: Migrate `mill-iw-support` to Central as a prerequisite (out of scope per issue text — `mill-iw-support` migration is a separate follow-up).

**Impact:** Affects whether the build works for contributors with no e-BS Nexus access. This intersects with one of the issue's stated motivations (downstream-without-Nexus consumers — but consumers don't need the plugin, only contributors do).

---

### CLARIFY: What's the test gate composition for the publish workflow?

`claude-code-query` runs `__.test` then `__.itest`. This repo has no `__.itest` — integration suites live in module-scoped paths like `mongo.it`, `filesMongo.it`, `sqldb-postgresql.test`. They typically need Testcontainers or live DBs.

**Questions to answer:**
1. On `v*` tag publish, is `./mill __.test` enough as the gate, or must integration suites also run?
2. If integration suites run, are we comfortable provisioning Testcontainers in the publish workflow?
3. If we run integration suites, which selector(s) do we use?

**Options:**
- **Option A**: `./mill __.test` only on tag publish — fast, covers basic suites. Risk: an integration regression could ship.
- **Option B**: `./mill __.test` plus a curated list of integration selectors (`mongo.it`, `filesMongo.it`, `sqldb-postgresql.test`, `sqldb-mysql.test`). Risk: workflow complexity, Testcontainers cost.
- **Option C**: Reuse the existing `ci.yml` test pattern verbatim in `publish.yml`.

**Impact:** Affects `publish.yml` shape and CI runtime per release.

---

### CLARIFY: Does the snapshot path on `main` also run tests?

The issue says "snapshot publish (no tests required for snapshots, or with tests — pick same as claude-code-query reference at implementation time)". `claude-code-query` does NOT publish snapshots from `main` — its workflow is tag-only — so the reference is silent on this question.

**Questions to answer:**
1. Should every `main` commit publish a `-SNAPSHOT` artifact regardless of test status?
2. Or should snapshot publish be gated on the same test target as the existing `ci.yml`?

**Options:**
- **Option A**: Skip tests on snapshot path — fastest, but a broken snapshot could land on Central.
- **Option B**: Run `__.test` on snapshot path — symmetric with tag path, slower per commit.
- **Option C**: Rely on `ci.yml`'s existing test pass for the `main` push — only run publish if `ci.yml` passed (workflow_run trigger).

**Impact:** Affects `publish.yml` shape and how confident we are in the freshness of `-SNAPSHOT` artifacts.

---

### CLARIFY: GPG setup expectations for contributors running `publishLocal`?

Mill's `publishLocal` does NOT require signing — it writes to `~/.ivy2/local`. Mill's `publishAll` against Central DOES require GPG. The issue says "Document local snapshot publishing for development."

**Questions to answer:**
1. Does `PUBLISHING.md` instruct contributors to set up GPG locally, or only describe it as needed for CI?
2. If a contributor wants to dry-run a Central publish locally, do we document that path or steer them to the workflow only?

**Options:**
- **Option A**: Document `publishLocal` as the contributor path (no GPG needed); CI is the only path to Central.
- **Option B**: Document both `publishLocal` and a contributor-side `publishAll` flow (requires GPG setup).
- **Option C**: CI-only Central publish path, with `publishLocal` for everything else; explicitly say contributors do not need GPG.

**Impact:** Affects `PUBLISHING.md` length and the contributor ramp-up cost.

---

### CLARIFY: Dependency syntax in README — `ivy"..."`, `mvn"..."`, or both?

The issue's acceptance text uses `mvn"works.iterative.support::iw-support-core::0.1.14"`. Mill 1.x uses `ivy"..."` historically and is migrating to `mvn"..."` (Mill's newer dependency literal). Sbt users use `"works.iterative.support" %% "iw-support-core" % "0.1.14"`.

**Questions to answer:**
1. Which Mill dependency-literal form do we put in the README — `ivy` (familiar) or `mvn` (per the issue)?
2. Do we provide an sbt snippet alongside the Mill snippet?

**Options:**
- **Option A**: `mvn"..."` per the issue text, plus an sbt snippet.
- **Option B**: `ivy"..."` (Mill 1.x default), plus an sbt snippet.
- **Option C**: Both Mill forms plus sbt.

**Impact:** Cosmetic; affects `README.md` only.

---

## Total Estimates

**Per-Layer Breakdown:**
- Build Configuration Layer (Stage A): 3–6 hours
- CI Layer (Stage B): 1–2 hours
- Documentation Layer (Stage C): 1–2 hours
- Verification & Release Layer (Stage D): 1–3 hours

**Total Range:** 6–13 hours

**Confidence:** Medium

**Reasoning:**
- The build.mill refactor for non-publishing modules is the dominant uncertainty — depends on which option for the BaseModuleNoPublish split is chosen and whether `formsScenarios` is included.
- The CI workflow is a near-direct copy of `claude-code-query/publish.yml`, but first-run YAML iteration can chew time.
- The verification layer's range is wide because first contact with Central can surface POM/signing/namespace issues that aren't visible locally.
- POM settings are already complete per project context — saves verification time.

## Recommended Phase Plan

Per the phase-size policy (3h floor, 6–13h total fits "4-12h → 1-3 phases" / "12-24h → 2-5 phases"), we group by code-vs-non-code and merge smaller layers in dependency order.

- **Phase 1: Sonatype-ready build + workflow** (Stage A + Stage B)
  - Includes: `build.mill` refactor (`BaseModule` swap, `BaseModuleNoPublish` introduction, version bump to `0.1.14-SNAPSHOT`, removal of `publishVersion = "0.0.0"` markers), `.github/workflows/publish.yml`.
  - Estimate: 4–8 hours
  - Rationale: Stage B alone (1–2h) is below the 3h floor and must merge with an adjacent layer in dependency order. Stage A is the only upstream candidate and is tightly coupled — the workflow's `__.publishArtifacts` selector only resolves correctly once Stage A's module classification is right. One PR, one review, one merge.

- **Phase 2: Docs + release execution** (Stage C + Stage D)
  - Includes: `PUBLISHING.md` rewrite, `README.md` Sonatype coordinates, delete `publish.sh`, then post-merge: `./mill resolve __.publishArtifacts` sanity check, `./mill __.publishLocal` dry run, version bump to `0.1.14`, tag `v0.1.14`, push, observe.
  - Estimate: 2–5 hours
  - Rationale: Stage C alone (1–2h) is below the floor and merges with Stage D. Stage D is largely post-merge maintainer execution; bundling it with the docs PR makes the docs land just before the tag, which is the right moment for them to be authoritative. Secrets must be configured manually before the tag is pushed — call this out in the PR description.

**Total phases:** 2 (for total estimate 6–13 hours)

## Testing Strategy

### Per-Layer Testing

**Build Configuration Layer:**
- Local: `./mill __.publishLocal` succeeds across the whole repo. Verify POMs in `~/.ivy2/local/works.iterative.support/iw-support-*/0.1.14-SNAPSHOT/` look correct.
- Local: `./mill resolve __.publishArtifacts` enumerates only the publishable modules. Compare against the inventory in the issue.
- Local: `./mill __.compile` and `./mill __.test` still pass (regression check that the trait swap didn't change Scala/compile settings).

**CI Layer:**
- First snapshot run: push to a feature branch, then `main`, observe the workflow runs `publishAll` and the Central snapshot endpoint accepts the artifact.
- First tag run: tag `v0.1.14`, observe the test gate runs and `publishAll` lands on the Central portal endpoint.
- Negative case: push a `main` commit that fails the test gate (if we choose Option B/C in the snapshot-test CLARIFY), confirm publish is skipped.

**Documentation Layer:**
- No automated tests. Manual review for accuracy: the snippets in `README.md` actually resolve, the `PUBLISHING.md` flow can be followed by someone unfamiliar with the repo.

**Verification & Release Layer:**
- From a clean repo with no Nexus configured (e.g., a fresh container), run `./mill resolve` against `ivy"works.iterative.support::iw-support-core::0.1.14"` (or `mvn"..."` — pending CLARIFY) and confirm it resolves from Central.
- Repeat for representative artifacts across the inventory: `iw-support-tapir`, `iw-support-mongo`, `iw-support-sqldb`, `iw-support-server-http`, `iw-support-all`.

**Test Data Strategy:**
- Not applicable (no application data).

**Regression Coverage:**
- Existing `ci.yml` tests must continue to pass on `main` after the build.mill changes.
- Internal consumers that resolve `iw-support` from e-BS Nexus must continue to work — this is preserved automatically because Nexus proxies Central, but worth a manual sanity check by pulling from one internal project after `0.1.14` ships.

## Deployment Considerations

### Database Changes
None.

### Configuration Changes
- Three new GitHub Actions secrets must be configured in repo settings before the first tag push: `MILL_PGP_SECRET_BASE64`, `MILL_SONATYPE_USERNAME`, `MILL_SONATYPE_PASSWORD`. This is a manual maintainer step, not part of any PR.
- The `works.iterative` namespace must already be verified on Central (project context says it is).

### Rollout Strategy
- Phase 1 PR merge does not publish anything externally — `0.1.14-SNAPSHOT` only goes to Central if a `main` push triggers the (newly-added) workflow. Maintainers should ensure secrets are configured before merging Phase 1.
- Phase 2 PR merge does not publish anything externally either. Publication happens when a maintainer pushes the `v0.1.14` tag.

### Rollback Plan
- If the snapshot path malfunctions: revert the workflow file or disable it in Actions settings. Snapshots on Central are mutable, so a bad snapshot doesn't pollute permanent state.
- If the tag publish malfunctions: a Central release cannot be deleted, but it can be superseded by `0.1.15` with a fix. Avoid `0.1.14` re-publish — Central rejects re-publish of the same coordinates.
- If the build.mill changes break internal consumers: revert the PR; the e-BS Nexus still has `0.1.13` and earlier intact.

## Dependencies

### Prerequisites
- `works.iterative` namespace verified on Sonatype Central (project context confirms).
- A maintainer with repo admin rights available to add the three GitHub Actions secrets before the tag push.
- A maintainer with a published GPG key matching `MILL_PGP_SECRET_BASE64`.

### Layer Dependencies
- Stage A (build) → Stage B (CI) — workflow's `__.publishArtifacts` selector depends on Stage A's module classification.
- Stage A → Stage D — local `publishLocal`/`resolve` depend on the build changes.
- Stage B → Stage D — tag-driven release depends on the workflow.
- Stage C (docs) is parallelizable with Stage A/B but bundled into Phase 2 by phase-sizing policy.

### External Blockers
- Sonatype Central availability and namespace verification (already done).
- Whether `mill-iw-support::0.1.4` is on Central (relates to the `//| repositories:` CLARIFY).

## Risks & Mitigations

### Risk 1: First Central publish rejected for POM/signing/namespace reason
**Likelihood:** Medium
**Impact:** Medium
**Mitigation:** Run `./mill __.publishLocal` exhaustively in Phase 2 verification; cross-check generated POMs against Central's validation rules (license, scm, developers, signed artifacts). If rejection occurs, the tag can stay and we re-run `publishAll` after the fix — no version bump needed unless artifacts already partially landed.

### Risk 2: Build.mill refactor changes inherited settings unintentionally
**Likelihood:** Low
**Impact:** Medium
**Mitigation:** Run `./mill __.compile` and `./mill __.test` after the trait swap. If `BaseModuleNoPublish` is introduced, share a `BaseModuleCommon` parent so settings cannot drift.

### Risk 3: `mill-iw-support` plugin not on Central, breaking contributors without Nexus access
**Likelihood:** Medium (depends on CLARIFY resolution)
**Impact:** High (would block external contributors)
**Mitigation:** Keep the Nexus `//| repositories:` directive until `mill-iw-support` is migrated; document the constraint in `PUBLISHING.md`. File a follow-up issue to migrate the plugin.

### Risk 4: Snapshot publish from `main` lands a broken artifact on Central
**Likelihood:** Low to Medium (depends on snapshot-test-gate CLARIFY)
**Impact:** Low (Central snapshots are mutable, downstream is unlikely to pin to a specific snapshot)
**Mitigation:** Choose Option B or C in the snapshot-test-gate CLARIFY; otherwise, accept the small risk for snapshot speed.

### Risk 5: Internal consumers (resolving via Nexus proxy) experience a delay before `0.1.14` appears in their proxy
**Likelihood:** Medium
**Impact:** Low
**Mitigation:** Internal projects can force-refresh their Nexus proxy or pin `0.1.13` until the proxy catches up. This is a known proxy behavior and not specific to this change.

---

## Implementation Sequence

**Recommended Layer Order:**

1. **Build Configuration Layer (Stage A)** — first, because every other layer depends on `__.publishArtifacts` and `BaseModule` being correct.
2. **CI Layer (Stage B)** — second, because the workflow's task selectors depend on Stage A's module classification.
3. **Documentation Layer (Stage C)** — third, but parallelizable with Stage B inside Phase 2's PR. Docs reference the workflow that Stage B introduces, so docs must land at or after Stage B.
4. **Verification & Release Layer (Stage D)** — fourth and final, executed mostly post-merge by a maintainer. The version bump to `0.1.14` and the tag push happen here, not in any PR.

**Ordering Rationale:**
- Stages A and B are tightly coupled; one PR is the right shape.
- Stages C and D both happen outside the dev loop (docs are reviewable, release is mechanical), so they share Phase 2.
- No two layers are truly parallelizable in time, but Stage C can be drafted in parallel with Stage A's review.

## Documentation Requirements

- [ ] Inline comments in `build.mill` explaining the `BaseModule` / `BaseModuleNoPublish` split and why `IWPublishModule` was dropped.
- [ ] `PUBLISHING.md` rewritten for Sonatype Central + GPG + tag flow, including local snapshot publishing for contributors and the three required GitHub Actions secrets.
- [ ] `README.md` updated with Sonatype coordinates and Mill/sbt usage snippets.
- [ ] No architecture decision record needed (this is an infra/CI change, not a domain pattern shift).
- [ ] No user-facing application documentation needed.
- [ ] No migration guide needed for internal consumers (Nexus proxies Central, no consumer-side change required).
- [ ] Workflow file (`.github/workflows/publish.yml`) self-documents via comments where the test gate composition is non-obvious.

---

**Analysis Status:** Ready for Review

**Next Steps:**
1. Resolve CLARIFY markers with stakeholders (seven open clarifications: non-publishing strategy, `formsScenarios` treatment, Nexus repositories directive, tag test gate composition, snapshot test gate, GPG contributor expectations, README dependency syntax).
2. Run **wf-create-tasks** with the issue ID.
3. Run **wf-implement** for layer-by-layer implementation.
