name := "iw-support-server-http"

IWDeps.useZIO()
IWDeps.zioConfig
IWDeps.zioConfigTypesafe
IWDeps.zioConfigMagnolia
IWDeps.zioLoggingSlf4j
IWDeps.zioInteropCats
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
