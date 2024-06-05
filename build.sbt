import org.scalajs.linker.interface.ModuleSplitStyle

ThisBuild / scalaVersion := scala3Version

ThisBuild / organization := "works.iterative.support"

publishToIW

// Exported projects

lazy val core = crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Full)
    .in(file("core"))

lazy val entity = crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Full)
    .in(file("entity"))
    .dependsOn(core)

lazy val `service-specs` = crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Full)
    .in(file("service-specs"))
    .dependsOn(core)

lazy val `tapir-support` = crossProject(JSPlatform, JVMPlatform)
    .in(file("tapir"))
    .dependsOn(core)

lazy val `files-core` = crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Full)
    .in(file("files/core"))
    .dependsOn(core)

lazy val `files-rest` = crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Full)
    .in(file("files/adapters/rest"))
    .dependsOn(`files-core`, `tapir-support`)

lazy val `files-mongo` = project
    .in(file("files/adapters/mongo"))
    .dependsOn(`files-core`.jvm, `mongo-support`)

lazy val `files-mongo-it` = project
    .in(file("files/adapters/mongo/it"))
    .settings(publish / skip := true)
    .settings(IWDeps.useZIO())
    .dependsOn(`files-mongo`)

lazy val `files-ui` = project
    .enablePlugins(ScalaJSPlugin)
    .in(file("files/adapters/ui"))
    .dependsOn(`files-core`.js, ui.js)

lazy val `files-ui-scenarios` = crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Full)
    .settings(publish / skip := true)
    .in(file("files/adapters/ui/scenarios"))
    .jsConfigure(_.dependsOn(`files-ui`))
    .dependsOn(`files-core`, ui)

lazy val `autocomplete` = crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Full)
    .in(file("autocomplete"))
    .jsConfigure(_.dependsOn(`files-ui`))
    .dependsOn(core, `tapir-support`, ui, `ui-forms`)

lazy val hashicorp = crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Full)
    .in(file("hashicorp"))
    .dependsOn(core, `service-specs`, `tapir-support`)

lazy val codecs = crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Pure)
    .in(file("codecs"))
    .dependsOn(core, entity, `tapir-support`)

lazy val `mongo-support` = project.in(file("mongo")).dependsOn(core.jvm)

lazy val paygate = project
    .in(file("paygate"))
    .dependsOn(core.jvm, `tapir-support`.jvm)

lazy val email = project.in(file("email")).dependsOn(core.jvm)

lazy val `akka-persistence-support` = project
    .in(file("akka-persistence"))
    .dependsOn(core.jvm, entity.jvm)

lazy val ui = crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Full)
    .in(file("ui"))
    .dependsOn(core, `tapir-support`)

lazy val `ui-forms` = crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Full)
    .in(file("ui/forms"))
    .dependsOn(ui, `files-core`)

lazy val http = (project in file("server/http"))
    .dependsOn(core.jvm, codecs.jvm, `tapir-support`.jvm)

// Internal projects

lazy val `mongo-support-it` = project
    .in(file("mongo/it"))
    .settings(publish / skip := true)
    .settings(IWDeps.useZIO())
    .dependsOn(`mongo-support`)

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
            // .withRelativizeSourceMapBase(Some(base.toURI()))
        },
        scalacOptions += {
            val localRootBase = (LocalRootProject / baseDirectory).value
            s"-scalajs-mapSourceURI:${localRootBase.toURI.toString}->/mdr/poptavky/@fs${localRootBase.toString}/",
        },
        scalaJSUseMainModuleInitializer := true
    )
    .dependsOn(`ui`.js, `ui-forms`.js)

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
        `service-specs`.jvm,
        hashicorp.jvm,
        codecs.js,
        codecs.jvm,
        `tapir-support`.js,
        `tapir-support`.jvm,
        `mongo-support`,
        `akka-persistence-support`,
        `files-core`.js,
        `files-core`.jvm,
        `files-mongo`,
        `files-rest`.js,
        `files-rest`.jvm,
        `files-ui`,
        ui.js,
        ui.jvm,
        http
    )
