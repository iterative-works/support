<!-- PURPOSE: Record of how the SSR form browser e2e suite was generated and what it covers -->
<!-- PURPOSE: Maps each scenario to the verified behavior it pins; entry point for extending the suite -->

# E2E Generation Summary — SSR form loop

Generated 2026-07-11 from the real-browser verification of `SsrFormScenario`
(Playwright MCP session; findings logged in `worklog.md` and the
"Found only by real-browser verification" section of `uiform-gap-inventory.md`).

Note: this effort uses the efforts structure, not `project-management/issues/`,
so this summary lives here instead of an issue directory and the source
"verification artifacts" are the worklog entry plus `webapp-knowledge/`
(both produced by the same verification session) rather than
`verification-phase-*.md` files.

## Framework

The repo's own `e2e-testing` module (Cucumber + Playwright JVM) — first
in-repo consumer, dogfooding what we ship to client projects.

## Run it

```bash
./mill formsScenarios.jvm.e2e
```

Self-contained: the suite starts the scenarios server **in-process** on port
8392 (from `baseUrl` in `application.conf`), so there is no external server
to manage and no Mill daemon lock conflict. Headless by default;
`channel = "chrome"` launches the system Chrome (the bundled Playwright
chromium cannot resolve host libraries on nix-profile machines) —
override with `PLAYWRIGHT_CHANNEL` where the bundled build works.

## Generated files

- `build.mill` — `formsScenarios.jvm.e2e` test module (JUnit4 runner,
  sources under `src/e2e/`)
- `forms/scenarios/jvm/src/e2e/resources/features/ssr-form.feature` —
  five scenarios
- `.../e2e/SsrFormE2eSuite.scala` — Cucumber JUnit entry point
- `.../e2e/SsrFormSteps.scala` — step definitions (IdPath-derived
  selectors, outcome-based waits)
- `.../e2e/ScenariosServerHooks.scala` — in-process server lifecycle
- `.../e2e/resources/application.conf` — baseUrl/headless/channel
- `e2e-testing/...` — framework gained optional `channel` launch config

## Scenario → pinned bug map

| Scenario | Pins |
|---|---|
| Choosing order reveals the delivery section without leaving the page | HTMX fragment swap works while required fields are blank (`novalidate`; htmx would otherwise halt every request) |
| Submitting a blank form shows human-readable required errors | Server-side Required validation + label resolution under the form prefix (raw keys once leaked for row fields) |
| Typed text survives a change-triggered re-render | Re-render trigger narrowed to committed-choice controls (outerHTML swap once wiped in-flight input) |
| Removing one row keeps the other row's data | Multi-value POST decoding via `QueryParams.decode` (`asURLEncodedForm` merged duplicate `__items` and deleted all rows) |
| A valid order submits faithfully including Czech input | UTF-8 charset in meta + Content-Type (browsers once submitted Latin-1 mojibake) |

## Test run result

All 5 scenarios pass (`0 failed, 5 total, ~8s` after compile). Existing
suites unaffected: forms.jvm 49 + formsScenarios.jvm 8 still green.

## Known gaps / skipped

- File upload is not exercised (out of slice scope; the
  serialize-pdf-roundtrip slice should extend the suite when it lands).
- No CI wiring yet — CI hosts need either `npx playwright install
  --with-deps chromium` (then unset the chrome channel) or a system Chrome.
