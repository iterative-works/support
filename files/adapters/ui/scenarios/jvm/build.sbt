libraryDependencies += "dev.zio" %% "zio-http" % "3.0.0-RC6"
libraryDependencies += "dev.zio" %% "zio-http-htmx" % "3.0.0-RC6"

reStart / mainClass := Some("works.iterative.files.scenarios.ScenariosServer")
