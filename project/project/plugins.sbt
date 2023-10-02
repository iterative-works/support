resolvers += "IW releases" at "https://dig.iterative.works/maven/releases"

resolvers += "IW snapshots" at "https://dig.iterative.works/maven/snapshots"

addSbtPlugin(
  "works.iterative.sbt" % "sbt-iw-plugin-presets" % "0.3.19"
)
