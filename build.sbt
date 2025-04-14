import org.typelevel.scalacoptions.ScalacOptions
import org.scalajs.linker.interface.ModuleSplitStyle

ThisBuild / scalaVersion := scala3Version

ThisBuild / organization := "works.iterative.support"

lazy val commonSettings = Seq(
    tpolecatScalacOptions += ScalacOptions.scala3Source("3.7")
)

publishToIW

// Exported projects

lazy val core = crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Full)
    .in(file("core"))
    .settings(commonSettings)

lazy val entity = crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Full)
    .in(file("entity"))
    .settings(commonSettings)
    .dependsOn(core)

lazy val `service-specs` = crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Full)
    .in(file("service-specs"))
    .settings(commonSettings)
    .dependsOn(core)

lazy val `tapir-support` = crossProject(JSPlatform, JVMPlatform)
    .in(file("tapir"))
    .settings(commonSettings)
    .dependsOn(core)

lazy val `files-core` = crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Full)
    .in(file("files/core"))
    .settings(commonSettings)
    .dependsOn(core)

lazy val `files-rest` = crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Full)
    .in(file("files/adapters/rest"))
    .settings(commonSettings)
    .dependsOn(`files-core`, `tapir-support`)

lazy val `files-mongo` = project
    .in(file("files/adapters/mongo"))
    .settings(commonSettings)
    .dependsOn(`files-core`.jvm, `mongo-support`)

lazy val `files-mongo-it` = project
    .in(file("files/adapters/mongo/it"))
    .settings(publish / skip := true)
    .settings(IWDeps.useZIO())
    .settings(commonSettings)
    .dependsOn(`files-mongo`)

lazy val `files-ui` = project
    .enablePlugins(ScalaJSPlugin)
    .in(file("files/adapters/ui"))
    .settings(commonSettings)
    .dependsOn(`files-core`.js, ui.js)

lazy val `files-ui-scenarios` = crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Full)
    .settings(publish / skip := true)
    .settings(commonSettings)
    .in(file("files/adapters/ui/scenarios"))
    .jsConfigure(_.dependsOn(`files-ui`))
    .dependsOn(`files-core`, `files-rest`, ui, scenarios)

lazy val `files-it` = project
    .in(file("files/it"))
    .settings(commonSettings)
    .dependsOn(`files-rest`.jvm, http)

lazy val `autocomplete` = crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Full)
    .in(file("autocomplete"))
    .settings(commonSettings)
    .jsConfigure(_.dependsOn(`files-ui`))
    .dependsOn(core, `tapir-support`, ui, `ui-forms`)

lazy val hashicorp = crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Full)
    .in(file("hashicorp"))
    .settings(commonSettings)
    .dependsOn(core, `service-specs`, `tapir-support`)

lazy val codecs = crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Pure)
    .in(file("codecs"))
    .settings(commonSettings)
    .dependsOn(core, entity, `tapir-support`)

lazy val `mongo-support` = project.in(file("mongo"))
    .settings(commonSettings)
    .dependsOn(core.jvm)

lazy val `sqldb-support` = project.in(file("sqldb"))
    .settings(commonSettings)
    .dependsOn(core.jvm)

lazy val `sqldb-testing-support` =
    project.in(file("sqldb/testing-support")).settings(commonSettings).dependsOn(`sqldb-support`)

lazy val paygate = project
    .in(file("paygate"))
    .settings(commonSettings)
    .dependsOn(core.jvm, `tapir-support`.jvm)

lazy val email = project.in(file("email"))
    .settings(commonSettings)
    .dependsOn(core.jvm)

lazy val `akka-persistence-support` = project
    .in(file("akka-persistence"))
    .settings(commonSettings)
    .dependsOn(core.jvm, entity.jvm)

lazy val ui = crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Full)
    .in(file("ui"))
    .settings(commonSettings)
    .dependsOn(core, `tapir-support`)

lazy val `ui-forms` = crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Full)
    .in(file("ui/forms"))
    .settings(commonSettings)
    .dependsOn(ui, `files-core`)

lazy val forms = crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Full)
    .in(file("forms"))
    .jvmConfigure(_.dependsOn(email, paygate, `files-mongo`))
    .settings(commonSettings)
    .dependsOn(core, codecs, autocomplete, `files-rest`)

lazy val `forms-scenarios` = crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Full)
    .enablePlugins(BuildInfoPlugin)
    .settings(commonSettings)
    .settings(
        publish / skip := true,
        buildInfoPackage := "works.iterative.forms.scenarios",
        buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion)
    )
    .in(file("forms/scenarios"))
    .dependsOn(forms, scenarios)

lazy val http = (project in file("server/http"))
    .settings(commonSettings)
    .dependsOn(core.jvm, codecs.jvm, `tapir-support`.jvm)

// Internal projects

lazy val `mongo-support-it` = project
    .in(file("mongo/it"))
    .settings(publish / skip := true)
    .settings(IWDeps.useZIO())
    .settings(commonSettings)
    .dependsOn(`mongo-support`)

lazy val `scenarios-ui` = project
    .in(file("ui/scenarios"))
    .enablePlugins(ScalaJSPlugin, VitePlugin)
    .configure(IWDeps.useScalaJavaTimeAndLocales)
    .settings(commonSettings)
    .dependsOn(`ui`.js, `ui-forms`.js)

lazy val scenarios = crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Full)
    .in(file("scenarios"))
    .settings(commonSettings)
    .dependsOn(core)

lazy val `forms-core` = crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Full)
    .in(file("forms/core"))
    .settings(commonSettings)
    .settings(
        name := "iw-support-forms-core"
    )
    .dependsOn(core)

lazy val `ui-core` = crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Full)
    .in(file("ui/core"))
    .settings(
        name := "iw-support-ui-core"
    )
    .settings(commonSettings)
    .dependsOn(`forms-core`)

lazy val `forms-http` = project
    .in(file("forms/http"))
    .settings(
        name := "iw-support-forms-http"
    )
    .settings(commonSettings)
    .dependsOn(`forms-core`.jvm, http)

lazy val `ui-scalatags` = crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Full)
    .in(file("ui/scalatags"))
    .settings(
        name := "iw-support-ui-scalatags"
    )
    .settings(commonSettings)
    .dependsOn(`ui-core`)

lazy val root = (project in file("."))
    .enablePlugins(IWScalaProjectPlugin)
    .settings(
        name := "iw-support",
        publish / skip := true
    )
    .settings(commonSettings)
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
        forms.js,
        forms.jvm,
        http,
        `forms-core`.js,
        `forms-core`.jvm,
        `ui-core`.js,
        `ui-core`.jvm,
        `forms-http`,
        `ui-scalatags`.js,
        `ui-scalatags`.jvm,
        `sqldb-support`,
        `sqldb-testing-support`
    )
