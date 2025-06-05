resolvers += "e-BS Release Repository" at "https://nexus.e-bs.cz/repository/maven-releases/"

resolvers += "e-BS Snapshot Repository" at "https://nexus.e-bs.cz/repository/maven-snapshots/"

(for {
  username <- sys.env.get("EBS_NEXUS_USERNAME")
  password <- sys.env.get("EBS_NEXUS_PASSWORD")
} yield credentials += Credentials(
  "Sonatype Nexus Repository Manager",
  "nexus.e-bs.cz",
  username,
  password
)).toList

addSbtPlugin(
    "works.iterative.sbt" % "sbt-iw-plugin-presets" % "0.3.28"
)
