# Phase 2: Documentation + release execution

**Issue:** SUPP-24
**Phase:** 2 of 2
**Estimate:** 2–5 hours
**Covers:** Stage C (Documentation Layer) + Stage D (Verification & Release Layer) from `analysis.md`.

## Goals

This phase has TWO logical halves with very different shapes. Keep the distinction front-of-mind throughout.

- **Stage C — in-PR documentation work.** Rewrite `PUBLISHING.md` so it documents the dual-publish flow (e-BS Nexus + GitHub Packages, GitHub Actions as the sanctioned release path, `./mill __.publishLocal` as the contributor path, the two `EBS_NEXUS_*` secrets and the auto-provided `GITHUB_TOKEN`, partial-failure recovery). Refresh `README.md` so it documents both consumer paths — Mill `mvn"..."` literal and sbt snippet for dependencies, per-destination resolver-config blocks (e-BS Nexus on one side, GitHub Packages with PAT on the other). Delete `publish.sh` (CI is the sanctioned publish path; the script is Nexus-shell-script-specific and obsolete after Phase 1's workflow). The PR for this phase contains exactly these three file changes.
- **Stage D — post-merge maintainer execution (runbook, NOT code-in-PR).** After the Phase 2 PR merges: run `./mill resolve __.publishArtifacts` as a post-merge sanity check, run `./mill __.publishLocal` as a dry run against the merged `main`, bump `CommonVersion.publishVersion` from `"0.1.14-SNAPSHOT"` to `"0.1.14"` and commit, tag `v0.1.14`, push the tag, observe both publish destinations on the GitHub Actions run, and verify consumer-side resolution from both registries against representative artifacts. The version bump commit happens AFTER the docs PR merges, immediately before the tag push — it is a separate, small commit on `main`, not part of the docs PR.

## Scope

**IN SCOPE (Stage C — in-PR work)**

- `PUBLISHING.md` — full rewrite. New content describes dual-publish via GitHub Actions, the tag-driven release process (`vX.Y.Z`), the snapshot-asymmetric publish behavior (snapshots on `main` go to GitHub Packages only; tagged releases go to BOTH destinations) per analysis "Decision: Snapshot path runs `__.test` and publishes to GitHub Packages only", the two GitHub Actions secrets (`EBS_NEXUS_USERNAME`, `EBS_NEXUS_PASSWORD`) and the auto-provided `GITHUB_TOKEN`, the contributor `./mill __.publishLocal` path (no credentials needed), and partial-failure recovery (re-run the workflow; both registries accept overwrites).
- `README.md` — refresh. Add a "Using iw-support" section with two dependency-snippet forms (Mill `mvn"works.iterative.support::iw-support-core::0.1.14"`, sbt `"works.iterative.support" %% "iw-support-core" % "0.1.14"`) and two resolver-config blocks (e-BS Nexus URLs; GitHub Packages with PAT) presented as separate concerns per analysis "Decision: README uses `mvn"..."` Mill literal + sbt snippet + per-destination resolver block". Update the existing UI scenarios development section to use `./mill` commands instead of the obsolete `sbtn` reference.
- `publish.sh` — delete.

**IN SCOPE (Stage D — post-merge runbook, NOT in any PR)**

- `./mill resolve __.publishArtifacts` sanity check on merged `main`.
- `./mill __.publishLocal` dry run on merged `main`.
- `CommonVersion.publishVersion` bump from `"0.1.14-SNAPSHOT"` to `"0.1.14"` in `build.mill` (line 40), as a small commit on `main`.
- Tag `v0.1.14` and push the tag.
- Observe the `publish.yml` workflow run; confirm BOTH publish steps complete and artifacts appear at the expected URLs on both registries.
- Consumer-side resolution checks against representative artifacts from both registries.

**OUT OF SCOPE**

- Any `build.mill` change other than the line-40 version bump in Stage D (the trait restructure is Phase 1, complete).
- Any `.github/workflows/publish.yml` change (Phase 1, complete).
- Configuring the `EBS_NEXUS_USERNAME` / `EBS_NEXUS_PASSWORD` GitHub Actions secrets — manual maintainer step with repo admin rights, NOT in any PR. Must be done before the Stage D tag push.
- Any change to `mill-iw-support`, `IWPublishModule`, or other shared infra (none required).
- Adding richer test gating beyond `./mill __.test` — explicitly deferred (analysis "Decision: Tag-publish test gate is `./mill __.test` only").
- Any user-facing application documentation, migration guide, or architecture decision record — none needed for this CI/infra change (per analysis "Documentation Requirements").

## Dependencies

**Prior phases:** Phase 1 is merged. Per `implementation-log.md` "For next phase":

- `CommonVersion.publishVersion` is `"0.1.14-SNAPSHOT"` at `build.mill:40`. Stage D bumps it to `"0.1.14"` for the release.
- `.github/workflows/publish.yml` exists with the dual-publish wiring (snapshot-on-`main` to GitHub Packages only, tag-on-`v*` dual to e-BS Nexus + GitHub Packages, `./mill __.test` gate, job-level `permissions: { contents: read, packages: write }`, sequential fail-fast steps). Untested live until the first `main` push or tag push lands.
- The `//| repositories:` Nexus directive at `build.mill:1-3` was removed in Phase 1 and verified to resolve cleanly from the public Mill plugin chain. `PUBLISHING.md` MUST NOT include any "Nexus access required for plugin resolution" caveat — that constraint no longer exists.
- The trait restructure (`BaseModuleCommon` / `BaseModule` / `BaseModuleNoPublish` / `BaseScalaJSModuleNoPublish` / `NoPublishCrossModule`) is in place; `__.publishArtifacts` is correct by construction. Stage D's `./mill resolve __.publishArtifacts` is a sanity check, not a defect search.

**External prerequisites for Stage D (NOT for the documentation PR itself):**

- `EBS_NEXUS_USERNAME` and `EBS_NEXUS_PASSWORD` GitHub Actions secrets MUST be configured in the repo settings before the Stage D tag push. Without them, the snapshot path on `main` continues to work (uses only `GITHUB_TOKEN`), but the FIRST tag push triggers the e-BS Nexus publish step and will fail at auth. This is a manual maintainer step with repo admin rights and is NOT part of the Phase 2 PR. Surface this prominently in the Phase 2 PR description as a Stage D prerequisite — the PR can merge without these secrets being in place, but the tag MUST NOT be pushed until they are.
- `GITHUB_TOKEN` is auto-provided by Actions; the `permissions: { packages: write }` declaration is already in `publish.yml` from Phase 1.
- A maintainer with the ability to push tags to `main` and observe Actions runs.

**External prerequisites for the Phase 2 PR itself:** None beyond a normal review/merge cycle.

## Approach

The architectural design and decisions are recorded in `analysis.md` ("Architecture Design" → "Documentation Layer (Stage C)" and "Verification & Release Layer (Stage D)") and "Technical Decisions". Refer there for rationale; this section describes execution.

### Stage C — documentation rewrite strategy

Two distinct readers must be served:

- `PUBLISHING.md` reader = a future maintainer wanting to release a new version, debug a failed publish, or understand the contributor publish path. Must answer: "What triggers a publish?" "What credentials does CI need?" "What if one destination fails partway through?" "How do I publish locally for testing without touching CI?"
- `README.md` reader = a consumer (internal e-BS project or external Michal-only project) wanting to add `iw-support-*` to their build. Must answer: "What's the dependency snippet?" "What resolver do I configure?" "What credentials does my resolver need?"

The current `PUBLISHING.md` (82 lines) describes the obsolete Nexus-only flow centered on `publish.sh`. Full rewrite is cleaner than incremental edits. Preserve nothing verbatim from the current file — the content shape changes (CI as the sanctioned path, not a script).

The current `README.md` (4 lines) only covers UI scenarios development and references `sbtn` (obsolete; the project moved from sbt to Mill). The refresh adds a "Using iw-support" section ahead of the existing UI scenarios section and corrects the `sbtn` reference to `./mill`.

Per analysis "Decision: README uses `mvn"..."` Mill literal + sbt snippet + per-destination resolver block", the dependency snippets and resolver-config blocks are presented as DISTINCT concerns. Concretely:

- **Dependency snippet — Mill form:** `mvn"works.iterative.support::iw-support-core::0.1.14"` (the original ticket text uses this form; use it verbatim). Show one example artifact (`iw-support-core`); list the full set of artifacts (`iw-support-tapir`, `iw-support-mongo`, `iw-support-sqldb`, `iw-support-server-http`, `iw-support-all`, etc.) elsewhere or by reference.
- **Dependency snippet — sbt form:** `"works.iterative.support" %% "iw-support-core" % "0.1.14"`.
- **Resolver block — e-BS Nexus:** Mill and sbt snippets pointing at `https://nexus.e-bs.cz/repository/maven-releases/` (for releases) and `https://nexus.e-bs.cz/repository/maven-snapshots/` (for snapshots), with a note that internal e-BS projects already have these configured.
- **Resolver block — GitHub Packages:** Mill and sbt snippets pointing at `https://maven.pkg.github.com/iterative-works/support`, with a note that consumers need a GitHub PAT (`read:packages` scope) configured in their coursier credentials (`~/.config/coursier/credentials.properties`) or sbt credentials.

`publish.sh` is deleted with a single `git rm`. Mention the deletion in the `PUBLISHING.md` rewrite ("Local publish during development uses `./mill __.publishLocal` directly; the previous `publish.sh` wrapper has been removed because GitHub Actions is now the sanctioned release path").

### Stage D — release execution sequence

Execute these steps in order, AFTER the Phase 2 PR merges to `main` and AFTER `EBS_NEXUS_USERNAME` / `EBS_NEXUS_PASSWORD` secrets are confirmed to be present.

1. **Sanity check on merged `main`:**
   - `git checkout main && git pull`.
   - `./mill resolve __.publishArtifacts` — confirm the publishable set excludes `scenarios.{jvm,js}`, `scenariosUI`, `filesUIScenarios.{jvm,js}`, `formsScenarios.{jvm,js}`. (This was already verified at end of Phase 1 implementation; this is a "did anything drift since merge?" check.)
2. **Dry run:**
   - `./mill __.publishLocal` — confirm POMs land at `~/.ivy2/local/works.iterative.support/iw-support-*/0.1.14-SNAPSHOT/`. Spot-check `iw-support-core_3.pom` for organization, version, license, VCS, developer fields.
3. **Pre-tag CI sanity (optional but recommended):**
   - With docs already merged, `main` HEAD has triggered the snapshot path of `publish.yml`. Inspect the most recent `publish.yml` run. Confirm: `./mill __.test` passed, the GitHub Packages publish step succeeded, the e-BS Nexus step was skipped (correct — snapshots are GH-Packages-only). Confirm `0.1.14-SNAPSHOT` artifacts appear at `https://github.com/iterative-works/support/packages`.
4. **Version bump:**
   - Edit `build.mill:40`: `val publishVersion = "0.1.14-SNAPSHOT"` → `val publishVersion = "0.1.14"`.
   - Commit on `main` with a small, focused message (e.g., "release: bump version to 0.1.14").
   - Push to `main`. (This push triggers a snapshot run for `0.1.14` — but `0.1.14` is a release version, so the snapshot URI logic will not apply. The `__.test` gate runs, GitHub Packages step runs and uploads `0.1.14` to GH Packages release URI. This is harmless — the same coordinates will be re-published shortly by the tag run, and both registries accept overwrites.)
5. **Tag and push:**
   - `git tag v0.1.14`
   - `git push --tags`
6. **Workflow observation:**
   - Watch the `publish.yml` run triggered by the tag. Confirm:
     - `./mill __.test` gate passes.
     - "Publish to GitHub Packages" step succeeds; artifacts appear at `https://github.com/iterative-works/support/packages` for `0.1.14`.
     - "Publish to e-BS Nexus" step runs (it's gated on `github.ref_type == 'tag'`) and succeeds; artifacts appear at `https://nexus.e-bs.cz/repository/maven-releases/works/iterative/support/iw-support-core_3/0.1.14/` (and similar paths for the other published artifacts).
     - Both steps green, workflow overall green.
7. **Consumer-side resolution:**
   - From an existing e-BS project (no resolver-config change): resolve `iw-support-core::0.1.14` from Nexus. Confirm classpath wiring works.
   - From a clean repo with a GitHub PAT (`read:packages` scope) in coursier credentials: resolve `iw-support-core::0.1.14` from GitHub Packages.
   - Repeat both checks for the representative artifact set: `iw-support-tapir`, `iw-support-mongo`, `iw-support-sqldb`, `iw-support-server-http`, `iw-support-all`.

If any step in 6 or 7 fails, follow the partial-failure recovery procedure documented in the new `PUBLISHING.md` (re-run the workflow after fixing the auth/permission issue; both registries accept overwrites of the same coordinates).

## Files to Modify

- `PUBLISHING.md` — full rewrite. Current file (82 lines) describes the Nexus-only flow centered on `publish.sh`; replace entirely with content for the dual-publish flow.
- `README.md` — refresh. Current file (4 lines) covers only UI scenarios development and references obsolete `sbtn`; add a "Using iw-support" section ahead of the existing section and correct the `sbtn` reference.
- `publish.sh` — delete (`git rm publish.sh`). Currently 65 lines, Nexus-shell-script-specific, obsoleted by Phase 1's `publish.yml`.
- `build.mill` — Stage D, post-docs-merge ONLY. `CommonVersion.publishVersion` at line 40: bump `"0.1.14-SNAPSHOT"` → `"0.1.14"`. This is a separate small commit on `main` that immediately precedes the `v0.1.14` tag push. NOT part of the Phase 2 documentation PR.

## Component Specifications

### `PUBLISHING.md` (full rewrite)

Required sections, in order:

1. **Overview** — one paragraph: this repo dual-publishes to e-BS Nexus and GitHub Packages via GitHub Actions; `IWPublishModule` (from `mill-iw-support`) is the publish trait, configured via env vars. No GPG signing.
2. **Release process (sanctioned path)** — describes the tag-driven release: bump `CommonVersion.publishVersion` to a non-SNAPSHOT version on `main`, push, then `git tag vX.Y.Z && git push --tags`. The `publish.yml` workflow runs `./mill __.test` and then publishes to BOTH destinations sequentially. Mention fail-fast behavior.
3. **Snapshot publishing** — describes the asymmetric snapshot behavior per analysis decision: every push to `main` runs the test gate and publishes to GitHub Packages only. e-BS Nexus does not receive snapshots — internal consumers see only tagged releases. Reasoning is in the analysis; the docs simply state the behavior.
4. **Local publishing for development** — `./mill __.publishLocal` lands artifacts in `~/.ivy2/local`. No credentials needed. This is the contributor path; no `publish.sh` wrapper exists (deleted in this commit).
5. **Required GitHub Actions secrets** — `EBS_NEXUS_USERNAME` and `EBS_NEXUS_PASSWORD` (manually configured by a maintainer with repo admin rights). `GITHUB_TOKEN` is auto-provided by Actions. The job-level `permissions: { contents: read, packages: write }` declaration in `publish.yml` is what lets `GITHUB_TOKEN` write to GitHub Packages.
6. **Consumer setup** — pointer to `README.md` for resolver coordinates; this section can be brief.
7. **Partial-failure recovery** — if one publish step fails after the other succeeded (e.g., Nexus auth glitch after GitHub Packages already accepted the artifacts), re-run the workflow from the GitHub Actions UI. Both registries accept overwrites of the same coordinates. Do NOT bump to `0.1.15` to fix a transient upload failure.
8. **Troubleshooting** — short list: auth failure → check secrets; module not enumerated → `./mill resolve __.publishArtifacts`; workflow not triggering → confirm tag matches `v*`.

### `README.md` (refresh)

Required structure:

1. **Title + one-line description** — e.g., `# iw-support` and a one-sentence summary.
2. **Using iw-support (new section).** Sub-sections:
   - **Mill** — `mvn"works.iterative.support::iw-support-core::0.1.14"` with one short note that `_3` cross-version suffixing is automatic via `::`.
   - **sbt** — `"works.iterative.support" %% "iw-support-core" % "0.1.14"`.
   - **Resolver: e-BS Nexus** — Mill and sbt resolver snippets pointing at the Nexus releases/snapshots URLs. Note for e-BS internal projects that this is already configured.
   - **Resolver: GitHub Packages** — Mill and sbt resolver snippets pointing at `https://maven.pkg.github.com/iterative-works/support`. Note that a GitHub PAT with `read:packages` scope must be in coursier credentials (`~/.config/coursier/credentials.properties`) or sbt `credentials`. Brief example of credential file shape.
   - **Available artifacts** — list of published artifact names (`iw-support-core`, `iw-support-tapir`, `iw-support-mongo`, `iw-support-sqldb`, `iw-support-server-http`, `iw-support-all`, etc. — confirm the full inventory from `./mill resolve __.publishArtifacts` output).
3. **UI scenarios development (existing section, corrected).** Replace `sbtn ~scenarios-ui/fastLinkJS` with the equivalent `./mill` command (likely `./mill -w scenariosUI.fastLinkJS` — confirm during implementation by checking what `scenariosUI` exposes). Keep the rest (`cd ui/scenarios/`, `yarn`, `yarn vite .`).

### `publish.sh` (deletion)

`git rm publish.sh`. No replacement file. The `PUBLISHING.md` rewrite explains where the contributor path moved (`./mill __.publishLocal` directly).

### `build.mill` line 40 (Stage D version bump, NOT in the docs PR)

- Before: `val publishVersion = "0.1.14-SNAPSHOT"`
- After: `val publishVersion = "0.1.14"`

Commit message format: e.g., `release: bump version to 0.1.14`. Single-line commit, no prose body needed.

## Testing Strategy

### Stage C — manual review

No automated tests for documentation. The review checklist:

- **`PUBLISHING.md` is followable end-to-end by someone unfamiliar with the repo.** Reviewer reads top-to-bottom and identifies any step that requires unstated context.
- **The contributor path (`./mill __.publishLocal`) actually works** as written. Reviewer runs the command from a clean checkout; POMs land in `~/.ivy2/local`.
- **The snapshot-asymmetric behavior is correctly described.** Reviewer cross-checks against `.github/workflows/publish.yml` (Phase 1 artifact): the Nexus step has `if: github.ref_type == 'tag'`; the GH Packages step does not. The docs match.
- **No "Nexus access required for plugin resolution" caveat appears.** Per Phase 1 implementation log, the `//| repositories:` directive removal verified successfully — this constraint no longer exists.
- **`README.md` snippets are accurate.** Reviewer checks the Mill `mvn"..."` form against current Mill 1.x syntax (`::` for cross-version, single-`:` for plain coordinates). Checks sbt snippet against standard `%%` cross-version form.
- **`README.md` resolver snippets actually resolve.** Reviewer (or the maintainer doing Stage D) tries each one in a scratch project against the live registry. This overlaps with Stage D's consumer-side checks.
- **Existing UI scenarios section was minimally edited.** Reviewer confirms only the `sbtn` → `./mill` fix and any related correction; the `cd ui/scenarios/` + `yarn` + `yarn vite .` lines remain.

### Stage D — live verification (consumer-side resolution checks)

Per analysis "Verification & Release Layer (Stage D)" → "Consumer verification". Run AFTER the `v0.1.14` tag push and after observing both publish steps go green.

- **e-BS path:** From an existing e-BS project (with its current resolver config — no changes), add a dependency on `iw-support-core::0.1.14` and resolve. Repeat for: `iw-support-tapir`, `iw-support-mongo`, `iw-support-sqldb`, `iw-support-server-http`, `iw-support-all`. Confirm classpath wiring works (a `./mill compile` against a small consumer module that exercises a class from each artifact, OR equivalent for whatever build tool the consumer uses).
- **GitHub Packages path:** From a clean scratch repo with a GH PAT (`read:packages` scope) in `~/.config/coursier/credentials.properties`, add a resolver pointing at `https://maven.pkg.github.com/iterative-works/support` and a dependency on `iw-support-core::0.1.14`. Resolve. Repeat for the same five representative artifacts.

If any artifact fails to resolve from either destination, fix the root cause (likely a POM error, a permissions misconfiguration, or a missed publication of a transitive dependency) before declaring Phase 2 complete. Do NOT bump to `0.1.15` to paper over a resolution failure.

## Acceptance Criteria

Documentation PR (Stage C):

- [ ] `PUBLISHING.md` reflects the dual-publish flow: GitHub Actions as the sanctioned release path, the tag-driven release process, the snapshot-asymmetric behavior (snapshots → GH Packages only; tags → both registries), the two `EBS_NEXUS_*` secrets and auto-provided `GITHUB_TOKEN`, the contributor `./mill __.publishLocal` path with no credentials needed, partial-failure recovery via workflow re-run.
- [ ] `PUBLISHING.md` does NOT include any "Nexus access required for plugin resolution" caveat — the `//| repositories:` directive removal verified successfully in Phase 1.
- [ ] `README.md` documents BOTH consumer paths: Mill `mvn"works.iterative.support::iw-support-core::0.1.14"` literal AND sbt `"works.iterative.support" %% "iw-support-core" % "0.1.14"` snippet, with a per-destination resolver block (e-BS Nexus on one side, GitHub Packages with PAT on the other) presented as a distinct concern from the dependency snippet — per analysis "Decision: README uses `mvn"..."` Mill literal + sbt snippet + per-destination resolver block".
- [ ] The existing UI scenarios development section in `README.md` is corrected: `sbtn` reference is replaced with the equivalent `./mill` command. Other content in that section is preserved.
- [ ] `publish.sh` is deleted from the repo root.

Stage D (post-merge maintainer execution):

- [ ] `./mill resolve __.publishArtifacts` on merged `main` enumerates the publishable set correctly (sanity check after merge; should match Phase 1 verification).
- [ ] `./mill __.publishLocal` on merged `main` succeeds; `0.1.14-SNAPSHOT` POMs land at `~/.ivy2/local/works.iterative.support/iw-support-*/0.1.14-SNAPSHOT/`.
- [ ] `CommonVersion.publishVersion` at `build.mill:40` is bumped to `"0.1.14"` and committed to `main`.
- [ ] `v0.1.14` tag is pushed.
- [ ] Pushing the `v0.1.14` tag triggers `publish.yml` and uploads to BOTH e-BS Nexus AND GitHub Packages — per analysis acceptance criteria.
- [ ] Artifacts at version `0.1.14` are resolvable from BOTH destinations — per analysis acceptance criteria. Verified against representative set: `iw-support-tapir`, `iw-support-mongo`, `iw-support-sqldb`, `iw-support-server-http`, `iw-support-all` (plus `iw-support-core`).

## Risks for This Phase

- **Documentation drift from the Phase 1 workflow file.** Likelihood low, impact medium. If `PUBLISHING.md` describes behavior that doesn't match `.github/workflows/publish.yml` (e.g., asserts both destinations get snapshots when actually only GH Packages does), consumers and future maintainers are misled. Mitigated by the manual review checklist explicitly cross-checking docs against the workflow file.
- **`README.md` snippet form drift.** Likelihood low, impact low. Mill 1.x `mvn"..."` literal syntax is settled; sbt `%%` syntax is decades-stable. The risk is a typo in the `works.iterative.support` org name or `iw-support-core` artifact name. Mitigated by Stage D's consumer-side checks, which exercise the snippets live.
- **Stage D version bump pushed to `main` while `EBS_NEXUS_*` secrets are absent.** Likelihood low (PR description should call this out), impact medium (the tag run would fail at the Nexus step; the maintainer would need to add secrets and re-run). Mitigated by the explicit prerequisite callout in this context document, the Phase 2 PR description, and the suggested pre-tag CI sanity check (step 3 of the Stage D sequence) that observes the most recent snapshot run before tagging.
- **First-run auth/permission failure on the e-BS Nexus tag publish.** Likelihood low to medium, impact low (recoverable by re-running the workflow after fixing credentials; both registries accept overwrites). Mitigated by the partial-failure recovery procedure documented in the new `PUBLISHING.md` and by the fail-fast workflow structure that surfaces auth errors loudly. See analysis "Risk 1" and "Risk 5".
- **Resolution failure for one specific artifact across the inventory.** Likelihood low (POMs are generated identically by `IWPublishModule` for all modules), impact medium (a missing artifact would block consumers needing it). Mitigated by Stage D's per-artifact resolution checks against the representative set; if a single artifact fails resolution on one destination, fix-forward to `0.1.15` after diagnosing.
- **Snapshot path landed a broken artifact between docs PR merge and tag push.** Likelihood low (Phase 1 already verified the snapshot path runs `./mill __.test` as a gate). Impact low (snapshots are mutable on both registries; recoverable by fixing-forward on `main` and re-tagging). Not specific to this phase.

## Notes

- **The PR contains Stage C only.** The PR description should explicitly delineate "this PR" from "after-merge runbook (Stage D)" so reviewers don't expect the version bump or tag in the diff.
- **PR description must call out Stage D prerequisites:** `EBS_NEXUS_USERNAME` / `EBS_NEXUS_PASSWORD` secrets must be configured before the tag is pushed (snapshot path on `main` works without them — only the tag-trigger Nexus step fails). The version bump to `0.1.14` and the tag push are post-merge actions executed by the maintainer in sequence.
- **Per analysis "Decision: README uses `mvn"..."` Mill literal + sbt snippet + per-destination resolver block":** the dependency snippet and the resolver-config block are distinct concerns. Don't conflate them. A consumer reading the README needs to see "what to ask for" and "where to ask for it" as two separate questions with two separate answers.
- **Per analysis "Decision: Snapshot path runs `__.test` and publishes to GitHub Packages only":** the snapshot publish behavior is asymmetric. `PUBLISHING.md` MUST describe this accurately — e-BS Nexus does NOT receive snapshots; tagged releases go to BOTH registries.
- **Per Phase 1 implementation log "For next phase":** the `//| repositories:` directive removal verified successfully on a clean cache. `mill-iw-support::0.1.4` resolves from the public Mill plugin chain. `PUBLISHING.md` MUST NOT include any "Nexus access required for plugin resolution" or equivalent caveat — that constraint no longer exists.
- **Stage D step ordering is load-bearing.** Run the post-merge sanity check (`./mill resolve __.publishArtifacts`, `./mill __.publishLocal`) BEFORE the version bump. Then bump and commit the version. Then push the tag. This sequencing means the version-bump commit on `main` triggers a snapshot path run as a "free" pre-tag CI sanity check (step 3 in the Stage D sequence above).
- **Coursier credentials format.** When documenting GitHub Packages PAT setup in `README.md`, use the standard `host=maven.pkg.github.com\nusername=<github-user>\npassword=<pat>` format that coursier expects. Confirm syntax against current coursier docs during implementation if uncertain.
- **`mill-iw-support` source location.** If implementation hits any uncertainty about `IWPublishModule` env vars or behavior while writing `PUBLISHING.md`, confirm against the plugin source at `~/Devel/iw/support-libs/iw-project-support` rather than guessing.
- **The UI scenarios section's `sbtn` correction.** Confirm the actual Mill task name during implementation (likely `./mill -w scenariosUI.fastLinkJS` for the watch-mode equivalent). The Phase 1 trait restructure put `scenariosUI` on `BaseScalaJSModuleNoPublish`; check `build.mill` for the current module path.
