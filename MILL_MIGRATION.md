# Mill Build Migration Guide

This document describes the experimental Mill build setup for the iw-support project.

## Overview

We're migrating from SBT to Mill build tool incrementally, starting with the `core` module. The Mill build coexists with the SBT build, allowing us to test and validate the migration without disrupting the existing workflow.

## Setup

1. **First-time setup**: Run the setup script to build and publish the mill-iw-support library locally:
   ```bash
   ./setup-mill.sh
   ```

2. **Manual setup** (if needed):
   ```bash
   # Build mill-iw-support
   cd ../iw-project-support/mill-iw-support
   ./mill core.publishLocal
   
   # Return to project
   cd ../../project
   
   # Test compilation
   ./mill verify.compile
   ```

## Project Structure

The Mill build maintains the existing SBT directory structure:
- `core/shared/src/main/scala` - Platform-independent code
- `core/jvm/src/main/scala` - JVM-specific code
- `core/js/src/main/scala` - JavaScript-specific code

## Available Commands

### Compilation
- `./mill core.jvm.compile` - Compile JVM core module
- `./mill core.js.compile` - Compile JS core module
- `./mill email.compile` - Compile email module
- `./mill verify.compile` - Compile all modules

### Testing
- `./mill core.jvm.test.test` - Run JVM tests
- `./mill core.js.test.test` - Run JS tests
- `./mill verify.test` - Run all tests

### Module-specific Commands
- `./mill email.compile` - Compile email module
- `./mill mongo.compile` - Compile mongo module
- `./mill sqldb.compile` - Compile sqldb module
- `./mill tapir.jvm.compile` - Compile tapir JVM module
- `./mill entity.jvm.compile` - Compile entity JVM module

### Other Commands
- `./mill resolve __` - List all available tasks
- `./mill inspect core.jvm.ivyDeps` - Show dependencies
- `./mill show email.mvnDeps` - Show email module dependencies
- `./mill verify.checkFormat` - Check code formatting

## Key Differences from SBT

1. **Module Definition**: Mill uses objects instead of lazy vals
2. **Cross-Compilation**: Explicit module definitions for each platform
3. **Dependencies**: Uses `ivyDeps` instead of `libraryDependencies`
4. **Settings**: Uses methods (def) instead of settings DSL

## Migration Status

- [x] Core module setup
- [x] Cross-compilation (JVM/JS)
- [x] Directory structure preservation
- [x] Basic compilation
- [x] Entity module (cross-compiled)
- [x] Service-specs module (cross-compiled)
- [x] Tapir module (cross-compiled)
- [x] Mongo module (JVM only)
- [x] SQLDB module with testing support (JVM only)
- [x] Email module (JVM only)
- [ ] Test execution for all modules
- [ ] Publishing configuration
- [ ] Remaining modules (files, autocomplete, hashicorp, codecs, paygate, akka-persistence, ui, forms, scenarios, http)

## Next Steps

1. Verify that core module compiles correctly
2. Run tests to ensure functionality
3. Compare artifacts with SBT build
4. Migrate next module (suggested: `entity`)

## Troubleshooting

### Missing mill-iw-support
If you get an error about missing `works.iterative::mill-iw-support`, run:
```bash
cd ../iw-project-support/mill-iw-support
./mill core.publishLocal
```

### Compilation Errors
Check that:
1. The Scala version matches (3.7.0)
2. Source directories are correctly configured
3. Dependencies are properly resolved

### Performance
Mill builds should be significantly faster than SBT, especially for incremental compilation. If builds are slow, check:
- Mill daemon status: `./mill shutdown` then retry
- Zinc compiler cache