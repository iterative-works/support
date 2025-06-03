//| mvnDeps:
//| - works.iterative::mill-iw-support::0.1.0-SNAPSHOT
import mill._
import mill.scalalib._
import mill.scalajslib._
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

// Convenience commands for testing the migration
object verify extends Module {
  // Compile all modules
  def compile() = Task.Command {
    core.jvm.compile()
    core.js.compile()
    entity.jvm.compile()
    entity.js.compile()
    println("✅ All modules compiled successfully!")
  }

  // Run all tests
  def test() = Task.Command {
    core.jvm.test.testForked()
    core.js.test.testForked()
    println("✅ All tests passed!")
  }

  // Format check
  def checkFormat() = Task.Command {
    core.jvm.checkFormat()
    core.js.checkFormat()
    println("✅ Code formatting is correct!")
  }
}
