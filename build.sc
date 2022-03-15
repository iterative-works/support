import mill._, scalalib._, scalajslib._

import $file.bom

object support {
  val Deps = bom.IWMaterials.Deps
  val Versions = bom.IWMaterials.Versions

  trait CommonModule extends ScalaModule {
    def scalaVersion = "3.1.1"
    def scalacOptions = T {
      super.scalacOptions() ++ Seq(
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
  }

  trait CommonJSModule extends CommonModule with ScalaJSModule {
    def scalaJSVersion = "1.9.0"
  }

  trait CrossPlatformModule extends Module { outer =>
    def ivyDeps: T[Agg[Dep]] = Agg[Dep]()
    def jsDeps: T[Agg[Dep]] = Agg[Dep]()
    def jvmDeps: T[Agg[Dep]] = Agg[Dep]()
    def moduleDeps: Seq[Module] = Seq[Module]()

    trait PlatformModule extends CommonModule {
      def platform: String
      override def millSourcePath = outer.millSourcePath
      override def ivyDeps = outer.ivyDeps() ++ (platform match {
        case "js" => jsDeps()
        case _    => jvmDeps()
      })
      override def moduleDeps = outer.moduleDeps.collect {
        case m: CrossPlatformModule =>
          platform match {
            case "js" => m.js
            case _    => m.jvm
          }
        case m: JavaModule => m
      }
    }

    trait JsModule extends PlatformModule with CommonJSModule {
      def platform = "js"
    }

    trait JvmModule extends PlatformModule {
      def platform = "jvm"
    }

    val js: JsModule
    val jvm: JvmModule
  }

  trait PureCrossModule extends CrossPlatformModule {
    override object js extends JsModule
    override object jvm extends JvmModule
  }

  trait PureCrossSbtModule extends CrossPlatformModule {
    override object js extends JsModule with SbtModule
    override object jvm extends JvmModule with SbtModule
  }

  trait FullCrossSbtModule extends CrossPlatformModule {
    trait FullSources extends JavaModule { self: PlatformModule =>
      override def sources = T.sources(
        millSourcePath / platform / "src" / "main" / "scala",
        millSourcePath / "shared" / "src" / "main" / "scala"
      )

      override def resources = T.sources(
        millSourcePath / platform / "src" / "main" / "resources",
        millSourcePath / "shared" / "src" / "main" / "resources"
      )
    }

    override object js extends JsModule with FullSources
    override object jvm extends JvmModule with FullSources
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
