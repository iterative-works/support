package works.iterative.server.http
package impl.pac4j

import zio.*

case class OidcClientConfig(
    clientId: String,
    clientSecret: String,
    discoveryURI: String
)

case class Pac4jSecurityConfig(
    urlBase: String,
    callbackBase: String,
    defaultUrl: Option[String],
    logoutUrl: Option[String],
    logoutUrlPattern: Option[String],
    sessionSecret: String,
    client: OidcClientConfig,
    clients: Map[String, OidcClientConfig]
)

object Pac4jSecurityConfig:
    import Config.*
    val oidcConfig: Config[OidcClientConfig] =
        (string("id") ++ string("secret") ++ string(
            "discoveryuri"
        )).map(OidcClientConfig.apply)
    end oidcConfig

    given config: Config[Pac4jSecurityConfig] =
        (
            string("urlbase") ++ string("callbackbase") ++ string("defaulturl").optional ++ string(
                "logouturl"
            ).optional ++ string("logouturlpattern").optional ++ string(
                "sessionsecret"
            ) ++ oidcConfig.nested("client") ++ Config.table(
                oidcConfig
            ).withDefault(Map.empty).nested("clients")
        ).nested("security").map(Pac4jSecurityConfig.apply)
    end config
end Pac4jSecurityConfig
