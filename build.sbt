import org.scalajs.linker.interface.ModuleSplitStyle

ThisBuild / scalaVersion := scala3Version

ThisBuild / organization := "works.iterative.support"

publishToIW

lazy val fixSilencerDoc =
  libraryDependencies += "com.github.ghik" %% "silencer-lib" % "1.4.2" % Provided cross CrossVersion.for3Use2_13

lazy val core = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full)
  .settings(name := "iw-support-core")
  .in(file("core"))
  .settings(
    IWDeps.zioPrelude,
    IWDeps.zioJson,
    // TODO: use zio-optics when derivation is available
    libraryDependencies ++= Seq(
      "dev.optics" %%% "monocle-core" % "3.2.0",
      "dev.optics" %%% "monocle-macro" % "3.2.0"
    )
  )

lazy val service = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full)
  .settings(name := "iw-support-service")
  .in(file("service"))
  .settings(IWDeps.useZIO())
  .dependsOn(core)

lazy val entity = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full)
  .settings(name := "iw-support-entity")
  .in(file("entity"))
  .settings(IWDeps.useZIO())
  .dependsOn(core, service)

lazy val `service-specs` = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full)
  .settings(name := "iw-support-service-specs")
  .in(file("service/specs"))
  .settings(IWDeps.useZIO(), IWDeps.useZIOTest(Compile))
  .dependsOn(service)

lazy val `tapir-support` = crossProject(JSPlatform, JVMPlatform)
  .in(file("tapir"))
  .settings(name := "iw-support-tapir")
  .settings(
    IWDeps.useZIO(),
    IWDeps.tapirCore,
    IWDeps.tapirZIOJson,
    IWDeps.useZIOJson,
    IWDeps.tapirSttpClient,
    IWDeps.sttpClientLib("zio")
  )
  .jvmSettings(
    IWDeps.tapirZIO,
    IWDeps.tapirZIOHttp4sServer,
    IWDeps.useZIOJson,
    IWDeps.zioConfig,
    IWDeps.zioInteropReactiveStreams,
    IWDeps.zioNIO,
    fixSilencerDoc,
    excludeDependencies += // Gets transitively dragged in by zio-nio, conflicting with _3
      ExclusionRule("org.scala-lang.modules", "scala-collection-compat_2.13")
  )
  .dependsOn(core)

lazy val hashicorp = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full)
  .in(file("hashicorp"))
  .settings(name := "iw-support-hashicorp")
  .settings(
    IWDeps.useZIO()
  )
  .dependsOn(service, `service-specs`, `tapir-support`)

lazy val codecs = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("codecs"))
  .settings(name := "iw-support-codecs")
  .settings(
    IWDeps.useZIOJson,
    excludeDependencies += // Gets transitively dragged in by zio-nio, conflicting with _3
      ExclusionRule("org.scala-lang.modules", "scala-collection-compat_2.13")
  )
  .dependsOn(core, entity, `tapir-support`)

lazy val `mongo-support` = project
  .in(file("mongo"))
  .settings(name := "iw-support-mongo")
  .settings(
    fixSilencerDoc,
    IWDeps.useZIO(),
    IWDeps.useZIOJson,
    IWDeps.zioConfig,
    libraryDependencies += ("org.mongodb.scala" %% "mongo-scala-driver" % "4.2.3")
      .cross(CrossVersion.for3Use2_13)
  )

lazy val `mongo-support-it` = project
  .in(file("mongo/it"))
  .settings(publish / skip := true)
  .settings(IWDeps.useZIO())
  .dependsOn(`mongo-support`)

lazy val `akka-persistence-support` = project
  .in(file("akka-persistence"))
  .settings(name := "iw-support-akka-persistence")
  .settings(
    IWDeps.useZIO(Test),
    IWDeps.useZIOJson,
    IWDeps.zioConfig,
    libraryDependencies += IWDeps.akka.modules.persistence
      .cross(CrossVersion.for3Use2_13),
    libraryDependencies += "com.typesafe.akka" %% "akka-cluster-sharding-typed" % IWDeps.akka.V cross (CrossVersion.for3Use2_13),
    IWDeps.akka.profiles.eventsourcedJdbcProjection,
    libraryDependencies += "com.github.ghik" %% "silencer-lib" % "1.4.2" % Provided cross CrossVersion.for3Use2_13
  )
  .dependsOn(core.jvm, entity.jvm)

lazy val ui = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full)
  .in(file("ui"))
  .settings(name := "iw-support-ui")
  .settings(
    IWDeps.useZIO(Test),
    IWDeps.useZIOJson,
    IWDeps.zioPrelude
  )
  .jsSettings(
    IWDeps.laminar,
    IWDeps.waypoint,
    IWDeps.urlDsl,
    IWDeps.laminextCore,
    IWDeps.laminextUI,
    IWDeps.laminextTailwind,
    IWDeps.laminextValidationCore
  )
  .jvmSettings(
    libraryDependencies += "org.apache.poi" % "poi-ooxml" % "5.2.1"
  )
  .dependsOn(core, `tapir-support`)

lazy val `scenarios-ui` = project
  .in(file("ui/scenarios"))
  .enablePlugins(org.scalajs.sbtplugin.ScalaJSPlugin)
  .configure(IWDeps.useScalaJavaTimeAndLocales)
  .settings(
    scalaJSLinkerConfig := {
      val prevConfig = scalaJSLinkerConfig.value
      val base = (LocalRootProject / baseDirectory).value
      prevConfig
        .withModuleKind(ModuleKind.ESModule)
        .withModuleSplitStyle(
          ModuleSplitStyle.SmallModulesFor(
            List("works.iterative")
          )
        )
        .withSourceMap(true)
      //.withRelativizeSourceMapBase(Some(base.toURI()))
    },
    scalacOptions += {
      val localRootBase = (LocalRootProject / baseDirectory).value
      s"-scalajs-mapSourceURI:${localRootBase.toURI.toString}->/mdr/poptavky/@fs${localRootBase.toString}/",
    },
    scalaJSUseMainModuleInitializer := true
  )
  .dependsOn(`ui`.js)

lazy val http = (project in file("server/http"))
  .settings(name := "iw-support-server-http")
  .settings(
    fixSilencerDoc,
    IWDeps.useZIO(),
    IWDeps.zioConfig,
    IWDeps.zioConfigTypesafe,
    IWDeps.zioConfigMagnolia,
    IWDeps.zioLoggingSlf4j,
    // TODO: use IWDeps.zioInteropCats with next iw-support version (0.3.19+)
    libraryDependencies += "dev.zio" %% "zio-interop-cats" % "23.0.0.8",
    IWDeps.tapirCore,
    IWDeps.tapirZIO,
    IWDeps.tapirZIOJson,
    IWDeps.tapirLib("files"),
    IWDeps.tapirZIOHttp4sServer,
    IWDeps.http4sBlazeServer,
    IWDeps.http4sPac4J,
    IWDeps.pac4jOIDC
  )
  .dependsOn(core.jvm, codecs.jvm, `tapir-support`.jvm)

lazy val root = (project in file("."))
  .enablePlugins(IWScalaProjectPlugin)
  .settings(
    name := "iw-support",
    publish / skip := true
  )
  .aggregate(
    core.js,
    core.jvm,
    entity.js,
    entity.jvm,
    service.js,
    service.jvm,
    `service-specs`.jvm,
    hashicorp.jvm,
    codecs.js,
    codecs.jvm,
    `tapir-support`.js,
    `tapir-support`.jvm,
    `mongo-support`,
    `akka-persistence-support`,
    ui.js,
    ui.jvm,
    http
  )
