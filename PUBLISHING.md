# Publishing iw-support to e-BS Nexus

This document describes how to publish the iw-support modules to the e-BS Nexus Maven repository.

## Prerequisites

Set up your environment variables for authentication:
```bash
export EBS_NEXUS_USERNAME=your-username
export EBS_NEXUS_PASSWORD=your-password
```

Mill will automatically use these environment variables when publishing.

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

The current version is set in the `BaseModule` trait as:
```scala
override def publishVersion = "0.1.10-SNAPSHOT"
```

To change the version:
1. Edit the `publishVersion` in `build.sc`
2. Commit the change
3. Tag the release if it's not a SNAPSHOT

## Publishing Configuration

The IWPublishModule trait in mill-iw-support is configured with:
- **GPG signing disabled by default** (suitable for private repositories)
- **Staging disabled by default** (direct publishing to repository)
- **Repository URLs**: 
  - Release: https://nexus.e-bs.cz/repository/maven-releases/
  - Snapshot: https://nexus.e-bs.cz/repository/maven-snapshots/

## Troubleshooting

1. **Authentication Failed**: Make sure your environment variables are set correctly
2. **Module Not Found**: Use `./mill resolve __.publish` to see all publishable modules
3. **Version Conflicts**: Snapshot versions always publish to the snapshot repository
4. **Publishing Hangs**: If publishing seems to hang, it may be uploading large artifacts. Check network connectivity and repository status