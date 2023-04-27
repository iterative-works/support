ThisBuild / scalaVersion := scala3Version

ThisBuild / organization := "works.iterative.support"

publishToIW

lazy val core = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full)
  .settings(name := "iw-support-core")
  .in(file("core"))
  .settings(IWDeps.zioPrelude)

lazy val `tapir-support` = crossProject(JSPlatform, JVMPlatform)
  .in(file("tapir"))
  .settings(name := "iw-support-tapir")
  .settings(
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
    IWDeps.zioInteropReactiveStreams,
    IWDeps.zioNIO,
    excludeDependencies += // Gets transitively dragged in by zio-nio, conflicting with _3
      ExclusionRule("org.scala-lang.modules", "scala-collection-compat_2.13")
  )

lazy val codecs = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("codecs"))
  .settings(name := "iw-support-codecs")
  .settings(
    IWDeps.useZIOJson,
    excludeDependencies += // Gets transitively dragged in by zio-nio, conflicting with _3
      ExclusionRule("org.scala-lang.modules", "scala-collection-compat_2.13")
  )
  .dependsOn(core, `tapir-support`)

lazy val `mongo-support` = project
  .in(file("mongo"))
  .configs(IntegrationTest)
  .settings(name := "iw-support-mongo")
  .settings(
    Defaults.itSettings,
    IWDeps.useZIO(Test, IntegrationTest),
    IWDeps.useZIOJson,
    IWDeps.zioConfig,
    libraryDependencies += ("org.mongodb.scala" %% "mongo-scala-driver" % "4.2.3")
      .cross(CrossVersion.for3Use2_13)
  )

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
    IWDeps.akka.profiles.eventsourcedJdbcProjection
  )

lazy val ui = crossProject(JSPlatform, JVMPlatform).crossType(CrossType.Full).in(file("ui"))
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
  ).dependsOn(core)

lazy val root = (project in file("."))
  .enablePlugins(IWScalaProjectPlugin)
  .settings(
    name := "iw-support",
    publish / skip := true
  )
  .aggregate(
    core.js,
    core.jvm,
    codecs.js,
    codecs.jvm,
    `tapir-support`.js,
    `tapir-support`.jvm,
    `mongo-support`,
    `akka-persistence-support`,
    ui.js,
    ui.jvm
  )
