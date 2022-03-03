package mdr.pdb.server

import zio.*
import zio.config.*

case class AppConfig(appPath: String, urlBase: String)

object AppConfig:
  val configDesc: ConfigDescriptor[AppConfig] =
    import ConfigDescriptor.*
    nested("APP")(
      string("PATH") zip string("BASE").default("http://localhost:8080")
    ).to[AppConfig]

  val fromEnv: ZLayer[System, ReadError[String], AppConfig] =
    ZConfig.fromSystemEnv(
      configDesc,
      keyDelimiter = Some('_'),
      valueDelimiter = Some(',')
    )
