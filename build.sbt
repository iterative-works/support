import org.scalajs.linker.interface.ModuleSplitStyle
import scala.sys.process._
import sbt.nio.file.FileTreeView
import com.typesafe.sbt.packager.docker._
import NativePackagerHelper._
import sbtcrossproject.CrossPlugin.autoImport.{CrossType, crossProject}

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := scala3Version

// TODO: integrate vite build and Docker publishing
// Taken from mdr-app, moving to plugin would be nice
lazy val viteBuild = taskKey[File]("Vite build")
lazy val viteMonitoredFiles =
  taskKey[Seq[File]]("Files monitored for vite build")
lazy val viteDist = settingKey[File]("Vite dist directory")
lazy val caddyFile = settingKey[File]("Caddyfile for caddy docker image")

lazy val core = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("core"))

lazy val app = (project in file("app"))
  .enablePlugins(ScalaJSPlugin, MockDataExport, DockerPlugin)
  .settings(
    IWDeps.useZIO(Test),
    IWDeps.laminar,
    IWDeps.zioJson,
    libraryDependencies ++= Seq(
      "com.raquo" %%% "waypoint" % "0.5.0",
      "be.doeraene" %%% "url-dsl" % "0.4.0",
      "io.laminext" %%% "core" % IWVersions.laminar,
      "io.laminext" %%% "ui" % IWVersions.laminar,
      "io.laminext" %%% "tailwind" % IWVersions.laminar,
      "io.laminext" %%% "validation-core" % IWVersions.laminar
    )
  )
  .settings(
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.ESModule) },
    scalaJSLinkerConfig ~= {
      _.withModuleSplitStyle(ModuleSplitStyle.FewestModules)
    },
    scalaJSLinkerConfig ~= { _.withSourceMap(false) },
    scalaJSUseMainModuleInitializer := true
  )
  .settings(
    caddyFile := baseDirectory.value / "Caddyfile",
    dockerRepository := Some("docker.e-bs.cz"),
    dockerUsername := Some("cmi/posuzovani-mdr-pdb"),
    dockerExposedPorts += 80,
    Docker / mappings ++= directory(viteBuild.value),
    Docker / mappings += caddyFile.value -> "Caddyfile",
    dockerCommands := Seq(
      Cmd("FROM", "caddy:2.4.6"),
      Cmd("COPY", "Caddyfile", "/etc/caddy/Caddyfile"),
      Cmd("COPY", "vite", "/srv/mdr/pdb")
    ),
    viteDist := target.value / "vite",
    viteMonitoredFiles := {
      val baseGlob = baseDirectory.value.toGlob
      def baseFiles(pattern: String): Glob = baseGlob / pattern
      val viteConfigs =
        FileTreeView.default.list(
          List(baseFiles("*.json"), baseFiles("*.js"), baseFiles("*.html"))
        )
      val linkerDirectory =
        (Compile / fullLinkJS / scalaJSLinkerOutputDirectory).value
      val viteInputs = FileTreeView.default.list(
        linkerDirectory.toGlob / "*.js"
      )
      (viteConfigs ++ viteInputs).map(_._1.toFile)
    },
    viteBuild := {
      val s = streams.value
      val dist = viteDist.value
      val files = viteMonitoredFiles.value
      // We depend on fullLinkJS
      val _ = (Compile / fullLinkJS).value
      def doBuild() = Process(
        "yarn" :: "build" :: "--outDir" :: dist.toString :: Nil,
        baseDirectory.value
      ) ! s.log
      val cachedFun = FileFunction.cached(s.cacheDirectory / "vite") { _ =>
        doBuild()
        Set(dist)
      }
      cachedFun(files.toSet).head
    }
  )
  .dependsOn(core.js)

lazy val server = (project in file("server")).settings(
  IWDeps.useZIO(),
  libraryDependencies ++= Seq(
    "org.http4s" %% "http4s-blaze-server" % "0.23.10",
    "com.softwaremill.sttp.tapir" %% "tapir-core" % "0.20.0-M10",
    "com.softwaremill.sttp.tapir" %% "tapir-zio" % "0.20.0-M10",
    "com.softwaremill.sttp.tapir" %% "tapir-zio-http4s-server" % "0.20.0-M10",
    "dev.zio" %% "zio-interop-cats" % "3.3.0-RC2",
    "dev.zio" %% "zio-logging-slf4j" % "2.0.0-RC5",
    "ch.qos.logback" % "logback-classic" % "1.2.10" % Runtime,
    "org.pac4j" %% "http4s-pac4j" % "4.0.0",
    "org.pac4j" % "pac4j-oidc" % "5.2.0"
  )
)

lazy val root = (project in file("."))
  .settings(name := "mdr-personnel-db", publish / skip := true)
  // Auto activates for all projects, but make sure we have required dependencies
  .enablePlugins(IWScalaProjectPlugin)
  .aggregate(app, server)
