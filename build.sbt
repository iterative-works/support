import org.scalajs.linker.interface.ModuleSplitStyle

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := scala3Version

lazy val app = (project in file("app"))
  .enablePlugins(ScalaJSPlugin)
  .settings(IWDeps.useZIO(Test), IWDeps.laminar)
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
