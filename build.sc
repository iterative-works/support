import mill._
import mill.scalalib._
import mill.scalajslib._
import mill.scalalib.publish._
import mill.scalalib.scalafmt._

// Import IW Mill Support library
// Note: Update this to the correct published version once available
// For now, you may need to publish mill-iw-support locally first
import $ivy.`works.iterative::mill-iw-support:0.1.0-SNAPSHOT`
import works.iterative.mill._

// BOM module for centralized dependency management
object bom extends IWBomModule {
  def publishVersion = "0.1.10-SNAPSHOT"
  
  def pomSettings = PomSettings(
    description = "IW Support Library - Bill of Material",
    organization = "works.iterative.support",
    url = "https://github.com/iterative-works/iw-support",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github("iterative-works", "iw-support"),
    developers = Seq(
      Developer("mprihoda", "Michal Příhoda", "https://github.com/mprihoda")
    )
  )
}

// Core module - cross-compiled for JVM and JS
object core extends Module {
  
  // Base trait for all core module variants
  trait CoreModule extends IWScalaModule with IWPublishModule {
    def bomModuleDeps = Seq(bom)
    def publishVersion = "0.1.10-SNAPSHOT"
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
    
    // Use SBT directory structure
  }
  
  // Shared module for platform-independent code
  object shared extends CoreModule {
    def sources = T.sources {
      super.sources() ++ Seq(
        PathRef(os.pwd / "core" / "shared" / "src" / "main" / "scala")
      )
    }
    
    def resources = T.sources {
      super.resources() ++ Seq(
        PathRef(os.pwd / "core" / "shared" / "src" / "main" / "resources")
      )
    }
    
    // Core module has minimal dependencies - mostly pure Scala
    def ivyDeps = super.ivyDeps() ++ Agg(
      ivy"dev.zio::zio",  // Core ZIO dependency for shared code
      ivy"dev.zio::zio-json",  // JSON support
      ivy"dev.zio::zio-prelude"  // ZIO Prelude for Validation
    )
  }
  
  // JVM-specific module
  object jvm extends CoreModule {
    def moduleDeps = Seq(shared)
    
    def sources = T.sources {
      super.sources() ++ Seq(
        PathRef(os.pwd / "core" / "jvm" / "src" / "main" / "scala")
      )
    }
    
    def resources = T.sources {
      super.resources() ++ Seq(
        PathRef(os.pwd / "core" / "jvm" / "src" / "main" / "resources")
      )
    }
    
    def ivyDeps = super.ivyDeps() ++ Agg(
      ivy"dev.zio::zio",  // ZIO for JVM
      ivy"dev.zio::zio-json"  // JSON support
    )
    
    // Test module for JVM
    object test extends IWTests with TestModule.ZioTest {
      def bomModuleDeps = Seq(bom)
      
      def sources = T.sources {
        Seq(
          PathRef(os.pwd / "core" / "jvm" / "src" / "test" / "scala")
        )
      }
      
      def resources = T.sources {
        Seq(
          PathRef(os.pwd / "core" / "jvm" / "src" / "test" / "resources")
        )
      }
      
      def ivyDeps = super.ivyDeps() ++ Agg(
        ivy"dev.zio::zio-test",
        ivy"dev.zio::zio-test-sbt"
      )
    }
  }
  
  // JavaScript-specific module
  object js extends CoreModule with ScalaJSModule {
    def scalaJSVersion = "1.16.0"
    def moduleDeps = Seq(shared)
    
    def sources = T.sources {
      super.sources() ++ Seq(
        PathRef(os.pwd / "core" / "js" / "src" / "main" / "scala")
      )
    }
    
    def resources = T.sources {
      super.resources() ++ Seq(
        PathRef(os.pwd / "core" / "js" / "src" / "main" / "resources")
      )
    }
    
    def ivyDeps = super.ivyDeps() ++ Agg(
      ivy"dev.zio::zio",  // ZIO for JS
      ivy"dev.zio::zio-json",  // JSON support
      ivy"org.scala-js::scalajs-dom::2.8.0"  // DOM API for ScalaJS
    )
    
    // Test module for JS
    object test extends IWTests with TestModule.ZioTest with ScalaJSModule {
      def bomModuleDeps = Seq(bom)
      def scalaJSVersion = "1.16.0"
      
      def sources = T.sources {
        Seq(
          PathRef(os.pwd / "core" / "js" / "src" / "test" / "scala")
        )
      }
      
      def resources = T.sources {
        Seq(
          PathRef(os.pwd / "core" / "js" / "src" / "test" / "resources")
        )
      }
      
      def ivyDeps = super.ivyDeps() ++ Agg(
        ivy"dev.zio::zio-test",
        ivy"dev.zio::zio-test-sbt"
      )
    }
  }
}

// Convenience commands for testing the migration
object verify extends Module {
  // Compile all core modules
  def compile() = T.command {
    core.shared.compile()
    core.jvm.compile()
    core.js.compile()
    println("✅ All core modules compiled successfully!")
  }
  
  // Run all tests
  def test() = T.command {
    core.jvm.test.test()
    core.js.test.test()
    println("✅ All tests passed!")
  }
  
  // Format check
  def checkFormat() = T.command {
    core.shared.checkFormat()
    core.jvm.checkFormat()
    core.js.checkFormat()
    println("✅ Code formatting is correct!")
  }
}
