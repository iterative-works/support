libraryDependencies += "dev.zio" %% "zio-http-htmx" % "3.0.0-RC6"

reStart / mainClass := Some("works.iterative.files.scenarios.ScenariosServer")

reStart / envVars += "VITE_BASE" -> s"http://localhost:5173/target/scala-${scalaVersion.value}/${name.value}-fastopt"
