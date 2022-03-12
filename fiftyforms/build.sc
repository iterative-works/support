import mill._, scalalib._, scalajslib._

import $file.bom

object support {
  val Deps = bom.IWMaterials.Deps
  val Versions = bom.IWMaterials.Versions

  trait CommonModule extends ScalaModule {
    def scalaVersion = "3.1.1"
    def scalacOptions = Seq(
      "-encoding",
      "utf8",
      "-deprecation",
      "-explain-types",
      "-explain",
      "-feature",
      "-language:experimental.macros",
      "-language:higherKinds",
      "-language:implicitConversions",
      "-unchecked",
      "-Xfatal-warnings",
      "-Ykind-projector"
    )
  }

  trait CommonJSModule extends CommonModule with ScalaJSModule {
    def scalaJSVersion = "1.8.0"
  }

  trait CrossPlatformModule extends Module { outer =>
    def ivyDeps: T[Agg[Dep]] = Agg[Dep]()
    def jsDeps: T[Agg[Dep]] = Agg[Dep]()
    def jvmDeps: T[Agg[Dep]] = Agg[Dep]()
    def moduleDeps: Seq[Module] = Seq[Module]()

    trait PlatformModule extends CommonModule {
      def platform: String
      def millSourcePath = outer.millSourcePath
      def ivyDeps = outer.ivyDeps() ++ (platform match {
        case "js" => jsDeps()
        case _    => jvmDeps()
      })
      def moduleDeps = outer.moduleDeps.collect {
        case m: CrossPlatformModule => m.platformModule(platform)
        case m: JavaModule          => m
      }
    }

    trait JsModule extends PlatformModule with CommonJSModule {
      def platform = "js"
    }

    trait JvmModule extends PlatformModule {
      def platform = "jvm"
    }

    def platformModule(platform: String): JavaModule
  }

  trait PureCrossModule extends CrossPlatformModule {
    object js extends JsModule
    object jvm extends JvmModule

    def platformModule(platform: String): JavaModule = platform match {
      case "js" => js
      case _    => jvm
    }
  }

  trait PureCrossSbtModule extends CrossPlatformModule {
    object js extends JsModule with SbtModule
    object jvm extends JvmModule with SbtModule

    def platformModule(platform: String): JavaModule = platform match {
      case "js" => js
      case _    => jvm
    }
  }

  trait FullCrossSbtModule extends CrossPlatformModule {
    trait FullSources extends PlatformModule {
      def sources = T.sources(
        millSourcePath / platform / "src" / "main" / "scala",
        millSourcePath / "shared" / "src" / "main" / "scala"
      )

      def resources = T.sources(
        millSourcePath / platform / "src" / "main" / "resources",
        millSourcePath / "shared" / "src" / "main" / "resources"
      )
    }

    object js extends JsModule with FullSources {
      def platform = "js"
    }
    object jvm extends JvmModule with FullSources {
      def platform = "jvm"
    }

    def platformModule(platform: String): JavaModule = platform match {
      case "js" => js
      case _    => jvm
    }
  }

}

import support._

object mongo extends CommonModule {
  def ivyDeps = Agg(
    Deps.zio,
    Deps.zioJson,
    Deps.zioConfig,
    ivy"org.mongodb.scala::mongo-scala-driver:4.2.3".withDottyCompat(
      scalaVersion()
    )
  )
}

object tapir extends FullCrossSbtModule {
  def ivyDeps = Agg(Deps.tapirCore, Deps.tapirZIOJson, Deps.zioJson)
  def jvmDeps = Agg(Deps.tapirZIO, Deps.tapirZIOHttp4sServer)
  def jsDeps = Agg(Deps.tapirSttpClient)
}

object akkaPersistence extends CommonModule {
  def millSourcePath = build.millSourcePath / "akka-persistence"
  def ivyDeps = (Agg(
    Deps.akka.persistenceQuery,
    Deps.akka.persistenceJdbc,
    Deps.akka.projection.core,
    Deps.akka.projection.eventsourced,
    Deps.akka.projection.slick
  ) ++ Deps.slick.default).map(_.withDottyCompat(scalaVersion())) ++ Agg(
    Deps.zio,
    Deps.zioJson,
    Deps.zioConfig,
    Deps.akka.persistence.withDottyCompat(scalaVersion()),
    ivy"com.typesafe.akka::akka-cluster-sharding-typed:${Deps.akka.V}"
      .withDottyCompat(scalaVersion())
  )
}

object ui extends CommonJSModule {
  def ivyDeps = Agg(
    Deps.zio,
    Deps.laminar,
    Deps.zioJson,
    Deps.waypoint,
    Deps.urlDsl,
    Deps.laminextCore,
    Deps.laminextUI,
    Deps.laminextTailwind,
    Deps.laminextValidationCore
  )
}
