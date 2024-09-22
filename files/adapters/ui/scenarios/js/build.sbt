enablePlugins(VitePlugin)

scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.ESModule) }

libraryDependencies += "com.raquo" %%% "laminar-shoelace" % "0.1.0"
