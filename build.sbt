import org.scalajs.linker.interface.ModuleSplitStyle
import scala.sys.process._
import com.typesafe.sbt.packager.docker._
import NativePackagerHelper._
import sbtcrossproject.CrossPlugin.autoImport.{CrossType, crossProject}

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := scala3Version

lazy val core = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("core"))

lazy val ui = (project in file("ui"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    IWDeps.useZIO(Test),
    IWDeps.laminar,
    IWDeps.zioJson,
    IWDeps.waypoint,
    IWDeps.urlDsl,
    IWDeps.laminextCore,
    IWDeps.laminextUI,
    IWDeps.laminextTailwind,
    IWDeps.laminextValidationCore
  )

lazy val app = (project in file("app"))
  .enablePlugins(ScalaJSPlugin, VitePlugin, MockDataExport)
  .settings(
    IWDeps.useZIO(Test),
    IWDeps.laminar,
    IWDeps.zioJson,
    IWDeps.waypoint,
    IWDeps.urlDsl,
    IWDeps.laminextCore,
    IWDeps.laminextUI,
    IWDeps.laminextTailwind,
    IWDeps.laminextValidationCore
  )
  .settings(
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.ESModule) },
    scalaJSLinkerConfig ~= {
      _.withModuleSplitStyle(ModuleSplitStyle.FewestModules)
    },
    scalaJSLinkerConfig ~= { _.withSourceMap(false) },
    scalaJSUseMainModuleInitializer := true
  )
  .dependsOn(core.js, ui)

lazy val server = (project in file("server"))
  .enablePlugins(DockerPlugin, JavaServerAppPackaging)
  .settings(
    IWDeps.useZIO(),
    IWDeps.zioConfig,
    IWDeps.zioConfigTypesafe,
    IWDeps.zioConfigMagnolia,
    IWDeps.zioLoggingSlf4j,
    IWDeps.zioInteropCats,
    IWDeps.tapirCore,
    IWDeps.tapirZIO,
    IWDeps.tapirZIOHttp4sServer,
    IWDeps.http4sBlazeServer,
    IWDeps.logbackClassic,
    IWDeps.http4sPac4J,
    IWDeps.pac4jOIDC,
    Docker / mappings ++= directory((app / viteBuild).value).map {
      case (f, p) => f -> s"/opt/docker/${p}"
    },
    dockerBaseImage := "openjdk:11",
    dockerRepository := Some("docker.e-bs.cz"),
    dockerExposedPorts := Seq(8080),
    Docker / packageName := "mdr-pdb-frontend-server",
    dockerEnvVars := Map(
      "BLAZE_HOST" -> "0.0.0.0",
      "BLAZE_PORT" -> "8080",
      "APP_PATH" -> "/opt/docker/vite"
    ),
    reStart / envVars := Map("APP_PATH" -> "../app/target/vite")
  )

lazy val root = (project in file("."))
  .settings(name := "mdr-personnel-db", publish / skip := true)
  // Auto activates for all projects, but make sure we have required dependencies
  .enablePlugins(IWScalaProjectPlugin)
  .aggregate(app, server)
