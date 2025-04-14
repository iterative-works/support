name := "iw-support-sqldb"

IWDeps.useZIO()
IWDeps.useZIOJson
IWDeps.zioConfig

IWDeps.magnumZIO
IWDeps.magnumPG

IWDeps.chimney

libraryDependencies ++= Seq(
    "org.flywaydb" % "flyway-core" % "11.4.0",
    "org.flywaydb" % "flyway-database-postgresql" % "11.4.0",
    "org.postgresql" % "postgresql" % "42.7.5",
    "com.zaxxer" % "HikariCP" % "6.2.1"
)
