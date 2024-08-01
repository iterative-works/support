import org.scalajs.linker.interface.ModuleSplitStyle

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
}

scalacOptions += {
    val localRootBase = (LocalRootProject / baseDirectory).value
    s"-scalajs-mapSourceURI:${localRootBase.toURI.toString}->/mdr/poptavky/@fs${localRootBase.toString}/",
}

scalaJSUseMainModuleInitializer := true
