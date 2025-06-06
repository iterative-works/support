# Mongo Module Scala 3 Compatibility Fix - 2025-06-03

## Issue
The mongo module was failing to resolve dependencies in Mill because:
- The module source code uses Scala 3 syntax (extension methods, end markers, etc.)
- The dependencies (mongo-scala-driver and silencer-lib) are only available for Scala 2.13
- Mill was trying to find Scala 3 versions of these libraries which don't exist

## Solution
Used Mill's `.withDottyCompat()` method to enable Scala 2.13 dependencies to work with Scala 3:

### 1. Updated build.sc for mongo module:
```scala
object mongo extends BaseModule {
  def artifactName = "iw-support-mongo"
  
  // ... other settings ...
  
  def mvnDeps = super.mvnDeps() ++ Seq(
    IWMillDeps.zio,
    IWMillDeps.zioJson,
    IWMillDeps.zioConfig,
    IWMillDeps.zioInteropReactiveStreams,
    // Use Scala 2.13 dependencies with Scala 3 compatibility
    mvn"org.mongodb.scala::mongo-scala-driver::4.2.3".withDottyCompat(scalaVersion()),
    mvn"com.github.ghik::silencer-lib::1.4.2".withConfiguration("provided").withDottyCompat(scalaVersion())
  )
}
```

### 2. Fixed IWScalaModule semanticDb options:
Made semanticDb options conditional based on Scala version to avoid compilation errors:
```scala
def semanticDbOptions = Task {
    if (scalaVersion().startsWith("2.")) {
        // For Scala 2, semanticdb requires a compiler plugin
        // We'll skip it for now to avoid compilation errors
        Seq.empty
    } else {
        Seq("-Xsemanticdb")
    }
}
```

## Key Learnings
1. Mill's `.withDottyCompat()` is equivalent to SBT's `CrossVersion.for3Use2_13`
2. Scala 3 has binary compatibility with Scala 2.13, allowing mixed dependencies
3. The mongo module uses Scala 3 syntax, so it must compile with Scala 3 (not 2.13)
4. Dependencies specified with explicit suffixes (e.g., `_2.13`) need special handling

## Verification
- ✅ mongo module compiles successfully
- ✅ All modules compile with `./mill verify.compile`
- ✅ Dependencies resolve correctly with Scala 2.13 libraries used in Scala 3 module