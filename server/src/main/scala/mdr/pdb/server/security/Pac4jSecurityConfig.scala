package mdr.pdb.server
package security

import zio.*
import zio.config.*

case class Pac4jSecurityConfig(
    urlBase: String,
    callbackBase: String,
    logoutUrl: Option[String],
    clientId: String,
    clientSecret: String,
    discoveryURI: String
)

object Pac4jSecurityConfig:
  val configDesc: ConfigDescriptor[Pac4jSecurityConfig] =
    import ConfigDescriptor.*
    nested("SECURITY")(
      string("URLBASE") zip string("CALLBACKBASE") zip string(
        "LOGOUTURL"
      ).optional zip string("CLIENTID") zip string("CLIENTSECRET")
        zip string(
          "DISCOVERYURI"
        )
    ).to[Pac4jSecurityConfig]

  val fromEnv: ZLayer[System, ReadError[String], Pac4jSecurityConfig] =
    ZConfig.fromSystemEnv(
      configDesc,
      keyDelimiter = Some('_'),
      valueDelimiter = Some(',')
    )
