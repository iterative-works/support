package works.iterative.server.http.impl.blaze

import zio.*
import zio.config.*

case class BlazeServerConfig(host: String, port: Int)

object BlazeServerConfig:
  val configDesc: ConfigDescriptor[BlazeServerConfig] =
    import ConfigDescriptor.*
    nested("BLAZE")(
      string("HOST").default("localhost") zip int("PORT").default(8080)
    ).to[BlazeServerConfig]

  val fromEnv: ZLayer[Any, ReadError[String], BlazeServerConfig] =
    ZConfig.fromSystemEnv(
      configDesc,
      keyDelimiter = Some('_'),
      valueDelimiter = Some(',')
    )
