package works.iterative.server.http
package impl.pac4j

import zio.*
import zio.config.*

case class Pac4jSecurityConfig(
    urlBase: String,
    callbackBase: String,
    logoutUrl: Option[String],
    clientId: String,
    clientSecret: String,
    discoveryURI: String,
    sessionSecret: String
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
                ) zip string("SESSIONSECRET")
        ).to[Pac4jSecurityConfig]
    end configDesc

    val fromEnv: ZLayer[Any, ReadError[String], Pac4jSecurityConfig] =
        ZConfig.fromSystemEnv(
            configDesc,
            keyDelimiter = Some('_'),
            valueDelimiter = Some(',')
        )
end Pac4jSecurityConfig
