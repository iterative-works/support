name := "iw-support-mongo"

IWDeps.useZIO()
IWDeps.useZIOJson
IWDeps.zioConfig
IWDeps.zioInteropReactiveStreams
libraryDependencies += ("org.mongodb.scala" %% "mongo-scala-driver" % "4.2.3")
    .cross(CrossVersion.for3Use2_13)

// Fix silencer doc
libraryDependencies += "com.github.ghik" %% "silencer-lib" % "1.4.2" % Provided cross CrossVersion.for3Use2_13
