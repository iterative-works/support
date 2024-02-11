package works.iterative.server.http
package impl.pac4j

import zio.*
import zio.config.*

case class OidcClientConfig(
    clientId: String,
    clientSecret: String,
    discoveryURI: String
)

case class Pac4jSecurityConfig(
    urlBase: String,
    callbackBase: String,
    logoutUrl: Option[String],
    sessionSecret: String,
    client: OidcClientConfig,
    clients: Map[String, OidcClientConfig]
)

object Pac4jSecurityConfig:
    import ConfigDescriptor.*
    val oidcConfigDesc: ConfigDescriptor[OidcClientConfig] =
        (string("ID") zip string("SECRET") zip string(
            "DISCOVERYURI"
        )).to[OidcClientConfig]
    end oidcConfigDesc

    val configDesc: ConfigDescriptor[Pac4jSecurityConfig] =
        nested("SECURITY")(
            string("URLBASE") zip string("CALLBACKBASE") zip string(
                "LOGOUTURL"
            ).optional zip string("SESSIONSECRET") zip nested("CLIENT")(oidcConfigDesc) zip map(
                "CLIENTS"
            )(oidcConfigDesc).default(Map.empty)
        ).to[Pac4jSecurityConfig]
    end configDesc

    val fromEnv: ZLayer[Any, ReadError[String], Pac4jSecurityConfig] =
        ZConfig.fromSystemEnv(
            configDesc,
            keyDelimiter = Some('_'),
            valueDelimiter = Some(',')
        )
end Pac4jSecurityConfig
