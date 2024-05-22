enablePlugins(VitePlugin)

scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.ESModule) }
