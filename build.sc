//| mvnDeps:
//| - works.iterative::mill-iw-support::0.1.0-SNAPSHOT
import mill._
import mill.scalalib._
import mill.scalajslib._
import mill.scalajslib.api._
import mill.scalalib.publish._
import mill.scalalib.scalafmt._

// Import IW Mill Support library
import works.iterative.mill._

// ============================================================================
// Common Configuration
// ============================================================================

/** Common POM settings for all modules */
object CommonPomSettings {
  def apply(description: String) = PomSettings(
    description = description,
    organization = "works.iterative.support",
    url = "https://github.com/iterative-works/iw-support",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github("iterative-works", "iw-support"),
    developers = Seq(
      Developer("mprihoda", "Michal Příhoda", "https://github.com/mprihoda")
    )
  )
}

/** Common version for all published modules */
object CommonVersion {
  val publishVersion = "0.1.10-SNAPSHOT"
}

/** Common dependency groups */
object Dependencies {
  // ZIO dependencies
  def zioCore = Seq(IWMillDeps.zio)
  def zioJson = Seq(IWMillDeps.zioJson)
  def zioFull = zioCore ++ zioJson ++ Seq(IWMillDeps.zioPrelude)
  def zioConfig = Seq(IWMillDeps.zioConfig)
  def zioTest = Seq(IWMillDeps.zioTest, IWMillDeps.zioTestSbt)
  
  // Tapir dependencies
  def tapirCore = Seq(
    IWMillDeps.tapirCore,
    IWMillDeps.tapirZIOJson,
    IWMillDeps.zioJson
  )
  
  def tapirClient = tapirCore ++ Seq(
    IWMillDeps.tapirSttpClient,
    IWMillDeps.sttpClientZio
  )
  
  def tapirServerJvm = tapirClient ++ Seq(
    IWMillDeps.tapirZIO,
    IWMillDeps.tapirZIOHttp4sServer,
    IWMillDeps.zioConfig,
    IWMillDeps.zioInteropReactiveStreams,
    IWMillDeps.zioNIO
  )
  
  // UI dependencies - FIXED: Remove non-existent laminar-zio dependency
  def laminar = Seq(
    IWMillDeps.laminar,
    IWMillDeps.waypoint
  )
  
  // Silencer for Scala 3 compatibility
  def silencer = Seq(
    mvn"com.github.ghik:silencer-lib_2.13:1.4.2".withConfiguration("provided")
  )
  
  def withSilencer(deps: Seq[Dep])(implicit scalaVersion: String): Seq[Dep] = 
    deps ++ Seq(mvn"com.github.ghik::silencer-lib::1.4.2".withConfiguration("provided").withDottyCompat(scalaVersion))
}

// ============================================================================
// Base Module Traits
// ============================================================================

/** Base trait for all IW Support modules */
trait BaseModule extends IWScalaModule with IWPublishModule { outer =>
  override def publishVersion = CommonVersion.publishVersion
  
  override def sources = Task.Sources("src/main/scala")
  override def resources = Task.Sources("src/main/resources")
  
  // Inner test trait (matching original structure)
  trait BaseTests extends ScalaTests with TestModule.ZioTest {
    override def moduleDir = outer.moduleDir
    override def intellijModulePath: os.Path = outer.moduleDir / "src/test"
    
    override def sources = Task.Sources("src/test/scala")
    override def resources = Task.Sources("src/test/resources")
    
    def mvnDeps = super.mvnDeps() ++ Dependencies.zioTest
  }
}

/** Base trait for ScalaJS modules */
trait BaseScalaJSModule extends BaseModule with ScalaJSModule { outer =>
  override def scalaJSVersion = "1.19.0"
  
  trait BaseScalaJSTests extends ScalaJSTests with TestModule.ZioTest {
    override def scalaJSVersion = "1.19.0"
    override def moduleDir = outer.moduleDir
    override def intellijModulePath: os.Path = outer.moduleDir / "src/test"
    
    override def sources = Task.Sources("src/test/scala")
    override def resources = Task.Sources("src/test/resources")
    
    def mvnDeps = super.mvnDeps() ++ Dependencies.zioTest
  }
}

/** Base trait for modules with shared sources */
trait FullCrossScalaModule extends ScalaModule { outer =>
  def sharedSources = Task.Sources(outer.moduleDir / os.up / "shared" / "src" / "main" / "scala")
  def sharedResources = Task.Sources(outer.moduleDir / os.up / "shared" / "src" / "main" / "resources")
  
  override def sources = Task {
    super.sources() ++ sharedSources()
  }
  
  override def resources = Task {
    super.resources() ++ sharedResources()
  }
}

// ============================================================================
// Cross-Platform Module Helper
// ============================================================================

/** Helper for creating cross-compiled module pairs */
trait CrossModule extends Module {
  def moduleName: String
  def description: String
  
  trait SharedModule extends BaseModule with FullCrossScalaModule {
    def artifactName = s"iw-support-$moduleName"
    def pomSettings = CommonPomSettings(description)
  }
  
  trait JvmModule extends SharedModule
  trait JsModule extends SharedModule with BaseScalaJSModule
}

// JVM-only module helper
trait JvmOnlyModule extends BaseModule {
  def moduleName: String
  def description: String
  def artifactName = s"iw-support-$moduleName"
  def pomSettings = CommonPomSettings(description)
}

// Nested cross-module helper
trait NestedCrossModule extends CrossModule {
  def parentPath: os.RelPath
  
  trait NestedSharedModule extends SharedModule {
    override def moduleDir = super.moduleDir / os.up / os.up / parentPath
    
    override def sharedSources = Task.Sources(moduleDir / "shared" / "src" / "main" / "scala")
    override def sharedResources = Task.Sources(moduleDir / "shared" / "src" / "main" / "resources")
    
    override def sources = Task {
      super.sources() ++ sharedSources()
    }
    
    override def resources = Task {
      super.resources() ++ sharedResources()
    }
  }
  
  trait NestedJvmModule extends NestedSharedModule
  trait NestedJsModule extends NestedSharedModule with BaseScalaJSModule
}

// ============================================================================
// Module Definitions
// ============================================================================

// Core module - cross-compiled
object core extends CrossModule {
  def moduleName = "core"
  def description = "IW Support Core Library"
  
  object jvm extends JvmModule {
    def mvnDeps = super.mvnDeps() ++ Dependencies.zioFull
    object test extends BaseTests
  }
  
  object js extends JsModule {
    def mvnDeps = super.mvnDeps() ++ Dependencies.zioFull ++ Seq(IWMillDeps.scalaJsDom)
    object test extends BaseScalaJSTests
  }
}

// Entity module - cross-compiled
object entity extends CrossModule {
  def moduleName = "entity"
  def description = "IW Support Entity Library"
  
  object jvm extends JvmModule {
    def moduleDeps = Seq(core.jvm)
    def mvnDeps = super.mvnDeps() ++ Dependencies.zioCore
  }
  
  object js extends JsModule {
    def moduleDeps = Seq(core.js)
    def mvnDeps = super.mvnDeps() ++ Dependencies.zioCore
  }
}

// Service specs module - cross-compiled
object serviceSpecs extends CrossModule {
  def moduleName = "service-specs"
  def description = "IW Support Service Specs Library"
  
  object jvm extends JvmModule {
    def moduleDeps = Seq(core.jvm)
    def mvnDeps = super.mvnDeps() ++ Dependencies.zioCore ++ Seq(IWMillDeps.zioTest)
  }
  
  object js extends JsModule {
    def moduleDeps = Seq(core.js)
    def mvnDeps = super.mvnDeps() ++ Dependencies.zioCore ++ Seq(IWMillDeps.zioTest)
  }
}

// Tapir module - cross-compiled with shared tests
object tapir extends CrossModule {
  def moduleName = "tapir"
  def description = "IW Support Tapir Library"
  
  trait TapirModule extends SharedModule {
    trait TapirTests extends BaseTests {
      def sharedTestSources = Task.Sources(
        moduleDir / os.up / os.up / "shared" / "src" / "test" / "scala"
      )
      
      override def sources = Task {
        super.sources() ++ sharedTestSources()
      }
    }
  }
  
  object jvm extends JvmModule with TapirModule {
    def moduleDeps = Seq(core.jvm)
    def mvnDeps = super.mvnDeps() ++ Dependencies.tapirServerJvm ++ Dependencies.silencer
    
    object test extends TapirTests
  }
  
  object js extends JsModule with TapirModule {
    def moduleDeps = Seq(core.js)
    def mvnDeps = super.mvnDeps() ++ Dependencies.tapirClient
    
    trait JsTapirTests extends BaseScalaJSTests {
      def sharedTestSources = Task.Sources(
        moduleDir / os.up / os.up / "shared" / "src" / "test" / "scala"
      )
      
      override def sources = Task {
        super.sources() ++ sharedTestSources()
      }
    }
    
    object test extends JsTapirTests
  }
}

// Mongo support - JVM only
object mongo extends JvmOnlyModule {
  def moduleName = "mongo"
  def description = "IW Support MongoDB Library"
  def moduleDeps = Seq(core.jvm)
  
  def mvnDeps = super.mvnDeps() ++ Dependencies.zioCore ++ Dependencies.zioJson ++ 
    Dependencies.zioConfig ++ Seq(
      IWMillDeps.zioInteropReactiveStreams,
      mvn"org.mongodb.scala::mongo-scala-driver::4.2.3".withDottyCompat(scalaVersion())
    ) ++ Dependencies.withSilencer(Seq())(scalaVersion())
  
  object it extends BaseTests {
    override def moduleDir = mongo.moduleDir / "it"
    override def intellijModulePath: os.Path = mongo.moduleDir / "it" / "src/test"
    
    override def sources = Task.Sources(mongo.moduleDir / "it" / "src" / "test" / "scala")
    override def resources = Task.Sources(mongo.moduleDir / "it" / "src" / "test" / "resources")
    
    def moduleDeps = Seq(mongo)
    
    def mvnDeps = super.mvnDeps() ++ Dependencies.zioCore ++ Dependencies.zioConfig
  }
}

// SQL Database support - JVM only
object sqldb extends JvmOnlyModule {
  def moduleName = "sqldb"
  def description = "IW Support SQL Database Library"
  def moduleDeps = Seq(core.jvm)
  
  def mvnDeps = super.mvnDeps() ++ Dependencies.zioCore ++ Dependencies.zioJson ++ 
    Dependencies.zioConfig ++ Seq(
      IWMillDeps.magnumZIO,
      IWMillDeps.magnumPG,
      IWMillDeps.chimney,
      mvn"org.flywaydb:flyway-core:11.4.0",
      mvn"org.flywaydb:flyway-database-postgresql:11.4.0",
      mvn"org.postgresql:postgresql:42.7.5",
      mvn"com.zaxxer:HikariCP:6.2.1"
    )
  
  // Testing support sub-module
  object testing extends JvmOnlyModule {
    def moduleName = "sqldb-testing"
    def description = "IW Support SQL Database Testing Library"
    override def moduleDir = sqldb.moduleDir / "testing-support"
    override def intellijModulePath: os.Path = sqldb.moduleDir / "testing-support"
    
    override def sources = Task.Sources(moduleDir / "src" / "main" / "scala")
    override def resources = Task.Sources(moduleDir / "src" / "main" / "resources")
    
    def moduleDeps = Seq(sqldb)
    def mvnDeps = super.mvnDeps() ++ Dependencies.zioCore ++ Seq(
      IWMillDeps.zioTest,
      IWMillDeps.logbackClassic,
      mvn"org.testcontainers:testcontainers:1.20.4",
      mvn"org.testcontainers:postgresql:1.20.4",
      mvn"com.dimafeng::testcontainers-scala-postgresql::0.41.5"
    )
  }
}

// Email support - JVM only
object email extends JvmOnlyModule {
  def moduleName = "email"
  def description = "IW Support Email Library"
  def moduleDeps = Seq(core.jvm)
  
  def mvnDeps = super.mvnDeps() ++ Dependencies.zioCore ++ Dependencies.zioConfig ++ Seq(
    mvn"org.apache.commons:commons-email:1.5"
  ) ++ Dependencies.withSilencer(Seq())(scalaVersion())
}

// Codecs module - pure shared sources
object codecs extends CrossModule {
  def moduleName = "codecs"
  def description = "IW Support Codecs Library"
  
  trait CodecsModule extends SharedModule {
    // Pure cross-compilation - only shared sources
    override def sources = Task.Sources("src/main/scala")
    override def resources = Task.Sources("src/main/resources")
  }
  
  object jvm extends JvmModule with CodecsModule {
    def moduleDeps = Seq(core.jvm, entity.jvm, tapir.jvm)
    def mvnDeps = super.mvnDeps() ++ Dependencies.zioJson
  }
  
  object js extends JsModule with CodecsModule {
    def moduleDeps = Seq(core.js, entity.js, tapir.js)
    def mvnDeps = super.mvnDeps() ++ Dependencies.zioJson
  }
}

// UI module - cross-compiled for JVM and JS
object ui extends CrossModule {
  def moduleName = "ui"
  def description = "IW Support UI Library"
  
  object jvm extends JvmModule {
    def moduleDeps = Seq(core.jvm, tapir.jvm)
    def mvnDeps = super.mvnDeps() ++ Dependencies.zioCore ++ Seq(
      IWMillDeps.scalatags,
      // Apache POI for Excel support
      mvn"org.apache.poi:poi-ooxml:5.2.1"
    )
  }
  
  object js extends JsModule {
    def moduleDeps = Seq(core.js, tapir.js)
    def mvnDeps = super.mvnDeps() ++ Dependencies.zioCore ++ Dependencies.zioJson ++ Dependencies.laminar ++ Seq(
      IWMillDeps.urlDsl,
      // Laminext libraries
      mvn"io.laminext::core::${IWMillVersions.laminext}",
      mvn"io.laminext::ui::${IWMillVersions.laminext}",
      mvn"io.laminext::tailwind::${IWMillVersions.laminext}",
      mvn"io.laminext::validation-core::${IWMillVersions.laminext}"
    )
  }
}

// Root aggregate module
object root extends BaseModule {
  def artifactName = "iw-support"
  override def publishVersion = "0.0.0"
  def pomSettings = CommonPomSettings("IW Support Root Aggregate")
  
  def moduleDeps = Seq(
    core.jvm, core.js,
    entity.jvm, entity.js,
    serviceSpecs.jvm, serviceSpecs.js,
    tapir.jvm, tapir.js,
    mongo, sqldb, sqldb.testing, email,
    codecs.jvm, codecs.js,
    ui.jvm, ui.js
  )
}

// Convenience commands
def verifyBuild() = Task.Command {
  println("Verifying build structure...")
  println(s"Total modules: ${root.moduleDeps.size}")
  
  // Run compile on a sample of modules to verify
  core.jvm.compile()
  core.js.compile()
  println("Build structure verified successfully!")
}

def runAllTests() = Task.Command {
  println("Running all tests...")
  // Run tests for modules that have them
  core.jvm.test.testForked()
  core.js.test.testForked()
  tapir.jvm.test.testForked()
  tapir.js.test.testForked()
  println("All tests completed!")
}