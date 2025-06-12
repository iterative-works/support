//| mvnDeps:
//| - works.iterative::mill-iw-support::0.1.0-SNAPSHOT
import mill._
import mill.scalalib._
import mill.scalajslib._
import mill.scalajslib.api._
import mill.scalalib.publish._
import mill.scalalib.scalafmt._

// Import IW Mill Support library
// Note: Update this to the correct published version once available
// For now, you may need to publish mill-iw-support locally first
import works.iterative.mill._

// Taken from MavenModule - we need to support ScalaJS and Scala in the same way
trait BaseModule extends IWScalaModule with IWPublishModule { outer =>
    override def publishVersion = "0.1.10-SNAPSHOT"

    override def sources = Task.Sources("src/main/scala")
    override def resources = Task.Sources("src/main/resources")

    trait BaseTests extends ScalaTests with TestModule.ZioTest {
        override def moduleDir = outer.moduleDir
        override def intellijModulePath: os.Path = outer.moduleDir / "src/test"

        override def sources = Task.Sources("src/test/scala")
        override def resources = Task.Sources("src/test/resources")

        def mvnDeps = super.mvnDeps() ++ Seq(
          IWMillDeps.zioTest,
          IWMillDeps.zioTestSbt
        )
    }
}

trait BaseScalaJSModule extends BaseModule with ScalaJSModule { outer =>
    override def scalaJSVersion = "1.19.0"

    trait BaseScalaJSTests extends ScalaJSTests with TestModule.ZioTest {
        override def scalaJSVersion = "1.19.0"
        override def moduleDir = outer.moduleDir
        override def intellijModulePath: os.Path = outer.moduleDir / "src/test"

        override def sources = Task.Sources("src/test/scala")
        override def resources = Task.Sources("src/test/resources")

        def mvnDeps = super.mvnDeps() ++ Seq(
          IWMillDeps.zioTest,
          IWMillDeps.zioTestSbt
        )
    }
}

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

// Core module - cross-compiled for JVM and JS
object core extends Module {

  // Base trait for all core module variants
  trait CoreModule extends BaseModule with FullCrossScalaModule {
    def artifactName = "iw-support-core"

    def pomSettings = PomSettings(
      description = "IW Support Core Library",
      organization = "works.iterative.support",
      url = "https://github.com/iterative-works/iw-support",
      licenses = Seq(License.MIT),
      versionControl = VersionControl.github("iterative-works", "iw-support"),
      developers = Seq(
        Developer("mprihoda", "Michal Příhoda", "https://github.com/mprihoda")
      )
    )
  }

  // JVM-specific module
  object jvm extends CoreModule {
    def mvnDeps = super.mvnDeps() ++ Seq(
      IWMillDeps.zio,  // ZIO for JVM
      IWMillDeps.zioJson,  // JSON support
      IWMillDeps.zioPrelude
    )

    // Test module for JVM
    object test extends BaseTests
  }

  // JavaScript-specific module
  object js extends CoreModule with BaseScalaJSModule {
    def mvnDeps = super.mvnDeps() ++ Seq(
      IWMillDeps.zio,  // ZIO for JS
      IWMillDeps.zioJson,  // JSON support
      IWMillDeps.zioPrelude,
      IWMillDeps.scalaJsDom
    )

    // Test module for JS
    object test extends BaseScalaJSTests
  }
}

// Entity module - cross-compiled for JVM and JS
object entity extends Module {

  // Base trait for all entity module variants
  trait EntityModule extends BaseModule with FullCrossScalaModule {
    def artifactName = "iw-support-entity"

    def pomSettings = PomSettings(
      description = "IW Support Entity Library",
      organization = "works.iterative.support",
      url = "https://github.com/iterative-works/iw-support",
      licenses = Seq(License.MIT),
      versionControl = VersionControl.github("iterative-works", "iw-support"),
      developers = Seq(
        Developer("mprihoda", "Michal Příhoda", "https://github.com/mprihoda")
      )
    )
  }

  // JVM-specific module
  object jvm extends EntityModule {
    def moduleDeps = Seq(core.jvm)

    def mvnDeps = super.mvnDeps() ++ Seq(
      IWMillDeps.zio
    )
  }

  // JavaScript-specific module
  object js extends EntityModule with BaseScalaJSModule {
    def moduleDeps = Seq(core.js)

    def mvnDeps = super.mvnDeps() ++ Seq(
      IWMillDeps.zio
    )
  }
}

// Service specs module - cross-compiled for JVM and JS
object serviceSpecs extends Module {

  // Base trait for all service specs module variants
  trait ServiceSpecsModule extends BaseModule with FullCrossScalaModule {
    def artifactName = "iw-support-service-specs"

    def pomSettings = PomSettings(
      description = "IW Support Service Specs Library",
      organization = "works.iterative.support",
      url = "https://github.com/iterative-works/iw-support",
      licenses = Seq(License.MIT),
      versionControl = VersionControl.github("iterative-works", "iw-support"),
      developers = Seq(
        Developer("mprihoda", "Michal Příhoda", "https://github.com/mprihoda")
      )
    )
  }

  // JVM-specific module
  object jvm extends ServiceSpecsModule {
    def moduleDeps = Seq(core.jvm)

    def mvnDeps = super.mvnDeps() ++ Seq(
      IWMillDeps.zio,
      IWMillDeps.zioTest
    )
  }

  // JavaScript-specific module
  object js extends ServiceSpecsModule with BaseScalaJSModule {
    def moduleDeps = Seq(core.js)

    def mvnDeps = super.mvnDeps() ++ Seq(
      IWMillDeps.zio,
      IWMillDeps.zioTest
    )
  }
}

// Tapir module - cross-compiled for JVM and JS
object tapir extends Module {

  // Base trait for all tapir module variants
  trait TapirModule extends BaseModule with FullCrossScalaModule {
    def artifactName = "iw-support-tapir"

    def pomSettings = PomSettings(
      description = "IW Support Tapir Library",
      organization = "works.iterative.support",
      url = "https://github.com/iterative-works/iw-support",
      licenses = Seq(License.MIT),
      versionControl = VersionControl.github("iterative-works", "iw-support"),
      developers = Seq(
        Developer("mprihoda", "Michal Příhoda", "https://github.com/mprihoda")
      )
    )

    // Test module with shared tests
    trait TapirTests extends BaseTests {
      def sharedTestSources = Task.Sources(
        moduleDir / os.up / os.up / "shared" / "src" / "test" / "scala"
      )
      
      override def sources = Task {
        super.sources() ++ sharedTestSources()
      }
    }
  }

  // JVM-specific module
  object jvm extends TapirModule {
    def moduleDeps = Seq(core.jvm)

    def mvnDeps = super.mvnDeps() ++ Seq(
      IWMillDeps.zio,
      IWMillDeps.tapirCore,
      IWMillDeps.tapirZIOJson,
      IWMillDeps.zioJson,
      IWMillDeps.tapirSttpClient,
      IWMillDeps.sttpClientZio,
      IWMillDeps.tapirZIO,
      IWMillDeps.tapirZIOHttp4sServer,
      IWMillDeps.zioConfig,
      IWMillDeps.zioInteropReactiveStreams,
      IWMillDeps.zioNIO,
      // Silencer lib for cross-compilation (2.13 version for Scala 3)
      mvn"com.github.ghik:silencer-lib_2.13:1.4.2".withConfiguration("provided")
    )

    // Excluding conflicting dependency
    // TODO: Mill doesn't have direct exclusion support like SBT
    // The conflict should be resolved by Coursier automatically
    // If needed, we can use mapDependencies to filter out specific dependencies

    // Test module for JVM
    object test extends TapirTests
  }

  // JavaScript-specific module
  object js extends TapirModule with BaseScalaJSModule {
    def moduleDeps = Seq(core.js)

    def mvnDeps = super.mvnDeps() ++ Seq(
      IWMillDeps.zio,
      IWMillDeps.tapirCore,
      IWMillDeps.tapirZIOJson,
      IWMillDeps.zioJson,
      IWMillDeps.tapirSttpClient,
      IWMillDeps.sttpClientZio
    )

    // Test module for JS  
    object test extends BaseScalaJSTests {
      def sharedTestSources = Task.Sources(
        moduleDir / os.up / os.up / "shared" / "src" / "test" / "scala"
      )
      
      override def sources = Task {
        super.sources() ++ sharedTestSources()
      }
    }
  }
}

// Mongo support module - JVM only
object mongo extends BaseModule {
  def artifactName = "iw-support-mongo"
  
  def pomSettings = PomSettings(
    description = "IW Support MongoDB Library",
    organization = "works.iterative.support",
    url = "https://github.com/iterative-works/iw-support",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github("iterative-works", "iw-support"),
    developers = Seq(
      Developer("mprihoda", "Michal Příhoda", "https://github.com/mprihoda")
    )
  )
  
  def moduleDeps = Seq(core.jvm)
  
  def mvnDeps = super.mvnDeps() ++ Seq(
    IWMillDeps.zio,
    IWMillDeps.zioJson,
    IWMillDeps.zioConfig,
    IWMillDeps.zioInteropReactiveStreams,
    // MongoDB-specific dependencies (only available for Scala 2.13)
    mvn"org.mongodb.scala::mongo-scala-driver::4.2.3".withDottyCompat(scalaVersion()),
    mvn"com.github.ghik::silencer-lib::1.4.2".withConfiguration("provided").withDottyCompat(scalaVersion())
  )
  
  // Integration test module
  object it extends BaseTests {
    override def moduleDir = mongo.moduleDir / "it"
    override def intellijModulePath: os.Path = mongo.moduleDir / "it" / "src/test"
    
    override def sources = Task.Sources(mongo.moduleDir / "it" / "src" / "test" / "scala")
    override def resources = Task.Sources(mongo.moduleDir / "it" / "src" / "test" / "resources")
    
    def moduleDeps = Seq(mongo)
    
    def mvnDeps = super.mvnDeps() ++ Seq(
      IWMillDeps.zio,
      IWMillDeps.zioConfig
    )
  }
}

// SQL Database support module - JVM only
object sqldb extends BaseModule {
  def artifactName = "iw-support-sqldb"
  
  def pomSettings = PomSettings(
    description = "IW Support SQL Database Library",
    organization = "works.iterative.support",
    url = "https://github.com/iterative-works/iw-support",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github("iterative-works", "iw-support"),
    developers = Seq(
      Developer("mprihoda", "Michal Příhoda", "https://github.com/mprihoda")
    )
  )
  
  def moduleDeps = Seq(core.jvm)
  
  def mvnDeps = super.mvnDeps() ++ Seq(
    IWMillDeps.zio,
    IWMillDeps.zioJson,
    IWMillDeps.zioConfig,
    IWMillDeps.magnumZIO,
    IWMillDeps.magnumPG,
    IWMillDeps.chimney,
    // SQL database specific dependencies
    mvn"org.flywaydb:flyway-core:11.4.0",
    mvn"org.flywaydb:flyway-database-postgresql:11.4.0",
    mvn"org.postgresql:postgresql:42.7.5",
    mvn"com.zaxxer:HikariCP:6.2.1"
  )
  
  // Testing support sub-module
  object testing extends BaseModule {
    def artifactName = "iw-support-sqldb-testing"
    
    override def moduleDir = sqldb.moduleDir / "testing-support"
    override def intellijModulePath: os.Path = sqldb.moduleDir / "testing-support"
    
    override def sources = Task.Sources(moduleDir / "src" / "main" / "scala")
    override def resources = Task.Sources(moduleDir / "src" / "main" / "resources")
    
    def pomSettings = PomSettings(
      description = "IW Support SQL Database Testing Library",
      organization = "works.iterative.support",
      url = "https://github.com/iterative-works/iw-support",
      licenses = Seq(License.MIT),
      versionControl = VersionControl.github("iterative-works", "iw-support"),
      developers = Seq(
        Developer("mprihoda", "Michal Příhoda", "https://github.com/mprihoda")
      )
    )
    
    def moduleDeps = Seq(sqldb)
    
    def mvnDeps = super.mvnDeps() ++ Seq(
      IWMillDeps.zio,
      IWMillDeps.zioTest,
      IWMillDeps.logbackClassic,
      // Testcontainers dependencies
      mvn"org.testcontainers:testcontainers:1.20.4",
      mvn"org.testcontainers:postgresql:1.20.4",
      mvn"com.dimafeng::testcontainers-scala-postgresql::0.41.5"
    )
  }
}

// Email support module - JVM only
object email extends BaseModule {
  def artifactName = "iw-support-email"
  
  def pomSettings = PomSettings(
    description = "IW Support Email Library",
    organization = "works.iterative.support",
    url = "https://github.com/iterative-works/iw-support",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github("iterative-works", "iw-support"),
    developers = Seq(
      Developer("mprihoda", "Michal Příhoda", "https://github.com/mprihoda")
    )
  )
  
  def moduleDeps = Seq(core.jvm)
  
  def mvnDeps = super.mvnDeps() ++ Seq(
    IWMillDeps.zio,
    IWMillDeps.zioConfig,
    mvn"org.apache.commons:commons-email:1.5",
    // Silencer lib for cross-compilation (2.13 version for Scala 3)
    mvn"com.github.ghik::silencer-lib::1.4.2".withConfiguration("provided").withDottyCompat(scalaVersion())
  )
}

// Codecs module - cross-compiled for JVM and JS (pure cross-compilation with only shared sources)
// Note: This module is used by forms and http modules (not yet migrated)
object codecs extends Module {
  
  // Base trait for all codecs module variants  
  trait CodecsModule extends BaseModule {
    def artifactName = "iw-support-codecs"

    def pomSettings = PomSettings(
      description = "IW Support Codecs Library",
      organization = "works.iterative.support",
      url = "https://github.com/iterative-works/iw-support",
      licenses = Seq(License.MIT),
      versionControl = VersionControl.github("iterative-works", "iw-support"),
      developers = Seq(
        Developer("mprihoda", "Michal Příhoda", "https://github.com/mprihoda")
      )
    )
    
    // Pure cross-compilation - only shared sources, no platform-specific sources
    override def sources = Task.Sources("src/main/scala")
    override def resources = Task.Sources("src/main/resources")
  }
  
  // JVM-specific module
  object jvm extends CodecsModule {
    def moduleDeps = Seq(core.jvm, entity.jvm, tapir.jvm)
    
    def mvnDeps = super.mvnDeps() ++ Seq(
      IWMillDeps.zioJson
    )
    
    // Excluding scala-collection-compat conflict (similar to SBT build)
    // Note: Mill handles this automatically through Coursier resolution
  }
  
  // JavaScript-specific module
  object js extends CodecsModule with BaseScalaJSModule {
    def moduleDeps = Seq(core.js, entity.js, tapir.js)
    
    def mvnDeps = super.mvnDeps() ++ Seq(
      IWMillDeps.zioJson
    )
  }
}

// Forms core module - cross-compiled for JVM and JS
object formsCore extends Module {
  
  // Base trait for all forms-core module variants
  trait FormsCoreModule extends BaseModule {
    def artifactName = "iw-support-forms-core"
    
    // Override module directory to match SBT structure
    override def moduleDir = super.moduleDir / os.up / os.up / "forms" / "core"
    
    // Override source paths for the specific module structure
    def sharedSources = Task.Sources(moduleDir / "shared" / "src" / "main" / "scala")
    def sharedResources = Task.Sources(moduleDir / "shared" / "src" / "main" / "resources")
    
    override def sources = Task {
      super.sources() ++ sharedSources()
    }
    
    override def resources = Task {
      super.resources() ++ sharedResources()
    }
    
    def pomSettings = PomSettings(
      description = "IW Support Forms Core Library",
      organization = "works.iterative.support",
      url = "https://github.com/iterative-works/iw-support",
      licenses = Seq(License.MIT),
      versionControl = VersionControl.github("iterative-works", "iw-support"),
      developers = Seq(
        Developer("mprihoda", "Michal Příhoda", "https://github.com/mprihoda")
      )
    )
  }
  
  // JVM-specific module
  object jvm extends FormsCoreModule {
    def moduleDeps = Seq(core.jvm)
  }
  
  // JavaScript-specific module
  object js extends FormsCoreModule with BaseScalaJSModule {
    def moduleDeps = Seq(core.js)
  }
}

// Files core module - cross-compiled for JVM and JS
object filesCore extends Module {
  
  // Base trait for all files-core module variants
  trait FilesCoreModule extends BaseModule {
    def artifactName = "iw-support-files-core"
    
    // Override module directory to match SBT structure
    override def moduleDir = super.moduleDir / os.up / os.up / "files" / "core"
    
    // Override source paths for the specific module structure
    def sharedSources = Task.Sources(moduleDir / "shared" / "src" / "main" / "scala")
    def sharedResources = Task.Sources(moduleDir / "shared" / "src" / "main" / "resources")
    
    override def sources = Task {
      super.sources() ++ sharedSources()
    }
    
    override def resources = Task {
      super.resources() ++ sharedResources()
    }
    
    def pomSettings = PomSettings(
      description = "IW Support Files Core Library",
      organization = "works.iterative.support",
      url = "https://github.com/iterative-works/iw-support",
      licenses = Seq(License.MIT),
      versionControl = VersionControl.github("iterative-works", "iw-support"),
      developers = Seq(
        Developer("mprihoda", "Michal Příhoda", "https://github.com/mprihoda")
      )
    )
  }
  
  // JVM-specific module
  object jvm extends FilesCoreModule {
    def moduleDeps = Seq(core.jvm)
    
    // Override sources to look in jvm directory
    override def sources = Task.Sources(
      moduleDir / "jvm" / "src" / "main" / "scala",
      moduleDir / "shared" / "src" / "main" / "scala"
    )
    
    def mvnDeps = super.mvnDeps() ++ Seq(
      IWMillDeps.zio
    )
  }
  
  // JavaScript-specific module
  object js extends FilesCoreModule with BaseScalaJSModule {
    def moduleDeps = Seq(core.js)
    
    // Override sources to look in js directory
    override def sources = Task.Sources(
      moduleDir / "js" / "src" / "main" / "scala",
      moduleDir / "shared" / "src" / "main" / "scala"
    )
    
    def mvnDeps = super.mvnDeps() ++ Seq(
      IWMillDeps.zio,
      IWMillDeps.scalaJsDom
    )
  }
}

// UI core module - cross-compiled for JVM and JS
object uiCore extends Module {
  
  // Base trait for all ui-core module variants
  trait UICoreModule extends BaseModule {
    def artifactName = "iw-support-ui-core"
    
    // Override module directory to match SBT structure
    override def moduleDir = super.moduleDir / os.up / os.up / "ui" / "core"
    
    // Override source paths for the specific module structure
    def sharedSources = Task.Sources(moduleDir / "shared" / "src" / "main" / "scala")
    def sharedResources = Task.Sources(moduleDir / "shared" / "src" / "main" / "resources")
    
    override def sources = Task {
      super.sources() ++ sharedSources()
    }
    
    override def resources = Task {
      super.resources() ++ sharedResources()
    }
    
    def pomSettings = PomSettings(
      description = "IW Support UI Core Library",
      organization = "works.iterative.support",
      url = "https://github.com/iterative-works/iw-support",
      licenses = Seq(License.MIT),
      versionControl = VersionControl.github("iterative-works", "iw-support"),
      developers = Seq(
        Developer("mprihoda", "Michal Příhoda", "https://github.com/mprihoda")
      )
    )
  }
  
  // JVM-specific module
  object jvm extends UICoreModule {
    def moduleDeps = Seq(formsCore.jvm)
    
    // Override sources to look in jvm directory if it exists
    override def sources = Task.Sources(
      moduleDir / "jvm" / "src" / "main" / "scala",
      moduleDir / "shared" / "src" / "main" / "scala"
    )
  }
  
  // JavaScript-specific module
  object js extends UICoreModule with BaseScalaJSModule {
    def moduleDeps = Seq(formsCore.js)
    
    // Override sources to look in js directory if it exists
    override def sources = Task.Sources(
      moduleDir / "js" / "src" / "main" / "scala",
      moduleDir / "shared" / "src" / "main" / "scala"
    )
  }
}

// UI module - cross-compiled for JVM and JS
object ui extends Module {
  
  // Base trait for all ui module variants
  trait UIModule extends BaseModule with FullCrossScalaModule {
    def artifactName = "iw-support-ui"
    
    def pomSettings = PomSettings(
      description = "IW Support UI Library",
      organization = "works.iterative.support",
      url = "https://github.com/iterative-works/iw-support",
      licenses = Seq(License.MIT),
      versionControl = VersionControl.github("iterative-works", "iw-support"),
      developers = Seq(
        Developer("mprihoda", "Michal Příhoda", "https://github.com/mprihoda")
      )
    )
  }
  
  // JVM-specific module
  object jvm extends UIModule {
    def moduleDeps = Seq(core.jvm, tapir.jvm)
    
    def mvnDeps = super.mvnDeps() ++ Seq(
      IWMillDeps.zio,
      IWMillDeps.scalatags,
      // Apache POI for Excel support
      mvn"org.apache.poi:poi-ooxml:5.2.1"
    )
  }
  
  // JavaScript-specific module
  object js extends UIModule with BaseScalaJSModule {
    def moduleDeps = Seq(core.js, tapir.js)
    
    def mvnDeps = super.mvnDeps() ++ Seq(
      IWMillDeps.zio,
      IWMillDeps.zioJson,
      IWMillDeps.laminar,
      IWMillDeps.waypoint,
      IWMillDeps.urlDsl,
      // Laminext libraries
      mvn"io.laminext::core::${IWMillVersions.laminext}",
      mvn"io.laminext::ui::${IWMillVersions.laminext}",
      mvn"io.laminext::tailwind::${IWMillVersions.laminext}",
      mvn"io.laminext::validation-core::${IWMillVersions.laminext}"
    )
  }
}

// UI Forms module - cross-compiled for JVM and JS
object uiForms extends Module {
  
  // Base trait for all ui-forms module variants
  trait UIFormsModule extends BaseModule {
    def artifactName = "iw-support-ui-forms"
    
    // Override module directory to match SBT structure
    override def moduleDir = super.moduleDir / os.up / os.up / "ui" / "forms"
    
    // Override source paths for the specific module structure
    def sharedSources = Task.Sources(moduleDir / "shared" / "src" / "main" / "scala")
    def sharedResources = Task.Sources(moduleDir / "shared" / "src" / "main" / "resources")
    
    override def sources = Task {
      super.sources() ++ sharedSources()
    }
    
    override def resources = Task {
      super.resources() ++ sharedResources()
    }
    
    def pomSettings = PomSettings(
      description = "IW Support UI Forms Library",
      organization = "works.iterative.support",
      url = "https://github.com/iterative-works/iw-support",
      licenses = Seq(License.MIT),
      versionControl = VersionControl.github("iterative-works", "iw-support"),
      developers = Seq(
        Developer("mprihoda", "Michal Příhoda", "https://github.com/mprihoda")
      )
    )
  }
  
  // JVM-specific module
  object jvm extends UIFormsModule {
    def moduleDeps = Seq(ui.jvm, filesCore.jvm)
    
    // Override sources to include platform-specific sources
    override def sources = Task.Sources(
      moduleDir / "jvm" / "src" / "main" / "scala",
      moduleDir / "shared" / "src" / "main" / "scala"
    )
  }
  
  // JavaScript-specific module
  object js extends UIFormsModule with BaseScalaJSModule {
    def moduleDeps = Seq(ui.js, filesCore.js)
    
    // Override sources to include platform-specific sources
    override def sources = Task.Sources(
      moduleDir / "js" / "src" / "main" / "scala",
      moduleDir / "shared" / "src" / "main" / "scala"
    )
  }
}

// UI Scalatags module - cross-compiled for JVM and JS
object uiScalatags extends Module {
  
  // Base trait for all ui-scalatags module variants
  trait UIScalatagsModule extends BaseModule {
    def artifactName = "iw-support-ui-scalatags"
    
    // Override module directory to match SBT structure
    override def moduleDir = super.moduleDir / os.up / os.up / "ui" / "scalatags"
    
    // Override source paths for the specific module structure
    def sharedSources = Task.Sources(moduleDir / "shared" / "src" / "main" / "scala")
    def sharedResources = Task.Sources(moduleDir / "shared" / "src" / "main" / "resources")
    
    override def sources = Task {
      super.sources() ++ sharedSources()
    }
    
    override def resources = Task {
      super.resources() ++ sharedResources()
    }
    
    def pomSettings = PomSettings(
      description = "IW Support UI Scalatags Library",
      organization = "works.iterative.support",
      url = "https://github.com/iterative-works/iw-support",
      licenses = Seq(License.MIT),
      versionControl = VersionControl.github("iterative-works", "iw-support"),
      developers = Seq(
        Developer("mprihoda", "Michal Příhoda", "https://github.com/mprihoda")
      )
    )
  }
  
  // JVM-specific module
  object jvm extends UIScalatagsModule {
    def moduleDeps = Seq(uiCore.jvm)
    
    // Override sources to include platform-specific sources
    override def sources = Task.Sources(
      moduleDir / "jvm" / "src" / "main" / "scala",
      moduleDir / "shared" / "src" / "main" / "scala"
    )
    
    def mvnDeps = super.mvnDeps() ++ Seq(
      IWMillDeps.scalatags,
      IWMillDeps.zioInteropCats,
      // HTTP4s core for server-side rendering
      mvn"org.http4s::http4s-core::${IWMillVersions.http4s}"
    )
  }
  
  // JavaScript-specific module
  object js extends UIScalatagsModule with BaseScalaJSModule {
    def moduleDeps = Seq(uiCore.js)
    
    def mvnDeps = super.mvnDeps() ++ Seq(
      IWMillDeps.scalatags
    )
  }
}

// Akka Persistence support module - JVM only
object akkaPersistence extends BaseModule {
  def artifactName = "iw-support-akka-persistence"
  
  def pomSettings = PomSettings(
    description = "IW Support Akka Persistence Library",
    organization = "works.iterative.support",
    url = "https://github.com/iterative-works/iw-support",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github("iterative-works", "iw-support"),
    developers = Seq(
      Developer("mprihoda", "Michal Příhoda", "https://github.com/mprihoda")
    )
  )
  
  def moduleDeps = Seq(core.jvm, entity.jvm)
  
  def mvnDeps = super.mvnDeps() ++ Seq(
    IWMillDeps.zio,
    IWMillDeps.zioJson,
    IWMillDeps.zioConfig,
    // Akka dependencies (all using Scala 2.13 with Scala 3 compatibility)
    mvn"com.typesafe.akka::akka-persistence-typed::${IWMillVersions.akka}".withDottyCompat(scalaVersion()),
    mvn"com.typesafe.akka::akka-cluster-sharding-typed::${IWMillVersions.akka}".withDottyCompat(scalaVersion()),
    mvn"com.lightbend.akka::akka-persistence-jdbc::5.1.0".withDottyCompat(scalaVersion()),
    // Akka Projection dependencies
    mvn"com.lightbend.akka::akka-projection-core::${IWMillVersions.akkaProjection}".withDottyCompat(scalaVersion()),
    mvn"com.lightbend.akka::akka-projection-eventsourced::${IWMillVersions.akkaProjection}".withDottyCompat(scalaVersion()),
    mvn"com.lightbend.akka::akka-projection-slick::${IWMillVersions.akkaProjection}".withDottyCompat(scalaVersion()),
    mvn"com.typesafe.akka::akka-persistence-query::${IWMillVersions.akka}".withDottyCompat(scalaVersion()),
    // Slick dependencies
    mvn"com.typesafe.slick::slick::${IWMillVersions.slick}".withDottyCompat(scalaVersion()),
    mvn"com.typesafe.slick::slick-hikaricp::${IWMillVersions.slick}".withDottyCompat(scalaVersion()),
    // Silencer for cross-compilation
    mvn"com.github.ghik::silencer-lib::1.4.2".withConfiguration("provided").withDottyCompat(scalaVersion())
  )
}

// Paygate payment gateway support module - JVM only
object paygate extends BaseModule {
  def artifactName = "iw-support-paygate"
  
  def pomSettings = PomSettings(
    description = "IW Support Paygate Library",
    organization = "works.iterative.support",
    url = "https://github.com/iterative-works/iw-support",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github("iterative-works", "iw-support"),
    developers = Seq(
      Developer("mprihoda", "Michal Příhoda", "https://github.com/mprihoda")
    )
  )
  
  def moduleDeps = Seq(core.jvm, tapir.jvm)
  
  def mvnDeps = super.mvnDeps() ++ Seq(
    IWMillDeps.zio,
    IWMillDeps.zioJson,
    IWMillDeps.zioConfig,
    IWMillDeps.sttpClientZio,
    mvn"com.softwaremill.sttp.client3::zio-json::${IWMillVersions.sttpClient3}",
    // Silencer for cross-compilation
    mvn"com.github.ghik::silencer-lib::1.4.2".withConfiguration("provided").withDottyCompat(scalaVersion())
  )
}

// HashiCorp integrations module - cross-compiled for JVM and JS
object hashicorp extends Module {
  
  // Base trait for all hashicorp module variants
  trait HashicorpModule extends BaseModule with FullCrossScalaModule {
    def artifactName = "iw-support-hashicorp"
    
    def pomSettings = PomSettings(
      description = "IW Support HashiCorp Library",
      organization = "works.iterative.support",
      url = "https://github.com/iterative-works/iw-support",
      licenses = Seq(License.MIT),
      versionControl = VersionControl.github("iterative-works", "iw-support"),
      developers = Seq(
        Developer("mprihoda", "Michal Příhoda", "https://github.com/mprihoda")
      )
    )
  }
  
  // JVM-specific module
  object jvm extends HashicorpModule {
    def moduleDeps = Seq(core.jvm, serviceSpecs.jvm, tapir.jvm)
    
    def mvnDeps = super.mvnDeps() ++ Seq(
      IWMillDeps.zio
    )
  }
  
  // JavaScript-specific module
  object js extends HashicorpModule with BaseScalaJSModule {
    def moduleDeps = Seq(core.js, serviceSpecs.js, tapir.js)
    
    def mvnDeps = super.mvnDeps() ++ Seq(
      IWMillDeps.zio
    )
  }
}

// HTTP server support module - JVM only
object http extends BaseModule {
  def artifactName = "iw-support-http"
  
  // Override module directory to match SBT structure
  override def moduleDir = super.moduleDir / os.up / "server" / "http"
  
  def pomSettings = PomSettings(
    description = "IW Support HTTP Server Library",
    organization = "works.iterative.support",
    url = "https://github.com/iterative-works/iw-support",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github("iterative-works", "iw-support"),
    developers = Seq(
      Developer("mprihoda", "Michal Příhoda", "https://github.com/mprihoda")
    )
  )
  
  def moduleDeps = Seq(core.jvm, codecs.jvm, tapir.jvm)
  
  def mvnDeps = super.mvnDeps() ++ Seq(
    IWMillDeps.zio,
    IWMillDeps.zioConfig,
    mvn"dev.zio::zio-config-typesafe::${IWMillVersions.zioConfig}",
    mvn"dev.zio::zio-config-magnolia::${IWMillVersions.zioConfig}",
    mvn"dev.zio::zio-logging-slf4j::${IWMillVersions.zioLogging}",
    IWMillDeps.zioInteropCats,
    IWMillDeps.tapirCore,
    IWMillDeps.tapirZIO,
    IWMillDeps.tapirZIOJson,
    mvn"com.softwaremill.sttp.tapir::tapir-files::${IWMillVersions.tapir}",
    IWMillDeps.tapirZIOHttp4sServer,
    mvn"org.http4s::http4s-blaze-server::${IWMillVersions.http4sBlaze}",
    mvn"org.pac4j::http4s-pac4j::${IWMillVersions.http4sPac4J}",
    mvn"org.pac4j:pac4j-oidc:${IWMillVersions.pac4j}",
    IWMillDeps.scalatags,
    // Silencer for cross-compilation
    mvn"com.github.ghik::silencer-lib::1.4.2".withConfiguration("provided").withDottyCompat(scalaVersion())
  )
}

// Autocomplete module - cross-compiled for JVM and JS
object autocomplete extends Module {
  
  // Base trait for all autocomplete module variants
  trait AutocompleteModule extends BaseModule with FullCrossScalaModule {
    def artifactName = "iw-support-autocomplete"
    
    def pomSettings = PomSettings(
      description = "IW Support Autocomplete Library",
      organization = "works.iterative.support",
      url = "https://github.com/iterative-works/iw-support",
      licenses = Seq(License.MIT),
      versionControl = VersionControl.github("iterative-works", "iw-support"),
      developers = Seq(
        Developer("mprihoda", "Michal Příhoda", "https://github.com/mprihoda")
      )
    )
  }
  
  // JVM-specific module
  object jvm extends AutocompleteModule {
    def moduleDeps = Seq(core.jvm, tapir.jvm, ui.jvm, uiForms.jvm)
    
    def mvnDeps = super.mvnDeps() ++ Seq(
      IWMillDeps.zio,
      IWMillDeps.quill
    )
  }
  
  // JavaScript-specific module
  object js extends AutocompleteModule with BaseScalaJSModule {
    def moduleDeps = Seq(core.js, tapir.js, ui.js, uiForms.js, filesUI)
    
    def mvnDeps = super.mvnDeps() ++ Seq(
      IWMillDeps.zio
    )
  }
}

// Files REST adapter module - cross-compiled for JVM and JS
object filesRest extends Module {
  
  // Base trait for all files-rest module variants
  trait FilesRestModule extends BaseModule with FullCrossScalaModule {
    def artifactName = "iw-support-files-rest"
    
    // Override module directory to match SBT structure
    override def moduleDir = super.moduleDir / os.up / os.up / "files" / "adapters" / "rest"
    
    // Override source paths for the specific module structure
    def sharedSources = Task.Sources(moduleDir / "shared" / "src" / "main" / "scala")
    def sharedResources = Task.Sources(moduleDir / "shared" / "src" / "main" / "resources")
    
    override def sources = Task {
      super.sources() ++ sharedSources()
    }
    
    override def resources = Task {
      super.resources() ++ sharedResources()
    }
    
    def pomSettings = PomSettings(
      description = "IW Support Files REST Library",
      organization = "works.iterative.support",
      url = "https://github.com/iterative-works/iw-support",
      licenses = Seq(License.MIT),
      versionControl = VersionControl.github("iterative-works", "iw-support"),
      developers = Seq(
        Developer("mprihoda", "Michal Příhoda", "https://github.com/mprihoda")
      )
    )
  }
  
  // JVM-specific module
  object jvm extends FilesRestModule {
    def moduleDeps = Seq(filesCore.jvm, tapir.jvm)
    
    // Override sources to look in jvm directory
    override def sources = Task.Sources(
      moduleDir / "jvm" / "src" / "main" / "scala",
      moduleDir / "shared" / "src" / "main" / "scala"
    )
  }
  
  // JavaScript-specific module
  object js extends FilesRestModule with BaseScalaJSModule {
    def moduleDeps = Seq(filesCore.js, tapir.js)
    
    // Override sources to look in js directory
    override def sources = Task.Sources(
      moduleDir / "js" / "src" / "main" / "scala",
      moduleDir / "shared" / "src" / "main" / "scala"
    )
  }
}

// Files Mongo adapter module - JVM only
object filesMongo extends BaseModule {
  def artifactName = "iw-support-files-mongo"
  
  // Override module directory to match SBT structure
  override def moduleDir = super.moduleDir / os.up / "files" / "adapters" / "mongo"
  
  def pomSettings = PomSettings(
    description = "IW Support Files MongoDB Library",
    organization = "works.iterative.support",
    url = "https://github.com/iterative-works/iw-support",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github("iterative-works", "iw-support"),
    developers = Seq(
      Developer("mprihoda", "Michal Příhoda", "https://github.com/mprihoda")
    )
  )
  
  def moduleDeps = Seq(filesCore.jvm, mongo)
  
  // Integration test module
  object it extends BaseTests {
    override def moduleDir = filesMongo.moduleDir / "it"
    override def intellijModulePath: os.Path = filesMongo.moduleDir / "it" / "src/test"
    
    override def sources = Task.Sources(filesMongo.moduleDir / "it" / "src" / "test" / "scala")
    override def resources = Task.Sources(filesMongo.moduleDir / "it" / "src" / "test" / "resources")
    
    def moduleDeps = Seq(filesMongo)
    
    def mvnDeps = super.mvnDeps() ++ Seq(
      IWMillDeps.zio,
      IWMillDeps.zioConfig
    )
  }
}

// Files UI adapter module - JS only
object filesUI extends BaseScalaJSModule {
  def artifactName = "iw-support-files-ui"
  
  // Override module directory to match SBT structure
  override def moduleDir = super.moduleDir / os.up / "files" / "adapters" / "ui"
  
  def pomSettings = PomSettings(
    description = "IW Support Files UI Library",
    organization = "works.iterative.support",
    url = "https://github.com/iterative-works/iw-support",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github("iterative-works", "iw-support"),
    developers = Seq(
      Developer("mprihoda", "Michal Příhoda", "https://github.com/mprihoda")
    )
  )
  
  def moduleDeps = Seq(filesCore.js, ui.js)
}

// Files integration tests module - JVM only
object filesIT extends BaseModule {
  def artifactName = "iw-support-files-it"
  
  // Override module directory to match SBT structure
  override def moduleDir = super.moduleDir / os.up / "files" / "it"
  
  def pomSettings = PomSettings(
    description = "IW Support Files Integration Tests",
    organization = "works.iterative.support",
    url = "https://github.com/iterative-works/iw-support",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github("iterative-works", "iw-support"),
    developers = Seq(
      Developer("mprihoda", "Michal Příhoda", "https://github.com/mprihoda")
    )
  )
  
  def moduleDeps = Seq(filesRest.jvm, http)
}

// Scenarios module - cross-compiled for JVM and JS
object scenarios extends Module {
  
  // Base trait for all scenarios module variants
  trait ScenariosModule extends BaseModule with FullCrossScalaModule {
    def artifactName = "iw-support-scenarios"
    
    def pomSettings = PomSettings(
      description = "IW Support Scenarios Library",
      organization = "works.iterative.support",
      url = "https://github.com/iterative-works/iw-support",
      licenses = Seq(License.MIT),
      versionControl = VersionControl.github("iterative-works", "iw-support"),
      developers = Seq(
        Developer("mprihoda", "Michal Příhoda", "https://github.com/mprihoda")
      )
    )
  }
  
  // JVM-specific module
  object jvm extends ScenariosModule {
    def moduleDeps = Seq(core.jvm)
    
    def mvnDeps = super.mvnDeps() ++ Seq(
      IWMillDeps.zio,
      mvn"dev.zio::zio-http::${IWMillVersions.zioHttp}"
    )
  }
  
  // JavaScript-specific module
  object js extends ScenariosModule with BaseScalaJSModule {
    def moduleDeps = Seq(core.js)
    
    def mvnDeps = super.mvnDeps() ++ Seq(
      IWMillDeps.zio
    )
  }
}

// Forms module - cross-compiled for JVM and JS
object forms extends Module {
  
  // Base trait for all forms module variants
  trait FormsModule extends BaseModule with FullCrossScalaModule {
    def artifactName = "iw-support-forms"
    
    def pomSettings = PomSettings(
      description = "IW Support Forms Library",
      organization = "works.iterative.support",
      url = "https://github.com/iterative-works/iw-support",
      licenses = Seq(License.MIT),
      versionControl = VersionControl.github("iterative-works", "iw-support"),
      developers = Seq(
        Developer("mprihoda", "Michal Příhoda", "https://github.com/mprihoda")
      )
    )
  }
  
  // JVM-specific module
  object jvm extends FormsModule {
    def moduleDeps = Seq(core.jvm, codecs.jvm, autocomplete.jvm, filesRest.jvm, email, paygate, filesMongo)
    
    def mvnDeps = super.mvnDeps() ++ Seq(
      IWMillDeps.zio,
      IWMillDeps.zioConfigTypesafe,
      mvn"org.scala-lang.modules::scala-xml::2.2.0",
      mvn"org.apache.xmlgraphics:fop:2.9",
      mvn"io.github.arainko::ducktape::0.1.11"
    )
  }
  
  // JavaScript-specific module
  object js extends FormsModule with BaseScalaJSModule {
    def moduleDeps = Seq(core.js, codecs.js, autocomplete.js, filesRest.js)
    
    def mvnDeps = super.mvnDeps() ++ Seq(
      IWMillDeps.zio
    )
  }
}

// Forms HTTP module - JVM only
object formsHttp extends BaseModule {
  def artifactName = "iw-support-forms-http"
  
  // Override module directory to match SBT structure
  override def moduleDir = super.moduleDir / os.up / "forms" / "http"
  
  def pomSettings = PomSettings(
    description = "IW Support Forms HTTP Library",
    organization = "works.iterative.support",
    url = "https://github.com/iterative-works/iw-support",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github("iterative-works", "iw-support"),
    developers = Seq(
      Developer("mprihoda", "Michal Příhoda", "https://github.com/mprihoda")
    )
  )
  
  def moduleDeps = Seq(formsCore.jvm, http)
}

// Forms scenarios module - cross-compiled for JVM and JS (test scenarios)
object formsScenarios extends Module {
  
  // Base trait for all forms-scenarios module variants
  trait FormsScenariosModule extends BaseModule with FullCrossScalaModule {
    def artifactName = "iw-support-forms-scenarios"
    
    // Override module directory to match SBT structure
    override def moduleDir = super.moduleDir / os.up / os.up / "forms" / "scenarios"
    
    // Skip publishing for test scenarios
    override def publishVersion = "0.0.0"
    
    def pomSettings = PomSettings(
      description = "IW Support Forms Scenarios",
      organization = "works.iterative.support",
      url = "https://github.com/iterative-works/iw-support",
      licenses = Seq(License.MIT),
      versionControl = VersionControl.github("iterative-works", "iw-support"),
      developers = Seq(
        Developer("mprihoda", "Michal Příhoda", "https://github.com/mprihoda")
      )
    )
    
    // TODO: Add BuildInfo support when available in Mill
  }
  
  // JVM-specific module
  object jvm extends FormsScenariosModule {
    def moduleDeps = Seq(forms.jvm, scenarios.jvm)
  }
  
  // JavaScript-specific module
  object js extends FormsScenariosModule with BaseScalaJSModule {
    def moduleDeps = Seq(forms.js, scenarios.js)
  }
}

// Files UI scenarios module - cross-compiled for JVM and JS (test scenarios)
object filesUIScenarios extends Module {
  
  // Base trait for all files-ui-scenarios module variants
  trait FilesUIScenariosModule extends BaseModule with FullCrossScalaModule {
    def artifactName = "iw-support-files-ui-scenarios"
    
    // Override module directory to match SBT structure
    override def moduleDir = super.moduleDir / os.up / os.up / "files" / "adapters" / "ui" / "scenarios"
    
    // Skip publishing for test scenarios
    override def publishVersion = "0.0.0"
    
    def pomSettings = PomSettings(
      description = "IW Support Files UI Scenarios",
      organization = "works.iterative.support",
      url = "https://github.com/iterative-works/iw-support",
      licenses = Seq(License.MIT),
      versionControl = VersionControl.github("iterative-works", "iw-support"),
      developers = Seq(
        Developer("mprihoda", "Michal Příhoda", "https://github.com/mprihoda")
      )
    )
  }
  
  // JVM-specific module
  object jvm extends FilesUIScenariosModule {
    def moduleDeps = Seq(filesCore.jvm, filesRest.jvm, ui.jvm, scenarios.jvm)
    
    def mvnDeps = super.mvnDeps() ++ Seq(
      mvn"dev.zio::zio-http-htmx::3.0.0-RC6"
    )
  }
  
  // JavaScript-specific module
  object js extends FilesUIScenariosModule with BaseScalaJSModule {
    def moduleDeps = Seq(filesCore.js, filesRest.js, ui.js, scenarios.js, filesUI)
  }
}

// Scenarios UI module - JS only with Vite support
object scenariosUI extends BaseScalaJSModule {
  def artifactName = "iw-support-scenarios-ui"
  
  // Override module directory to match SBT structure
  override def moduleDir = super.moduleDir / os.up / "ui" / "scenarios"
  
  // Skip publishing for internal scenarios
  override def publishVersion = "0.0.0"
  
  def pomSettings = PomSettings(
    description = "IW Support Scenarios UI",
    organization = "works.iterative.support",
    url = "https://github.com/iterative-works/iw-support",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github("iterative-works", "iw-support"),
    developers = Seq(
      Developer("mprihoda", "Michal Příhoda", "https://github.com/mprihoda")
    )
  )
  
  def moduleDeps = Seq(ui.js, uiForms.js)
  
  // Enable main module initializer
  def scalaJSUseMainModuleInitializer = true
  
  override def moduleKind = Task {
    ModuleKind.ESModule
  }

  override def moduleSplitStyle = Task {
    ModuleSplitStyle.SmallModulesFor(List("works.iterative"))
  }
  
  // Add custom scalac option for source map URI
  override def scalacOptions = Task {
    super.scalacOptions() ++ Seq(
      s"-scalajs-mapSourceURI:${moduleDir.toNIO.toUri.toString}->/mdr/poptavky/@fs${moduleDir.toString}/"
    )
  }
  
  // Custom tasks for Vite integration
  def viteConfig = Task.Source(moduleDir / "vite.config.js")
  def packageJson = Task.Source(moduleDir / "package.json")
  
  // Install npm dependencies
  def npmInstall() = Task.Command {
    os.proc("yarn", "install").call(cwd = moduleDir)
  }
  
  // PID file for tracking Vite dev server
  def viteDevPidFile = Task { Task.dest / "vite.pid" }
  
  // Check if process is running
  def isProcessRunning(pid: String): Boolean = {
    try {
      os.proc("ps", "-p", pid).call(check = false).exitCode == 0
    } catch {
      case _: Exception => false
    }
  }
  
  // Run Vite dev server in background with PID management
  def viteDev() = Task.Command {
    npmInstall()
    fastLinkJS()
    
    val pidFile = viteDevPidFile()
    val pidOpt = if (os.exists(pidFile)) {
      try Some(os.read(pidFile).trim) catch {
        case _: Exception => None
      }
    } else None
    
    pidOpt match {
      case Some(pid) if isProcessRunning(pid) =>
        println(s"✅ Vite dev server already running with PID $pid")
        println(s"   Access the server at http://localhost:5173")
        println(s"   ScalaJS output: ${fastLinkJS().dest.path}")
      case _ =>
        // Kill old process if PID exists but process is dead
        pidOpt.foreach { pid =>
          println(s"⚠️  Previous Vite process (PID $pid) is not running, cleaning up...")
          try { os.proc("kill", "-9", pid).call(check = false) } catch { case _: Exception => }
        }
        
        println("🚀 Starting Vite development server...")
        println(s"   ScalaJS output: ${fastLinkJS().dest.path}")
        
        // Start the server in background
        val process = os.proc("yarn", "run", "dev")
          .spawn(cwd = moduleDir, stdout = os.Inherit, stderr = os.Inherit)
        
        // Get the PID using ProcessHandle
        val pid = process.wrapped.pid().toString
        os.write.over(pidFile, pid)
        
        println(s"✅ Vite dev server started with PID $pid")
        println(s"   Access the server at http://localhost:5173")
        println(s"   To stop: mill scenariosUI.viteStop")
    }
    ()  // Explicitly return Unit
  }
  
  // Stop the Vite dev server
  def viteStop() = Task.Command {
    val pidFile = viteDevPidFile()
    if (os.exists(pidFile)) {
      val pid = os.read(pidFile).trim
      if (isProcessRunning(pid)) {
        println(s"🛑 Stopping Vite dev server (PID $pid)...")
        os.proc("kill", pid).call(check = false)
        os.remove(pidFile)
        println("✅ Vite dev server stopped")
      } else {
        println("⚠️  Vite dev server is not running")
        os.remove(pidFile)
      }
    } else {
      println("⚠️  No Vite dev server PID file found")
    }
    ()  // Explicitly return Unit
  }
  
  // Build with Vite (depends on fullLinkJS for production)
  def viteBuild() = Task.Command {
    fullLinkJS()
    println(s"Building with Vite...")
    os.proc("yarn", "run", "build", "--outDir", Task.dest).call(cwd = moduleDir)
  }
}

// Root aggregate module - equivalent to SBT root project
object root extends BaseModule {
  def artifactName = "iw-support"
  
  // Skip publishing for root aggregate
  override def publishVersion = "0.0.0"
  
  def pomSettings = PomSettings(
    description = "IW Support Root Aggregate",
    organization = "works.iterative.support",
    url = "https://github.com/iterative-works/iw-support",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github("iterative-works", "iw-support"),
    developers = Seq(
      Developer("mprihoda", "Michal Příhoda", "https://github.com/mprihoda")
    )
  )
  
  // Aggregate all modules - this compiles all modules when root is compiled
  def moduleDeps = Seq(
    // Core modules
    core.jvm, core.js,
    entity.jvm, entity.js,
    serviceSpecs.jvm, serviceSpecs.js,
    tapir.jvm, tapir.js,
    
    // Storage modules
    mongo, sqldb, sqldb.testing,
    
    // Communication modules
    email, paygate,
    
    // Support modules
    codecs.jvm, codecs.js,
    hashicorp.jvm, hashicorp.js,
    akkaPersistence,
    
    // File handling
    filesCore.jvm, filesCore.js,
    filesRest.jvm, filesRest.js,
    filesMongo, filesUI, filesIT,
    
    // UI modules
    uiCore.jvm, uiCore.js,
    ui.jvm, ui.js,
    uiForms.jvm, uiForms.js,
    uiScalatags.jvm, uiScalatags.js,
    
    // Server modules
    http,
    
    // Application modules
    autocomplete.jvm, autocomplete.js,
    
    // Forms modules
    formsCore.jvm, formsCore.js,
    forms.jvm, forms.js,
    formsHttp,
    
    // Scenarios and testing
    scenarios.jvm, scenarios.js,
    formsScenarios.jvm, formsScenarios.js,
    filesUIScenarios.jvm, filesUIScenarios.js,
    scenariosUI
  )
}

// Convenience commands for testing the migration
object verify extends Module {
  // Compile all modules via root aggregate
  def compile() = Task.Command {
    root.compile()
    println("✅ All modules compiled successfully!")
  }

  // Run all tests
  def test() = Task.Command {
    core.jvm.test.testForked()
    core.js.test.testForked()
    tapir.jvm.test.testForked()
    tapir.js.test.testForked()
    println("✅ All tests passed!")
  }

  // Format check
  def checkFormat() = Task.Command {
    core.jvm.checkFormat()
    core.js.checkFormat()
    entity.jvm.checkFormat()
    entity.js.checkFormat()
    serviceSpecs.jvm.checkFormat()
    serviceSpecs.js.checkFormat()
    tapir.jvm.checkFormat()
    tapir.js.checkFormat()
    mongo.checkFormat()
    sqldb.checkFormat()
    sqldb.testing.checkFormat()
    email.checkFormat()
    codecs.jvm.checkFormat()
    codecs.js.checkFormat()
    formsCore.jvm.checkFormat()
    formsCore.js.checkFormat()
    filesCore.jvm.checkFormat()
    filesCore.js.checkFormat()
    uiCore.jvm.checkFormat()
    uiCore.js.checkFormat()
    ui.jvm.checkFormat()
    ui.js.checkFormat()
    uiForms.jvm.checkFormat()
    uiForms.js.checkFormat()
    uiScalatags.jvm.checkFormat()
    uiScalatags.js.checkFormat()
    akkaPersistence.checkFormat()
    paygate.checkFormat()
    hashicorp.jvm.checkFormat()
    hashicorp.js.checkFormat()
    http.checkFormat()
    autocomplete.jvm.checkFormat()
    autocomplete.js.checkFormat()
    filesRest.jvm.checkFormat()
    filesRest.js.checkFormat()
    filesMongo.checkFormat()
    filesUI.checkFormat()
    filesIT.checkFormat()
    scenarios.jvm.checkFormat()
    scenarios.js.checkFormat()
    forms.jvm.checkFormat()
    forms.js.checkFormat()
    formsHttp.checkFormat()
    formsScenarios.jvm.checkFormat()
    formsScenarios.js.checkFormat()
    filesUIScenarios.jvm.checkFormat()
    filesUIScenarios.js.checkFormat()
    scenariosUI.checkFormat()
    println("✅ Code formatting is correct!")
  }
}
