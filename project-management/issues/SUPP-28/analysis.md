# Technical Analysis: Pin testcontainers Docker API for PostgreSQL/MySQL testing modules

**Issue:** SUPP-28
**Created:** 2026-05-14
**Status:** Superseded — see Resolution below

## Resolution (2026-05-15)

Fixed in commit `ec69743f` by bumping `org.testcontainers:{testcontainers,postgresql,mysql}` from `1.20.4` to `1.21.4` in `build.mill:404`, `:437`, `:471`.

testcontainers-java `1.21.4` (released 2025-12-15) is an upstream backport described as "makes version 1.21.x works with recent Docker Engine changes" — it removes the hard-coded `VERSION_1_32` fallback that caused silent test-skips on Docker 25+. The fix ships transparently to every downstream consumer of `iw-support-sqldb-postgresql-testing` / `iw-support-sqldb-mysql-testing` via the regular transitive dependency.

Verified locally against Docker engine 29.1.3 (API 1.52, min 1.44):
- `sqldb-postgresql.test` → 50 tests passed across 7 specs, 0 ignored
- `sqldb-mysql.test` → 10 tests passed, 0 ignored
- Wire traffic confirmed `User-Agent: tc-java/1.21.4` and requests against `/v1.44/containers/...`

The originally proposed workaround (Mill `TestModule` mixin with `-Dapi.version=1.46` in `forkArgs`) is no longer needed and was never implemented. The architectural analysis below is preserved as historical record of the investigation that led to checking for an upstream fix.

Upstream context:
- [testcontainers/testcontainers-java#11212 — Docker 29 incompatibility](https://github.com/testcontainers/testcontainers-java/issues/11212)
- [testcontainers/testcontainers-java#11360 — fallback fix](https://github.com/testcontainers/testcontainers-java/issues/11360)
- [testcontainers-java 1.21.4 release](https://github.com/testcontainers/testcontainers-java/releases/tag/1.21.4)

---

## Problem Statement

`org.testcontainers:testcontainers:1.20.x` (transitively pulled into both
`iw-support-sqldb-postgresql-testing` and `iw-support-sqldb-mysql-testing` via
`com.dimafeng::testcontainers-scala-*::0.41.5`) hard-codes a fallback to Docker
API version **1.32** in `DockerClientProviderStrategy.getClientForConfig`. On
Docker engine 25+ (current floor 1.44, verified on engine 29.1.3), the daemon
rejects 1.32 with:

> `client version 1.32 is too old. Minimum supported API version is 1.44, please upgrade your client to a newer version`

The failure mode is the *silent-skip* one that the issue calls out: testcontainer
startup throws inside the `whenDockerAvailable` guard, scalatest interprets the
container as "not available", and the surrounding spec degrades to **skipped**
with no test failure. Every CI / dev run that uses Docker 25+ produces a
false-green build for any spec gated on the container.

This affects both downstream consumers of these published modules and
**`iw-support`'s own test modules** today:

- `sqldb-postgresql.test` at `build.mill:442-444` — depends on `sqldb-postgresql.testing` and runs `PostgreSQLPermissionRepositorySpec`, `MessageCatalogueRepositorySpec`, `MessageCatalogueAuditTriggerSpec`, `RealMessageMigrationSpec`, `DatabasePermissionServiceE2ESpec`, `SqlMessageCatalogueE2ESpec`, `MessageCatalogueMigrationSpec`, `MessageCatalogueMigrationCLISpec`, `MessageCatalogueRowSpec`, `MessageCataloguePerformanceSpec`. Multiple of these almost certainly hit `whenDockerAvailable` paths and are being silently skipped on any Docker-29 developer machine right now.
- `sqldb-mysql.test` at `build.mill:476-478` — same pattern, smaller surface.
- Downstream consumers (e.g. `remote-signing` ACA-507 Phase 3, `remote-signing`'s `PostgresCredentialRepositorySpec` in Phase 1) carry a per-itest-module workaround:

  ```scala
  override def forkArgs = super.forkArgs() ++ Seq("-Dapi.version=1.46")
  ```

The mechanism that makes that workaround work is documented in the issue and
verified in bytecode: testcontainers' shaded `DefaultDockerClientConfig.createDefaultConfigBuilder()`
calls `overrideDockerPropertiesWithSystemProperties`, which walks `CONFIG_KEYS`
and reads `api.version` from JVM system properties before the
`VERSION_1_32` fallback fires. Setting `-Dapi.version=1.46` at fork time lets
testcontainers negotiate the right API at handshake time.

The issue also notes — and bytecode inspection confirms — that bumping
testcontainers to 1.21.3 does **not** fix this: the `VERSION_1_32` constant
fallback is present in 1.20.4 and 1.21.3 alike. The fix has to come from the
caller side, not from a transitive version bump.

## Proposed Solution

### High-Level Approach

Provide a build-system test trait that sets `-Dapi.version=1.46` in `forkArgs`
for every test module that touches PostgreSQL or MySQL testcontainers, so the
api-version override is inherited rather than copy-pasted into every consumer
build. The mechanism (system property → `DefaultDockerClientConfig` →
`overrideDockerPropertiesWithSystemProperties`) is the same as the documented
ad-hoc workaround; the change is *where* the trait lives so that consumers
inherit it.

The central architectural question is **where the trait is defined**, because
the issue's literal phrasing ("provide a `TestModule` mixin trait *in*
`sqldb-postgresql-testing`") conflates a published Maven artifact with a Mill
build-system trait — those live in two different worlds. The published
`iw-support-sqldb-postgresql-testing` jar contains compiled Scala (the
`PostgreSQLTestingLayers` object); a downstream Mill `build.mill` cannot pick
up a `TestModule` mixin from a jar dependency. The trait must live in a
**build-script-visible** location:

- the `mill-iw-support` plugin (downstream `build.mill` files already consume
  it for `IWScalaModule`, `IWPublishModule`, `IWBomModule`), or
- in this repo's `build.mill` next to `BaseTests`, or
- both, or
- only as documentation.

See **CLARIFY 1** for the option set. Until that resolves, this analysis
documents the structure assuming the trait lives somewhere build-script-visible
and applies symmetrically to PostgreSQL and MySQL.

### Why This Approach

The `-Dapi.version=1.46` system-property override is the only intervention that
fixes the root cause without forking testcontainers:

- **Bumping testcontainers** does nothing — the `VERSION_1_32` fallback is
  present in 1.20.4 and 1.21.3 (issue documents bytecode-level verification).
- **Pinning the Docker engine version in CI** would mask the failure on the
  CI machine but leaves every developer machine running modern Docker (the
  industry norm) silently skipping the same tests. The bug is the dev-loop
  bug, not just a CI bug.
- **Environment variable override** (`DOCKER_API_VERSION`) does not match
  testcontainers' config-key walk — `overrideDockerPropertiesWithSystemProperties`
  reads JVM system properties, not env vars. Setting `DOCKER_API_VERSION=1.46`
  in the shell does not affect the shaded docker-java config builder along the
  path testcontainers actually uses.
- **A `TestModule` mixin** is the right abstraction because the override needs
  to apply at JVM-fork time (per test module) and Mill's `forkArgs` is the
  intended hook for that.

The version `1.46` is chosen per the issue's reasoning: it is a safe ceiling
inside `[1.44, current]` that Docker 25+ supports, with headroom for future
engine bumps. Anything below 1.44 reproduces the failure; anything above the
running engine's max negotiates down at handshake time.

## Architecture Design

**Purpose:** Describe WHAT each part of the build-system change needs, not HOW
each Scala line is written. Build-system / plugin work doesn't map onto
domain/application/infrastructure/presentation layers; the layering below
matches the actual structure of the change.

### Build-System Trait Layer

**Components:**

- A `TestModule` mixin trait that sets `forkArgs ++ Seq("-Dapi.version=1.46")`. Exact location depends on **CLARIFY 1** (Option A: `mill-iw-support` plugin; Option B: inline next to `BaseTests` in this repo's `build.mill`; Option C: both; Option D: documentation only — no trait).
- Naming: per-database (`PostgreSQLTestcontainersTests`, `MySQLTestcontainersTests`) or single generic (`TestcontainersTests`). See **CLARIFY 2**.
- Optional overridable cap: `def testcontainersApiVersion: String = "1.46"` as a task, so consumers can bump the ceiling without redefining the trait. See **CLARIFY 4**.
- If Option A or C: trait source path under `mill-iw-support/src/...` in the separate `iw-project-support` repo (path `/home/mph/Devel/iw/support-libs/iw-project-support/mill-iw-support/`), plus a published version bump of `mill-iw-support` (currently `0.1.4`) and a corresponding bump of the `//| mvnDeps:` directive at the top of this repo's `build.mill`.
- If Option B or C: trait declared inline near `BaseTests` (`build.mill:102-109`), in the `BaseModuleCommon` context or alongside it.

**Responsibilities:**

- Inject `-Dapi.version=1.46` into the test JVM's `forkArgs` for any test module that mixes in the trait.
- Compose cleanly with `BaseTests` (which is `ScalaTests with TestModule.ZioTest`) — the mixin is additive, override `forkArgs` to extend, not replace, `super.forkArgs()`.
- Provide a stable name and stable trait shape so downstream `build.mill` files can rely on it across `mill-iw-support` releases.

**Dependencies:** None — this is a leaf trait. It does not depend on any other layer in this work.

**Acceptance criteria mapping:**

- "Mixin trait exists" — issue's "Proposed fix" section maps directly to this layer.
- "Consumers inherit the API override automatically" — depends on **CLARIFY 1** resolution; only Options A and C achieve this for downstream Mill consumers.

**Estimated Effort:** 0.5–2 hours

**Complexity:** Straightforward, modulo **CLARIFY 1**. The Scala/Mill change itself is ~3 lines. Cross-repo coordination (Option A or C) adds plugin release ceremony: bump `mill-iw-support` version, publish, bump the `//|` directive here, verify resolution. Option B is purely in-repo and is the smallest possible change.

---

### Build Integration Layer

**Components:**

- `build.mill:442-444` (`sqldb-postgresql.test`): apply the trait (Option A/B/C) OR add `override def forkArgs = super.forkArgs() ++ Seq("-Dapi.version=1.46")` inline (Option D).
- `build.mill:476-478` (`sqldb-mysql.test`): same as above.
- No changes to the published artifacts (`sqldb-postgresql.testing` at `build.mill:426-440`, `sqldb-mysql.testing` at `build.mill:460-474`) — these are compiled Scala libraries; they carry no Mill semantics.
- See **CLARIFY 3** for whether this layer is in scope at all (issue text focuses on downstream consumers, but the same failure mode applies to iw-support's own test modules).

**Responsibilities:**

- Make `./mill sqldb-postgresql.test.test` actually run the PostgreSQL container-backed specs on Docker 25+ instead of silently skipping them.
- Make `./mill sqldb-mysql.test.test` do the same for MySQL.
- Keep the existing `BaseTests` inheritance intact — the mixin is *additive* to `BaseTests`.

**Dependencies:**

- Depends on the Build-System Trait Layer if the trait route is chosen. Independent if Option D (documentation only) — in that case this layer adds inline `forkArgs` to the two modules and the Build-System Trait Layer is empty.

**Acceptance criteria mapping:**

- "Stops `whenDockerAvailable` silent-skip in iw-support's own test suites" — this layer (conditional on **CLARIFY 3**).

**Estimated Effort:** 0.25–0.5 hours

**Complexity:** Straightforward. Two near-identical edits at `build.mill:442-444` and `build.mill:476-478`.

---

### Documentation Layer

**Components:**

- Scaladoc on the new trait (wherever it lives): explain why the override exists, link to the testcontainers `DockerClientProviderStrategy.VERSION_1_32` fallback as the root cause, and document the `1.46` choice (safe ceiling in `[1.44, current]`).
- A note in `sqldb-postgresql/testing-support/src/main/scala/works/iterative/sqldb/postgresql/testing/PostgreSQLTestingLayers.scala` (or a sibling README) describing the dev-side requirement: testcontainer-using consumers must set `-Dapi.version=1.46` in their test JVM's `forkArgs` on Docker 25+, with the recommended pattern being the `mill-iw-support` trait (if Option A/C) or the inline workaround (if Option B/D).
- Mirror note in `sqldb-mysql/testing-support/src/main/scala/works/iterative/sqldb/mysql/testing/MySQLTestingLayers.scala`.
- Update `docs/HTTP_SERVER_GUIDE.md` if it cross-references the testing-support modules (verify; do not add a note if it doesn't). Project memory does not list a dedicated SQL testing guide.
- Optional: file an upstream testcontainers issue per the original ticket's "Worth flagging upstream too if not already." See **CLARIFY 5**.

**Responsibilities:**

- Make the override discoverable from the published modules' own source/docs — so a consumer who has not yet adopted the `mill-iw-support` trait still knows what to set and why.
- Document the Docker-version range the cap covers (`>= 1.44` since Docker 25; ceiling `1.46` chosen for headroom; safe up to current).

**Dependencies:**

- Depends on the Build-System Trait Layer (to know the trait's final name and home, for accurate cross-references). Independent for the bare-`forkArgs` workaround text.

**Acceptance criteria mapping:**

- Issue's "Notes" section (`1.46` rationale, testcontainers bytecode caveat, upstream flag) maps to this layer.
- Issue's "Workaround in consumer projects" section maps here too — the documented inline pattern is what consumers fall back to when not on a `mill-iw-support` version carrying the trait.

**Estimated Effort:** 0.5–1 hour

**Complexity:** Straightforward.

---

### Verification Layer

**Components:**

- Manual run of `./mill sqldb-postgresql.test.test` on a Docker 29+ host, comparing pre/post-change behavior. Pre-change: silent skip of `whenDockerAvailable` blocks (visible as scalatest "skipped" / "pending" counts inflated, zero container logs in test output). Post-change: containers start, specs run, test output shows real assertion results.
- Manual run of `./mill sqldb-mysql.test.test` on the same host.
- Manual verification that test output is **pristine** (per project conventions) — the testcontainer start log must be clean; the "client version 1.32 is too old" error must NOT appear.
- Regression check: run the same suites on Docker 24 (or any engine whose floor is below 1.44) to confirm the `1.46` cap negotiates down without breaking. If a Docker 24 environment is not readily available, document this as a deferred verification.
- Optional: bytecode confirmation that the `forkArgs` value actually reaches the JVM (e.g., `./mill show sqldb-postgresql.test.forkArgs` and verify `-Dapi.version=1.46` is present in the resolved list).

**Responsibilities:**

- Prove the fix changes behavior end-to-end on a modern Docker host.
- Catch any silent-skip regression by checking the test output is pristine, not just that the build is green.
- Confirm the cap value (`1.46`) does not break the engine-version range we care about.

**Dependencies:**

- Depends on the Build Integration Layer (the trait or `forkArgs` must actually be applied to the test modules before verification is meaningful).

**Acceptance criteria mapping:**

- "Reproduce silent-skip pre-change; observe real spec runs post-change" — this layer.
- "Full PostgreSQL/MySQL test suites pass on Docker 29" — this layer.

**Estimated Effort:** 0.5–1 hour

**Complexity:** Straightforward, with the caveat that there is no automated way to assert "the spec wasn't silently skipped" beyond inspecting test output. The verification is necessarily manual / observational.

---

## Technical Decisions

### Patterns

- `TestModule` mixin trait composed with `BaseTests` at the test-module declaration site. Standard Mill pattern (matches how `BaseTests` itself is structured at `build.mill:102-109`).
- `super.forkArgs() ++ Seq(...)` to *extend* rather than override forked-JVM args. Composition discipline; lets multiple mixins coexist.
- System-property override at JVM-fork time, not at runtime. Matches testcontainers' `overrideDockerPropertiesWithSystemProperties` config-key walk.

### Technology Choices

- **Build tool:** Mill 1.1.2 (existing).
- **Test framework:** ZIO Test via `TestModule.ZioTest` (existing, inherited via `BaseTests`).
- **No new dependencies:** the change is purely Mill build-script wiring + (optionally) `mill-iw-support` plugin code. No new Maven dependencies on the testing-support modules.
- **API version cap:** `1.46` — safe within Docker 25+'s `>= 1.44` floor, with headroom; documented per issue.

### Integration Points

- `sqldb-postgresql.test` and `sqldb-mysql.test` (in `build.mill`) ← consume the trait or inline `forkArgs`.
- `mill-iw-support` plugin (in the separate `iw-project-support` repo) ← *optionally* host the trait (Option A or C in **CLARIFY 1**), in which case `build.mill:1-5`-ish `//| mvnDeps:` directive must bump the plugin version.
- Published `sqldb-postgresql-testing` / `sqldb-mysql-testing` jars ← **no Mill semantics**; they cannot carry the mixin. They can carry a documentation note pointing consumers to the trait or the inline pattern.

### Alternatives Considered

- **Bump testcontainers to 1.21.3 (or higher):** Rejected. Issue documents bytecode-level verification that the `VERSION_1_32` fallback constant is unchanged in both 1.20.4 and 1.21.3. A version bump alone does not fix the bug.
- **Pin Docker engine to 24.x in CI:** Rejected. Masks the failure on CI but leaves every developer machine on modern Docker silently skipping the same tests. The bug is the dev-loop bug.
- **Env-var override (`DOCKER_API_VERSION=1.46`):** Rejected. Does not match the code path testcontainers actually walks (`overrideDockerPropertiesWithSystemProperties` reads JVM system properties, not env vars).
- **One generic `TestcontainersTests` trait vs. per-database traits:** See **CLARIFY 2**. The `-Dapi.version=1.46` override is database-agnostic, so a single generic trait would express the actual coupling. Per-database naming would match the issue's literal wording and leave room for future database-specific test wiring.
- **Documentation only (no trait):** See **CLARIFY 1** Option D. Smallest delivery but leaves every consumer to copy-paste the workaround, which is the status quo the issue is trying to fix.

## Technical Risks & Uncertainties

### CLARIFY 1: Trait location

The issue says "Provide a `TestModule` mixin trait in `sqldb-postgresql-testing`."
Mechanically that is not possible: `sqldb-postgresql-testing` is a published
Maven library (compiled Scala), but `TestModule` is a Mill build-system trait
that only has meaning inside `build.mill`. A downstream Mill build cannot pick
up a `TestModule` mixin from a jar dependency. Where the trait lives changes
the scope of the work significantly.

**Questions to answer:**
1. Should consumers inherit the override automatically through the
   `mill-iw-support` plugin, or is documenting the inline workaround enough?
2. Are we willing to coordinate a `mill-iw-support` plugin release (and bump the
   `//| mvnDeps:` directive in this repo's `build.mill`) as part of this issue?
3. If yes, does this issue also include applying the trait inside `iw-support`'s
   own test modules (i.e. is Option C — both — the target)?

**Options:**
- **Option A — Add the trait to `mill-iw-support` only.** Cleanest long-term:
  downstream Mill builds gain the trait by upgrading the plugin version. Requires
  a coordinated change across two repos (`iw-project-support` and `iw-support`),
  a plugin release, and a plugin-version bump in `build.mill`'s `//| mvnDeps:`
  directive. Does not fix iw-support's own test modules unless paired with
  Option B's edits — strictly speaking, until the plugin is bumped in this
  repo and the local test modules adopt the trait, nothing inside iw-support
  changes.
- **Option B — Inline trait in `build.mill` only.** Fastest, fully in-repo.
  Define `BaseTestcontainersTests` (or per-database equivalents) inline near
  `BaseTests` at `build.mill:102-109`. Apply to `sqldb-postgresql.test` and
  `sqldb-mysql.test`. Downstream consumers do NOT inherit it; they must
  copy-paste the `forkArgs` line or rely on documentation.
- **Option C — Both.** Trait lives in `mill-iw-support`; this repo bumps the
  plugin version and applies the trait locally. Best deliverable, biggest
  scope. Closes the loop for both iw-support and downstream consumers in one
  pass.
- **Option D — Documentation only.** Document the `-Dapi.version=1.46`
  workaround in the `sqldb-postgresql-testing` / `sqldb-mysql-testing` scaladoc
  and/or a README. Do not add any trait. Lowest cost, weakest delivery —
  consumers still copy-paste the workaround on every itest module, and
  iw-support's own test modules either keep silently skipping or get the
  inline `forkArgs` treatment as a separate concern.

**Impact:** Total estimate, the cross-repo coordination question, and what
"done" looks like. Option B is ~1h end-to-end; Option C plausibly ~3–4h once
plugin release ceremony is included.

---

### CLARIFY 2: Trait naming and granularity

Issue suggests two per-database traits (`PostgreSQLTestcontainersTests` and a
parallel for MySQL). The `-Dapi.version=1.46` override is database-agnostic, so
a single generic trait could cover both.

**Questions to answer:**
1. Do we anticipate adding database-specific test wiring (e.g. PostgreSQL
   container-port hints, Flyway migration roots) to these traits in the future
   that would justify separating them now?
2. Is the redundancy of two near-identical traits a clarity win (each
   testing-support module gets a "matching" trait) or noise?

**Options:**
- **Option A — `PostgreSQLTestcontainersTests` + `MySQLTestcontainersTests`.**
  Matches the issue's literal wording. Leaves room for future per-database
  wiring. Two near-identical declarations today.
- **Option B — Single generic `TestcontainersTests`.** Less duplication. Both
  databases (and any future testcontainer-using module) mix in the same trait.
  Honest about the actual coupling (api-version is not database-specific).
- **Option C — Single generic trait + thin per-database aliases.** Single
  source of truth, but `PostgreSQLTestcontainersTests = TestcontainersTests`
  (or trait inheriting it without additions) preserves the naming surface
  from the issue. Slightly more code for marginal cosmetic gain.

**Impact:** Naming and discoverability in downstream `build.mill` files.

---

### CLARIFY 3: Apply to iw-support's own test modules?

The issue text frames this as a downstream-consumer problem. iw-support's own
`sqldb-postgresql.test` and `sqldb-mysql.test` modules sit on top of the same
testing-support and almost certainly hit the same silent-skip on Docker 25+.

**Questions to answer:**
1. Has anyone confirmed on a Docker 29 dev machine whether `sqldb-postgresql.test`
   specs are currently being silently skipped? (Strong working assumption: yes,
   given the testcontainer client path is identical.)
2. Do we treat the fix as covering iw-support's own coverage gap as well, or
   leave that as a follow-up?
3. If we leave it for a follow-up, can we live with iw-support shipping a
   release that *advertises* a fix while its own equivalents stay broken?

**Options:**
- **Option A — Include iw-support's own test modules in scope.** Apply the
  trait or inline `forkArgs` to `sqldb-postgresql.test` (`build.mill:442-444`)
  and `sqldb-mysql.test` (`build.mill:476-478`) as part of this issue.
  Strongly recommended: the in-repo failure mode is the same one the issue is
  trying to fix; not fixing it here is shipping a partial solution.
- **Option B — Out of scope for this issue.** Track separately. Lets this
  issue stay narrow but ships an asymmetric outcome.

**Impact:** Whether the Build Integration Layer described above is empty
(Option B) or non-empty (Option A). The analysis assumes Option A unless
clarified otherwise.

---

### CLARIFY 4: Version-cap configurability

`1.46` is a defensible default but will age. Docker engine release cadence
means the safe ceiling will rise; the trait should not require redefinition
to keep up.

**Questions to answer:**
1. Do we want the cap to be overridable per test module (e.g. `override def
   testcontainersApiVersion = "1.47"`), or is hard-coded `1.46` good enough
   for the foreseeable future?
2. Is the overridable form worth its small added complexity (one `def` instead
   of one `Seq` literal)?

**Options:**
- **Option A — Hard-code `"1.46"` in the trait body.** Simplest. Any future
  bump requires a `mill-iw-support` release or a local `build.mill` edit.
- **Option B — Expose `def testcontainersApiVersion: String = "1.46"` and
  reference it from `forkArgs`.** Lets consumers bump the cap without
  redefining the trait. Marginal complexity cost; one additional def in the
  trait surface area.
- **Option C — Read from an env var with `1.46` fallback.** Lets CI control
  the cap independently of the build. Probably overkill for this scope.

**Impact:** Trait surface area and how future Docker upgrades land.

---

### CLARIFY 5: Upstream filing

The issue notes "Worth flagging upstream too if not already" — i.e. file a
testcontainers GitHub issue requesting the hard-coded `VERSION_1_32` fallback
be removed or raised to a sensible current floor.

**Questions to answer:**
1. Is upstream filing in scope for this issue or a separate concern (research
   existing upstream issue, draft a reproducer, file)?
2. If in scope, who is the maintainer doing the filing — does this need a
   sign-off?

**Options:**
- **Option A — In scope, this issue.** Search testcontainers GitHub for an
  existing issue on `VERSION_1_32`; if absent, file one with a minimal
  reproducer (engine 29 + testcontainers 1.20.4 → silent skip). Adds ~1h.
- **Option B — Out of scope; track as follow-up.** Most pragmatic.
- **Option C — Skip entirely.** We carry the workaround; upstream will fix
  it when they fix it.

**Impact:** Total estimate and whether the analysis grows by a follow-up
ticket. The estimate range below assumes Option B.

---

## Total Estimates

**Per-Layer Breakdown:**
- Build-System Trait Layer: 0.5–2 hours (range driven by **CLARIFY 1**)
- Build Integration Layer: 0.25–0.5 hours (assumes **CLARIFY 3** = Option A)
- Documentation Layer: 0.5–1 hour
- Verification Layer: 0.5–1 hour

**Total Range:** 1.75–4.5 hours

**Confidence:** Medium-High

**Reasoning:**
- The mechanical change is tiny (a one-line forkArgs override applied to two
  modules). The estimate range is dominated by **CLARIFY 1** — Option B (in-repo
  only) lands at the low end; Options A or C add cross-repo plugin-release
  ceremony to the high end.
- Verification is observational rather than asserted, so there's no test-writing
  cost; just a run of the suites on a Docker 29 host and a check for pristine
  output.
- Documentation is small and tightly bounded — scaladoc on the trait plus a
  scaladoc note in each `*TestingLayers.scala`.
- Risk of unanticipated coupling is low: this is purely additive build-script
  wiring with no runtime/library surface change.

## Recommended Phase Plan

Per the phase-size policy: total estimate is **1.75–4.5 hours**. Total-size
guidance for `< 4h` is **1 phase**; for `4–12h` it is 1–3 phases with merging
applied below the 3h floor. The high end of our range is `4.5h`, which sits
just over the `< 4h` boundary, but every individual layer is well below the
3h floor and the work is tightly coupled (the trait, its application, its
documentation, and its verification all want to land together for the fix to
be reviewable as a single artifact). **One phase.**

- **Phase 1: API-version mixin + apply + document + verify**
  - Includes: Build-System Trait Layer + Build Integration Layer + Documentation Layer + Verification Layer.
  - Estimate: 1.75–4.5 hours
  - Rationale: Every layer is below the 3h floor and they're tightly coupled — the trait alone is unreviewable without seeing it applied; the application is meaningless without the trait; the docs reference the trait's final name and home; verification proves the loop closes. One PR, one review. If **CLARIFY 1** resolves to Option A or C, the `mill-iw-support` plugin release happens as a prerequisite step inside this phase (publish plugin, bump `//| mvnDeps:` directive here, then apply the trait), still cleanly inside one delivery slice.

**Total phases:** 1 (for total estimate 1.75–4.5 hours)

## Testing Strategy

### Per-Layer Testing

There is no good way to *unit-test* a `forkArgs` system-property override. The
override is materialized at JVM fork time and consumed by shaded docker-java
code we don't control. Property-test or unit-test framing does not apply.

**Build-System Trait Layer:**
- `./mill show sqldb-postgresql.test.forkArgs` and `./mill show sqldb-mysql.test.forkArgs` — verify the resolved list contains `-Dapi.version=1.46`. This is the closest we get to an assertion on the trait's behavior.
- If Option A or C (trait in `mill-iw-support`): a smoke build in this repo (`./mill resolve __.compile`) confirms the plugin version is picked up cleanly and the trait is visible inside `build.mill`.

**Build Integration Layer:**
- `./mill sqldb-postgresql.test.test` on a Docker 29 host — pre-change establishes the silent-skip baseline (specs show as "skipped", no container logs); post-change shows real container start + spec assertions.
- `./mill sqldb-mysql.test.test` on the same host — same protocol.

**Documentation Layer:**
- Manual review only. Scaladoc compiles via `./mill __.docJar` (if exercised) — sanity check there are no broken links or malformed tags. No automated testing applies.

**Verification Layer:**
- End-to-end run of both suites on Docker 29 with the change in place. Capture the test output and confirm:
  - No `client version 1.32 is too old` error in any log line.
  - The `whenDockerAvailable` blocks execute their bodies (visible by container-startup logs and real spec assertions).
  - Pristine output otherwise (per project conventions — log noise is a failure).
- Optional regression run on a Docker 24 host, if one is available, to confirm `1.46` negotiates down without breaking the older engine. If no Docker 24 host is on hand, document this as deferred.

**Test Data Strategy:**
- Not applicable — no application data. The testing-support modules manage their own ephemeral container lifecycle.

**Regression Coverage:**
- Existing `BaseTests` semantics (`ScalaTests with TestModule.ZioTest`) must be preserved on every test module the trait touches. The mixin is additive (`super.forkArgs() ++ Seq(...)`) and does not replace any existing setting; regression risk is bounded to "did super.forkArgs() get called correctly?", verifiable via the `./mill show ... forkArgs` task.
- Tests that don't use Docker at all (any pure-unit specs inside `sqldb-postgresql.test` or `sqldb-mysql.test`) are unaffected — the system property is inert when no testcontainer is started.

## Deployment Considerations

### Database Changes
None.

### Configuration Changes
- If Option A or C in **CLARIFY 1**: bump the `//| mvnDeps:` directive in `build.mill` to the new `mill-iw-support` version that ships the trait. No env vars or feature flags.
- If Option B or D: no configuration change at all.

### Rollout Strategy
- The change is internal to the build system and ships in the next iw-support release. Downstream consumers either inherit it via the plugin (Option A/C) or copy the inline workaround (Option B/D) — both paths are documented per the Documentation Layer.
- No phased / canary rollout needed; the trait is purely additive and the override is inert on Docker engines that don't need it.

### Rollback Plan
- If the trait causes an unexpected `forkArgs` regression in any test module, remove the mixin from that module's declaration. The trait itself does not need to be deleted; just unapplied.
- If the `mill-iw-support` plugin bump (Option A/C) breaks plugin resolution, restore the previous `//| mvnDeps:` directive line in `build.mill` and fall back to inline `forkArgs`.
- Reverting is trivially safe — the system property override is the only behavior the trait carries; absent the trait, the build returns to the pre-change state (which is the current silent-skip state on Docker 25+).

## Dependencies

### Prerequisites
- Access to a Docker 25+ host (engine version `>= 25`) for the Verification Layer. Docker 29.1.3 is the host the issue reproduced on; any engine with a `>= 1.44` API floor reproduces.
- If Option A or C in **CLARIFY 1**: maintainer access to publish a `mill-iw-support` release from the `iw-project-support` repo.

### Layer Dependencies
- Build-System Trait Layer → Build Integration Layer (the trait must exist before it can be applied; **trivially skipped** if Option D).
- Build Integration Layer → Verification Layer (the modules must adopt the override before verification is meaningful).
- Documentation Layer can be drafted in parallel with the Build-System Trait Layer but should land in the same PR for accurate cross-references.

### External Blockers
- None. The fix is fully under our control. Upstream testcontainers behavior is the cause but not a blocker — the override mechanism is already there in the shaded config builder.

## Risks & Mitigations

### Risk 1: `mill-iw-support` plugin release ceremony underestimated (Option A/C only)
**Likelihood:** Low–Medium
**Impact:** Low
**Mitigation:** Time-box the cross-repo coordination; if the plugin release stalls, fall back to Option B (in-repo trait) which is unblocked at any time. Track the plugin upgrade as a follow-up.

### Risk 2: Future Docker engine raises the API floor above `1.46`
**Likelihood:** Low (Docker engine floor moves slowly; `1.44` has been the floor since Docker 25 and Docker 29 keeps it)
**Impact:** Low (re-triggers the same silent-skip failure mode, identically diagnosable)
**Mitigation:** Choose **CLARIFY 4** Option B (overridable def) so consumers can bump locally without waiting for a release. Document the cap-bump procedure in the trait scaladoc.

### Risk 3: Trait composition breaks an existing test module's `forkArgs`
**Likelihood:** Very Low
**Impact:** Low
**Mitigation:** The mixin uses `super.forkArgs() ++ Seq(...)`, preserving everything upstream of it. Verifiable via `./mill show <module>.forkArgs` before and after.

### Risk 4: Silent-skip baseline cannot be reproduced (Verification Layer cannot establish a pre-change behavior)
**Likelihood:** Low
**Impact:** Low (the post-change "containers start and specs run" observation is sufficient to confirm the fix, even without a clean pre-change baseline)
**Mitigation:** If the pre-change repro is hard to capture cleanly, lean on the post-change positive observation plus `./mill show ... forkArgs` confirming the system property is present.

### Risk 5: Documentation drifts as the trait's home moves (e.g. moves from `build.mill` to `mill-iw-support` later)
**Likelihood:** Low
**Impact:** Very Low (docs are easy to update)
**Mitigation:** Keep the documentation phrased in terms of "set `-Dapi.version=1.46` in forkArgs" — the *mechanism* — with a note on the current preferred way to do that. The mechanism note ages well even if the recommended trait location changes.

---

## Implementation Sequence

**Recommended Layer Order:**

1. **Build-System Trait Layer** — first, because every other layer references the trait's name and home. Pick **CLARIFY 1** option, then declare the trait. Skipped (empty layer) only under Option D.
2. **Build Integration Layer** — second, because applying the trait to `sqldb-postgresql.test` and `sqldb-mysql.test` is what makes any of the rest observable. Under Option D this layer adds inline `forkArgs` instead of mixing in a trait.
3. **Documentation Layer** — third, but parallelizable with layer 2 inside the same PR. Scaladoc on the trait and notes in the two `*TestingLayers.scala` files.
4. **Verification Layer** — fourth and final. Manual end-to-end run on Docker 29, plus the `./mill show ... forkArgs` check.

**Ordering Rationale:**
- The trait has to exist before it can be applied or documented; that pins layer 1 first.
- The application has to land before verification is meaningful; that pins layer 4 last.
- Documentation can be drafted in parallel with the trait definition but must reference the trait's final name and home, so it commits alongside layers 1–2.
- Total work is small enough that one phase (and one PR) covers all four layers comfortably.

## Documentation Requirements

- [ ] Scaladoc on the new trait (wherever it lives per **CLARIFY 1**) explaining the testcontainers `VERSION_1_32` fallback and the `1.46` cap choice.
- [ ] Scaladoc note in `sqldb-postgresql/testing-support/src/main/scala/works/iterative/sqldb/postgresql/testing/PostgreSQLTestingLayers.scala` describing the dev-side requirement (set `-Dapi.version=1.46` in test JVM forkArgs on Docker 25+).
- [ ] Mirror scaladoc note in `sqldb-mysql/testing-support/src/main/scala/works/iterative/sqldb/mysql/testing/MySQLTestingLayers.scala`.
- [ ] No architecture decision record needed (this is build/CI infrastructure, not a domain pattern shift).
- [ ] No user-facing application documentation needed.
- [ ] No migration guide needed (additive change; pre-change behavior was silently broken on modern Docker, post-change is correct).
- [ ] Optional: upstream testcontainers issue per **CLARIFY 5**.

---

**Analysis Status:** Ready for Review

**Next Steps:**
1. Resolve CLARIFY markers 1–5 with stakeholders. CLARIFY 1 (trait location) is the critical scope decision; others are smaller follow-ons.
2. Run **wf-create-tasks** with the issue ID.
3. Run **wf-implement** for layer-by-layer implementation.
