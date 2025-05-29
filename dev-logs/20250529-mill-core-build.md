# Mill Core Build Migration - 2025-05-29

## Overview
Successfully migrated the `core` module from SBT to Mill build system. The module includes cross-platform compilation for Scala.js and JVM targets with shared code.

## Key Achievements

### 1. Fixed Deprecation Warnings
- Replaced deprecated `millSourcePath` with proper path handling
- Changed from `override def millSourcePath = ...` to using `os.pwd` with explicit paths
- Used `super.sources() ++ Seq(PathRef(...))` pattern for source definitions

### 2. Resolved Dependency Issues
The main compilation failures were due to missing dependencies not being properly declared:

**Shared module dependencies added:**
- `ivy"dev.zio::zio"` - Core ZIO functionality
- `ivy"dev.zio::zio-json"` - JSON support used in multiple files
- `ivy"dev.zio::zio-prelude"` - For Validation types

**JVM module dependencies:**
- `ivy"dev.zio::zio"`
- `ivy"dev.zio::zio-json"`

**JS module dependencies:**
- `ivy"dev.zio::zio"`
- `ivy"dev.zio::zio-json"`
- `ivy"org.scala-js::scalajs-dom::2.8.0"` - DOM API for ScalaJS

### 3. Fixed Import Issues
- Corrected import path: `import core.{MessageCatalogue, MessageId}` → `import works.iterative.core.{MessageCatalogue, MessageId}`

### 4. Cross-Platform Compilation Challenge
Encountered an issue with platform-specific traits:
- `CzechSupportPlatformSpecific` is defined in platform modules (js/jvm) but referenced in shared
- This pattern works in SBT with `CrossType.Full` but requires different handling in Mill
- **Temporary solution**: Provided default implementation in shared module with TODO comment
- **Long-term solution**: Need to investigate Mill's approach to cross-platform trait patterns

## Build Verification

### Compilation Output
Successfully compiled all modules:
- **Shared module**: 167 class files
- **JVM module**: 8 class files  
- **JS module**: 3 class files

### Useful Mill Commands
```bash
# Compile all modules
./mill verify.compile

# Show compilation result
./mill show core.MODULE.compile

# List compiled classes
find out/core/MODULE/compile.dest/classes -name "*.class"

# Generate and inspect JAR
./mill core.MODULE.jar
jar tf out/core/MODULE/jar.dest/out.jar
```

## Lessons Learned

1. **Dependency Management**: Mill modules need explicit dependency declarations even when using BOM. Unlike Maven's dependency management, Mill's BOM only provides version alignment, not automatic inclusion.

2. **Source Structure**: Mill prefers explicit source definitions using `super.sources() ++` pattern rather than overriding module paths.

3. **Cross-Platform Patterns**: Mill handles cross-platform code differently than SBT. Platform-specific traits referenced in shared code need special consideration.

4. **Build Verification**: Mill provides good visibility into build outputs through `show` commands and explicit output directories.

## Next Steps

1. Investigate proper Mill patterns for cross-platform trait implementations
2. Consider extracting the temporary `CzechSupport` implementation into a proper cross-platform solution
3. Continue migrating other modules following these patterns
4. Document Mill-specific patterns for the team

## Migration Status
- ✅ Core module structure
- ✅ Dependency resolution
- ✅ Compilation successful
- ⚠️  Cross-platform traits (temporary workaround in place)
- ✅ JAR generation