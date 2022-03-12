import org.scalajs.linker.interface.ModuleSplitStyle
import scala.sys.process._
import com.typesafe.sbt.packager.docker._
import NativePackagerHelper._
import sbtcrossproject.CrossPlugin.autoImport.{CrossType, crossProject}

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := scala3Version

lazy val proof = entityProject("proof", file("domain/proof"))
  .components(_.dependsOn(ui))
  .model(_.dependsOn(core))
  .codecs(_.dependsOn(codecs, `tapir-support`))
  .repo(_.dependsOn(`mongo-support`))
  .entity(_.dependsOn(`akka-persistence-support`))
  .projection(_.dependsOn(`akka-persistence-support`))
  .endpoints(_.dependsOn(`tapir-support`, endpoints))

lazy val parameters = entityProject("parameters", file("domain/parameters"))
  .components(_.dependsOn(ui))
  .model(_.dependsOn(core))
  .codecs(_.dependsOn(codecs, `tapir-support`))
  .endpoints(_.dependsOn(`tapir-support`, endpoints))

lazy val users = entityProject("users", file("domain/users"))
  .components(_.dependsOn(ui))
  .model(_.dependsOn(core))
  .codecs(_.dependsOn(codecs, `tapir-support`))
  .endpoints(_.dependsOn(`tapir-support`, endpoints))

lazy val core = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("core"))

lazy val codecs = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("codecs"))
  .settings(IWDeps.zioJson)
  .dependsOn(core, `tapir-support`)

lazy val endpoints = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("endpoints"))
  .dependsOn(core, codecs, `tapir-support`)

lazy val ui = (project in file("iw/ui"))
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

lazy val `tapir-support` = crossProject(JSPlatform, JVMPlatform)
  .in(file("iw/tapir"))
  .settings(IWDeps.tapirCore, IWDeps.tapirZIOJson, IWDeps.zioJson)
  .jsSettings(IWDeps.tapirSttpClient)
  .jvmSettings(IWDeps.tapirZIO, IWDeps.tapirZIOHttp4sServer)

lazy val `mongo-support` = project
  .in(file("iw/mongo"))
  .settings(
    IWDeps.useZIO(Test),
    IWDeps.zioJson,
    IWDeps.zioConfig,
    libraryDependencies += ("org.mongodb.scala" %% "mongo-scala-driver" % "4.2.3")
      .cross(CrossVersion.for3Use2_13)
  )

lazy val `akka-persistence-support` = project
  .in(file("iw/akka-persistence"))
  .settings(
    IWDeps.useZIO(Test),
    IWDeps.zioJson,
    IWDeps.zioConfig,
    libraryDependencies += IWDeps.akka.modules.persistence
      .cross(CrossVersion.for3Use2_13),
    libraryDependencies += "com.typesafe.akka" %% "akka-cluster-sharding-typed" % IWDeps.akka.V cross (CrossVersion.for3Use2_13),
    IWDeps.akka.profiles.eventsourcedJdbcProjection
  )

lazy val app = (project in file("app"))
  .enablePlugins(ScalaJSPlugin, VitePlugin)
  .settings(
    IWDeps.useZIO(Test),
    IWDeps.laminar,
    IWDeps.zioJson,
    IWDeps.waypoint,
    IWDeps.urlDsl,
    IWDeps.laminextCore,
    IWDeps.laminextUI,
    IWDeps.laminextTailwind,
    IWDeps.laminextValidationCore,
    IWDeps.tapirSttpClient,
    IWDeps.sttpClientCore
  )
  .settings(
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.ESModule) },
    scalaJSLinkerConfig ~= {
      _.withModuleSplitStyle(ModuleSplitStyle.FewestModules)
    },
    scalaJSLinkerConfig ~= { _.withSourceMap(false) },
    scalaJSUseMainModuleInitializer := true
  )
  .dependsOn(
    core.js,
    ui,
    parameters.query.client,
    parameters.command.client,
    users.query.client,
    users.command.client,
    proof.query.client,
    proof.command.client,
    endpoints.js
  )

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
    IWDeps.tapirZIOJson,
    IWDeps.tapirZIOHttp4sServer,
    IWDeps.http4sBlazeServer,
    IWDeps.logbackClassic,
    IWDeps.http4sPac4J,
    IWDeps.pac4jOIDC,
    libraryDependencies += "mysql" % "mysql-connector-java" % "8.0.28",
    // libraryDependencies += "com.typesafe.akka" %% "akka-serialization-jackson" % IWDeps.akka.V,
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
    reStart / envVars := Map(
      "DB_HOST" -> "localhost",
      "APP_PATH" -> "../app/target/vite",
      "SECURITY_URLBASE" -> "http://localhost:8080",
      "SECURITY_DISCOVERYURI" -> "https://login.cmi.cz/auth/realms/MDRTest/.well-known/openid-configuration",
      "SECURITY_CALLBACKBASE" -> "mdr/pdb/auth/",
      "SECURITY_LOGOUTURL" -> "https://tc163.cmi.cz/mdr/app",
      "SECURITY_CLIENTID" -> "mdrpdbtest",
      "SECURITY_CLIENTSECRET" -> "aCZqYp2aGl1C2MbGDvglZXbJEUwRHV02"
    )
    // Revolver.enableDebugging(port = 5005, suspend = true)
  )
  .dependsOn(
    core.jvm,
    parameters.query.api,
    parameters.command.api,
    users.query.api,
    users.command.api,
    proof.query.api,
    proof.command.api,
    proof.query.projection,
    proof.command.entity,
    endpoints.jvm
  )

lazy val root = (project in file("."))
  .settings(name := "mdr-personnel-db", publish / skip := true)
  // Auto activates for all projects, but make sure we have required dependencies
  .enablePlugins(IWScalaProjectPlugin)
  .aggregate(app, server)
