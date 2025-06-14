name := "iw-support-e2e-testing"

// Testing framework dependencies
libraryDependencies ++= Seq(
    "com.microsoft.playwright" % "playwright" % "1.47.0",
    "io.cucumber" %% "cucumber-scala" % "8.24.0",
    "io.cucumber" % "cucumber-junit" % "7.20.0",
    "com.github.sbt" % "junit-interface" % "0.13.3",
    "com.typesafe" % "config" % "1.4.3",
    "ch.qos.logback" % "logback-classic" % "1.5.15"
)
