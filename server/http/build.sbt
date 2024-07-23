name := "iw-support-server-http"

IWDeps.useZIO()
IWDeps.zioConfig
IWDeps.zioConfigTypesafe
IWDeps.zioConfigMagnolia
IWDeps.zioLoggingSlf4j
// TODO: use IWDeps.zioInteropCats with next iw-support version (0.3.19+)
libraryDependencies += "dev.zio" %% "zio-interop-cats" % "23.0.0.8"
IWDeps.tapirCore
IWDeps.tapirZIO
IWDeps.tapirZIOJson
IWDeps.tapirLib("files")
IWDeps.tapirZIOHttp4sServer
IWDeps.http4sBlazeServer
IWDeps.http4sPac4J
IWDeps.pac4jOIDC

libraryDependencies += "com.lihaoyi" %% "scalatags" % "0.13.1"

// Fix silencer doc
libraryDependencies += "com.github.ghik" %% "silencer-lib" % "1.4.2" % Provided cross CrossVersion.for3Use2_13
