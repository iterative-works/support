# Publishing iw-support to e-BS Nexus

This document describes how to publish the iw-support modules to the e-BS Nexus Maven repository.

## Prerequisites

Set up your environment variables for authentication:
```bash
export EBS_NEXUS_USERNAME=your-username
export EBS_NEXUS_PASSWORD=your-password
```

The publish script translates these into the `MILL_SONATYPE_USERNAME` / `MILL_SONATYPE_PASSWORD`
variables that Mill expects.

## Repository URLs

`IWPublishModule` defaults to:
- **Releases:** `https://nexus.e-bs.cz/repository/maven-releases/`
- **Snapshots:** `https://nexus.e-bs.cz/repository/maven-snapshots/`

These can be overridden per-project by setting `IW_PUBLISH_RELEASE_URI` / `IW_PUBLISH_SNAPSHOT_URI`
environment variables, or by overriding `publishReleaseUri` / `publishSnapshotUri` in code.

## Publishing Commands

### Using the publish.sh Script (Recommended)

The easiest way to publish is using the provided script:

```bash
# Publish all modules
./publish.sh

# Publish a specific module
./publish.sh --module core.jvm

# Publish to local repository for testing
./publish.sh --local
```

### Direct Mill Commands

You can also use Mill directly:

```bash
# Set Mill-specific environment variables
export MILL_SONATYPE_USERNAME=$EBS_NEXUS_USERNAME
export MILL_SONATYPE_PASSWORD=$EBS_NEXUS_PASSWORD

# Publish all modules
./mill __.publish

# Publish a specific module
./mill core.jvm.publish

# Publish to local repository
./mill __.publishLocal
```

## Version Management

The current version is set in `CommonVersion.publishVersion` in `build.mill`.

To change the version:
1. Edit `publishVersion` in `build.mill`
2. Commit the change
3. Tag the release if it's not a SNAPSHOT

## Publishing Configuration

The `IWPublishModule` trait in `mill-iw-support` is configured with:
- **GPG signing disabled by default** (suitable for private repositories)
- **Staging disabled by default** (direct publishing to repository)

## Troubleshooting

1. **Authentication Failed**: Make sure `EBS_NEXUS_USERNAME` and `EBS_NEXUS_PASSWORD` are set correctly
2. **Module Not Found**: Use `./mill resolve __.publish` to see all publishable modules
3. **Version Conflicts**: Snapshot versions always publish to the snapshot repository
4. **Publishing Hangs**: If publishing seems to hang, it may be uploading large artifacts. Check network connectivity and repository status
