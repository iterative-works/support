# Publishing iw-support

This repository dual-publishes to **e-BS Nexus** and **GitHub Packages** via GitHub Actions. The
publish trait is `IWPublishModule` from `mill-iw-support`, configured through environment variables.
No GPG signing is used (suitable for private/internal registries). GitHub Actions is the sanctioned
release path; local publishing is available for development and testing without any credentials.

## Release process (sanctioned path)

Releases are driven by version tags in the format `vX.Y.Z`.

1. Edit `CommonVersion.publishVersion` in `build.mill` (line 40) — change from `"X.Y.Z-SNAPSHOT"`
   to `"X.Y.Z"`.
2. Commit on `main` and push.
3. Tag and push the tag:
   ```bash
   git tag vX.Y.Z
   git push --tags
   ```

The `publish.yml` workflow runs on every `v*` tag push. It:
1. Runs `./mill __.test` as a gate (fail-fast — the publish steps do not run if tests fail).
2. Publishes to GitHub Packages.
3. Publishes to e-BS Nexus (gated on `github.ref_type == 'tag'` — this step runs only for tags).

Both publish steps run sequentially. If either fails the workflow stops.

## Snapshot publishing

Every push to `main` triggers `publish.yml` and publishes a snapshot to **GitHub Packages only**.
The e-BS Nexus step is gated on `github.ref_type == 'tag'` and is skipped for branch pushes.

This means:
- Internal e-BS consumers see only tagged releases from Nexus — no snapshots land there.
- Snapshot builds are available on GitHub Packages for anyone with a PAT and `read:packages` scope.

## Local publishing for development

```bash
./mill __.publishLocal
```

Artifacts land in `~/.ivy2/local/works.iterative.support/`. No credentials are needed. This is the
contributor path for testing a local build against a consumer project. The previous `publish.sh`
wrapper has been removed because GitHub Actions is now the sanctioned release path.

## Required GitHub Actions secrets

| Secret | Purpose |
|--------|---------|
| `EBS_NEXUS_USERNAME` | e-BS Nexus credentials — configured manually by a maintainer with repo admin rights |
| `EBS_NEXUS_PASSWORD` | e-BS Nexus credentials — configured manually by a maintainer with repo admin rights |

`GITHUB_TOKEN` is auto-provided by Actions. The job-level `permissions: { contents: read, packages: write }`
declaration in `publish.yml` is what lets `GITHUB_TOKEN` write to GitHub Packages — no additional
secret is needed for the GitHub Packages step.

`EBS_NEXUS_USERNAME` and `EBS_NEXUS_PASSWORD` must be configured in **Settings → Secrets and
variables → Actions** before the first tag push. Without them, the snapshot path on `main` continues
to work (uses only `GITHUB_TOKEN`), but the Nexus publish step will fail at auth on tag pushes.

## Consumer setup

See `README.md` for resolver coordinates and dependency snippets for both e-BS Nexus and GitHub
Packages.

## Partial-failure recovery

If one publish step fails after the other has already succeeded (for example, a transient Nexus auth
error after GitHub Packages already accepted the artifacts), **re-run the workflow from the GitHub
Actions UI**. Both registries accept overwrites of the same coordinates — re-uploading is safe.

Do NOT bump the version to fix a transient upload failure. The correct recovery is to fix the root
cause (credentials, network, permissions) and re-run.

## Troubleshooting

- **Authentication failed on Nexus step** — verify `EBS_NEXUS_USERNAME` and `EBS_NEXUS_PASSWORD`
  are set in repo Actions secrets.
- **Module not being published** — run `./mill resolve __.publishArtifacts` locally to enumerate the
  publishable set. Modules on `BaseScalaJSModuleNoPublish` or `BaseModuleNoPublish` are excluded by
  design.
- **Workflow not triggering on tag push** — confirm the tag matches the `v*` pattern (e.g., `v0.1.14`
  not `0.1.14`).
- **Snapshot path skipping the Nexus step** — this is correct behavior. Only tag pushes reach the
  e-BS Nexus publish step.
