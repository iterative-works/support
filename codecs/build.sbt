name := "iw-support-codecs"

IWDeps.useZIOJson

excludeDependencies += // Gets transitively dragged in by zio-nio, conflicting with _3
  ExclusionRule("org.scala-lang.modules", "scala-collection-compat_2.13")
