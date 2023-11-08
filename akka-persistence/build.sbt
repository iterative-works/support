name := "iw-support-akka-persistence"

IWDeps.useZIO(Test)
IWDeps.useZIOJson
IWDeps.zioConfig
libraryDependencies += IWDeps.akka.modules.persistence
  .cross(CrossVersion.for3Use2_13)
libraryDependencies += "com.typesafe.akka" %% "akka-cluster-sharding-typed" % IWDeps.akka.V cross (CrossVersion.for3Use2_13)
IWDeps.akka.profiles.eventsourcedJdbcProjection
libraryDependencies += "com.github.ghik" %% "silencer-lib" % "1.4.2" % Provided cross CrossVersion.for3Use2_13
