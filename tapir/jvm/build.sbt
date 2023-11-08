name := "iw-support-tapir"

IWDeps.useZIO()
IWDeps.tapirCore
IWDeps.tapirZIOJson
IWDeps.useZIOJson
IWDeps.tapirSttpClient
IWDeps.sttpClientLib("zio")
IWDeps.tapirZIO
IWDeps.tapirZIOHttp4sServer
IWDeps.useZIOJson
IWDeps.zioConfig
IWDeps.zioInteropReactiveStreams
IWDeps.zioNIO

// Fix silencer doc
libraryDependencies += "com.github.ghik" %% "silencer-lib" % "1.4.2" % Provided cross CrossVersion.for3Use2_13

excludeDependencies += // Gets transitively dragged in by zio-nio, conflicting with _3
  ExclusionRule("org.scala-lang.modules", "scala-collection-compat_2.13")
