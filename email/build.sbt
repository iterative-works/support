name := "iw-support-email"

IWDeps.useZIO()
IWDeps.zioConfig

libraryDependencies += "org.apache.commons" % "commons-email" % "1.5"


// Fix silencer doc
libraryDependencies += "com.github.ghik" %% "silencer-lib" % "1.4.2" % Provided cross CrossVersion.for3Use2_13
