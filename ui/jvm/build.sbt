name := "iw-support-ui"

IWDeps.useZIO(Test)
IWDeps.useZIOJson
IWDeps.zioPrelude

libraryDependencies += "org.apache.poi" % "poi-ooxml" % "5.2.1"
