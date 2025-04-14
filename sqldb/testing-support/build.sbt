name := "iw-support-sqldb-testing"

IWDeps.zio
IWDeps.zioLib("test", IWVersions.zio)

IWDeps.logbackClassic
libraryDependencies ++= Seq(
    // Use latest testcontainers (testcontainers-scala pulls in older version)
    "org.testcontainers" % "testcontainers" % "1.20.6",
    "org.testcontainers" % "postgresql" % "1.20.6",
    "com.dimafeng" %% "testcontainers-scala-postgresql" % "0.43.0"
)
