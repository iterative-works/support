import org.scalajs.linker.interface.ModuleSplitStyle

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := scala3Version

lazy val app = (project in file("app"))
  .enablePlugins(ScalaJSPlugin)
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

lazy val root = (project in file("."))
  .settings(name := "mdr-personnel-db", publish / skip := true)
  // Auto activates for all projects, but make sure we have required dependencies
  .enablePlugins(IWScalaProjectPlugin)
  .aggregate(app)
