<!-- PURPOSE: Numbered, stable-ID cross-issue decisions for the release-and-ci effort -->
<!-- PURPOSE: Append-only; supersede rather than edit; each decision names where it propagates -->

# Release & CI — Decisions

Stable IDs: `RCI-Dn`. Append-only. Supersede rather than edit. Each decision
names every document it propagates to; an unchecked box is visible drift.

---

## RCI-D1 — Dual-publish to e-BS Nexus + GitHub Packages; no Sonatype Central

*Source: SUPP-24. Supersedes the original ticket framing ("publish to Sonatype
Maven Central, drop Nexus").*

Publish to **both** e-BS Nexus and GitHub Packages (Maven, under
`iterative-works/support`). e-BS workflows keep resolving from Nexus unchanged;
non-e-BS consumers resolve from GitHub Packages with a GH PAT. No Sonatype
Central, no GPG. Rationale: Central's ceremony (GPG, namespace verification,
irrevocable releases) pays off only for a public framework; this library is for
our own applications, and Central-proxy indirection would add lag and an
external dependency for e-BS consumers.

**Propagates to:**
- [x] `build.mill` (publish targets driven by env-var URIs)
- [x] `.github/workflows/publish.yml` (two publish steps, one per destination)
- [x] `PUBLISHING.md`
- [x] `README.md` (both sets of resolver coordinates)

---

## RCI-D2 — `IWPublishModule` is the publish primitive

*Source: SUPP-24.*

Keep `IWPublishModule` (from `mill-iw-support`) as the publish trait. Its
env-var-configurable publish URIs (`IW_PUBLISH_RELEASE_URI` /
`IW_PUBLISH_SNAPSHOT_URI`) are exactly the primitive that makes dual-publish
trivial — the same Mill task is invoked twice in CI with different env vars per
destination. GPG signing stays disabled.

**Propagates to:**
- [x] `build.mill`
- [x] `.github/workflows/publish.yml`

---

## RCI-D3 — Publishability is a type-level property

*Source: SUPP-24.*

Split the base trait: `BaseModuleCommon` holds shared Scala/compile/scalafix
config; `BaseModule` adds `IWPublishModule` (publishable); `BaseModuleNoPublish`
does not extend `PublishModule`. Internal-only modules use
`BaseModuleNoPublish`; the `publishVersion = "0.0.0"` markers are removed.
Whether a module publishes is visible in its type, not encoded in per-module
boilerplate.

**Propagates to:**
- [x] `build.mill`

---

## RCI-D4 — Prefer an upstream fix over a local workaround for infra compatibility

*Source: SUPP-28.*

When a dependency breaks against current infrastructure, check for and adopt an
upstream fix before building a local workaround. The Docker-25+ silent-skip in
the `sqldb` testing modules was resolved by pinning
`org.testcontainers:*` to `1.21.4` (an upstream backport), **not** by the
originally proposed Mill `TestModule` mixin with `-Dapi.version` in `forkArgs`.
The fix ships transparently to every downstream consumer via the normal
transitive dependency.

**Propagates to:**
- [x] `build.mill` (testcontainers pinned to `1.21.4`)
