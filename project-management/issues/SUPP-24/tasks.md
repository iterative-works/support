# Implementation Tasks: Dual-publish to e-BS Nexus + GitHub Packages

**Issue:** SUPP-24
**Created:** 2026-04-29
**Status:** 1/2 phases complete (50%)

## Phase Index

- [x] Phase 1: Build refactor + dual-publish workflow (Est: 5–9h) → `phase-01-context.md`
- [ ] Phase 2: Documentation + release execution (Est: 2–5h) → `phase-02-context.md`

## Progress Tracker

**Completed:** 1/2 phases
**Estimated Total:** 7–14 hours
**Time Spent:** ~6 hours

## Notes

- Phase context files are generated just-in-time during implementation. Use `wf-implement` to start the next phase automatically.
- **Phase 1** merges Stage A (build.mill trait split with `BaseModuleCommon` parent, four internal modules onto `BaseModuleNoPublish`, version bump to `0.1.14-SNAPSHOT`, drop `//|` Nexus directive with verification) and Stage B (`.github/workflows/publish.yml` with `__.test` gate, dual-publish on tag, GH-Packages-only snapshot on `main`, two `EBS_NEXUS_*` secrets + auto `GITHUB_TOKEN`).
- **Phase 2** merges Stage C (rewrite `PUBLISHING.md` for dual-publish, refresh `README.md` with both consumer paths and `mvn"..."` + sbt snippets, delete `publish.sh`) and Stage D (post-merge: `./mill resolve __.publishArtifacts` sanity check, `./mill __.publishLocal` dry run, version bump to `0.1.14`, tag, push, verify on both registries).
- **Phase-size note:** Phase 2's low-end estimate (2h) is below the standard 3h floor. The analysis justifies retaining it as a distinct phase because Stage D is qualitatively post-merge release execution (tag/push/observe), not in-PR implementation — bundling them would force the implementation PR to stay open during release verification.
- **Issue body caveat:** The GitHub issue body still describes the original "Sonatype Central, drop Nexus" plan. `analysis.md` and `tasks.md` reflect the agreed-upon redirect to dual-publish (e-BS Nexus + GitHub Packages, no GPG, `IWPublishModule` retained). Update the issue body or surface the redirect in the PR description before implementation.
- Estimates are rough and will be refined during implementation.
- Phases follow layer dependency order: build configuration → CI workflow → documentation → release execution.
