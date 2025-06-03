# Mill Dependency Management Update - 2025-06-03

## Overview
Updated the Mill build configuration to use direct dependency definitions instead of a BOM (Bill of Materials) module approach. This change simplifies dependency management and avoids issues with ScalaJS support in Mill's BOM module.

## Changes Made

### 1. Updated IWMillDeps.scala
- Changed from returning String literals to returning actual `mvn"..."` dependencies
- Added necessary Mill imports (`mill._` and `mill.scalalib._`)
- Added `scalaJsDom` dependency definition
- All dependencies are now ready to use directly in build.sc files

### 2. Removed BOM Module
- Removed the `bom` object that extended `IWBomModule`
- Removed all `bomModuleDeps` references from modules
- This simplifies the build configuration and avoids ScalaJS compatibility issues

### 3. Updated Module Dependencies
- Changed from `mvn"dev.zio::zio"` to `IWMillDeps.zio`
- Changed from `mvn"dev.zio::zio-json"` to `IWMillDeps.zioJson`
- Changed from `mvn"dev.zio::zio-prelude"` to `IWMillDeps.zioPrelude`
- Changed from `mvn"dev.zio::zio-test"` to `IWMillDeps.zioTest`
- Changed from `mvn"dev.zio::zio-test-sbt"` to `IWMillDeps.zioTestSbt`
- Changed from `mvn"org.scala-js::scalajs-dom::2.8.0"` to `IWMillDeps.scalaJsDom`

## Benefits

1. **Simpler Configuration**: No need to define and maintain a BOM module
2. **Direct Dependency Usage**: Dependencies are imported directly from IWMillDeps
3. **ScalaJS Compatibility**: Avoids issues with BOM module not supporting ScalaJS
4. **Type Safety**: Dependencies are typed as Mill dependency objects, not strings
5. **Centralized Versions**: All versions still managed in IWMillVersions

## Usage Example

```scala
object myModule extends IWScalaModule {
  def mvnDeps = super.mvnDeps() ++ Seq(
    IWMillDeps.zio,
    IWMillDeps.zioJson,
    IWMillDeps.zioPrelude
  )
}
```

## Verification
- ✅ Core module (JVM & JS) compiles successfully
- ✅ Entity module (JVM & JS) compiles successfully
- ✅ All modules compile with `./mill verify.compile`

## Next Steps
1. Continue migrating other modules following this pattern
2. Update documentation to reflect the new dependency management approach
3. Consider creating helper methods in IWMillDeps for common dependency groups