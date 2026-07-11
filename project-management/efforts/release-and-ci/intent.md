<!-- PURPOSE: The value-axis compass for the release-and-ci effort — trustworthy, decoupled distribution + automated quality gates -->
<!-- PURPOSE: Judges every increment against the goal, invariants, and fit; amend only by dated amendment -->

# Release & CI — Standing Intent: trustworthy distribution and automated quality gates

`iw-support` can be consumed and contributed to without depending on a single
employer's infrastructure or on manual vigilance. Distribution is decoupled and
redundant; quality is enforced automatically before code lands; and a broken
environment fails loudly instead of passing green.

## Goal — the measurable change

- A consumer outside the e-BS VPN can resolve `iw-support` with only a GitHub
  PAT, while every existing e-BS consumer keeps resolving from Nexus **without
  changing its resolver config**.
- A pull request that does not compile (JVM + JS) or does not pass tests /
  formatting goes **red before merge**; contributors get that feedback locally
  via git hooks before they push.
- A test that depends on Docker (or any external infrastructure) fails
  **loudly** when that infrastructure is incompatible — never degrades to a
  silent skip / false-green.

## Before → after

- **Before:** publish coupled exclusively to e-BS Nexus via `IWPublishModule`;
  no CI, no automated quality gates; contributors can push code that doesn't
  compile; Docker-gated `sqldb` tests silently skip on Docker 25+ and report a
  false-green build.
- **After:** **dual-publish** to e-BS Nexus **and** GitHub Packages
  (`iterative-works/support`), no Sonatype Central and no GPG; GitHub Actions
  CI validates compilation/tests/formatting on every PR, with git hooks giving
  the same feedback pre-push; `testcontainers` pinned so Docker-gated tests run
  or fail rather than skip.

## Invariants — what must keep holding

- **e-BS consumers are never disturbed.** They keep resolving from Nexus with
  unchanged resolver config; no proxy indirection, no new external dependency
  forced on them.
- **No Central, no GPG ceremony.** This is a library for *our* applications,
  not a public framework; Maven Central's discoverability and signing ceremony
  buy nothing here.
- **Test output stays pristine.** An infrastructure failure surfaces as a
  failure, not a skip. A green build means the tests actually ran.
- **Publishability is type-level.** Whether a module publishes is a property of
  its base trait (`BaseModule` vs `BaseModuleNoPublish`), not per-module
  boilerplate or magic version markers.

## Fit test — how we'll know it worked

- A fresh, non-e-BS project with a GH PAT resolves and builds against
  `iw-support` with no e-BS access.
- A PR with a compilation error, a failing test, or a formatting violation
  cannot be merged green.
- Running the `sqldb` tests against a Docker engine the client version can't
  speak produces **red tests**, not skipped specs.
