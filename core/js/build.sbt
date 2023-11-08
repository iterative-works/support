name := "iw-support-core"

IWDeps.zioPrelude
IWDeps.zioJson

// TODO: use zio-optics when derivation is available
libraryDependencies ++= Seq(
  "dev.optics" %%% "monocle-core" % "3.2.0",
  "dev.optics" %%% "monocle-macro" % "3.2.0"
)
