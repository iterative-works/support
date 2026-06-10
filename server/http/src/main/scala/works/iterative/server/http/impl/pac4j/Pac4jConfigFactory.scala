// PURPOSE: Builds the Pac4j Config (clients + session store) from a validated Pac4jSecurityConfig.
// PURPOSE: Composes the OIDC redirect_uri via Pac4jSecurityConfig.callbackUrl — no string surgery here.
package works.iterative.server.http
package impl.pac4j

import org.pac4j.core.config.*
import org.pac4j.oidc.client.OidcClient
import org.pac4j.oidc.config.OidcConfiguration
import org.pac4j.http4s.DefaultHttpActionAdapter
import cats.effect.Sync
import org.pac4j.core.client.Clients
import org.pac4j.core.authorization.generator.AuthorizationGenerator
import org.pac4j.core.profile.UserProfile
import java.util.Optional
import scala.jdk.CollectionConverters.*
import scala.annotation.nowarn
import org.pac4j.core.context.CallContext
import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod
import org.http4s.SameSite
import org.pac4j.http4s.Http4sGenericSessionStore
import org.pac4j.http4s.CacheSessionRepository
import cats.effect.std.Dispatcher

/** Builds Pac4j's `Config` (clients + session store) from a validated [[Pac4jSecurityConfig]].
  *
  * The OIDC client's callback URL is taken verbatim from [[Pac4jSecurityConfig.callbackUrl]];
  * the cookie `Path` comes from [[Pac4jSecurityConfig.resolvedCookiePath]]. Both values are
  * validated at config-load time, so this class performs no further URL surgery.
  *
  * **Breaking change vs. iw-support 0.1.15:** the `baseUri: BaseUri` constructor parameter has
  * been removed. Pass [[Pac4jSecurityConfig]] alone — the callback URL is derived from
  * `urlBase + callbackBase` and the cookie path from `cookiePath`.
  */
class Pac4jConfigFactory[F[_] <: AnyRef: Sync](
    pac4jConfig: Pac4jSecurityConfig,
    dispatcher: Dispatcher[F],
    authorizationGenerator: AuthorizationGenerator =
        Pac4jConfigFactory.defaultAuthorizationGenerator
) extends ConfigFactory:
    val sessionStore = Http4sGenericSessionStore[F](
        new CacheSessionRepository[F],
        dispatcher
    )(
        path = Some(pac4jConfig.resolvedCookiePath),
        secure = pac4jConfig.urlBase.startsWith("https://"),
        httpOnly = true,
        sameSite = Some(SameSite.Lax)
    )

    @nowarn("cat=deprecation")
    override def build(parameters: AnyRef*): Config =
        val clients = Clients(
            pac4jConfig.callbackUrl,
            (oidcClient(pac4jConfig.client) :: (pac4jConfig.clients.map: (name, conf) =>
                val client = oidcClient(conf)
                client.setName(name)
                client
            ).toList).asJava
        )
        val config = new Config(clients)
        config.setHttpActionAdapter(DefaultHttpActionAdapter[F]())
        config.setSessionStoreFactory(_ => sessionStore)

        config
    end build

    def oidcClient(c: OidcClientConfig): OidcClient =
        val oidcConfiguration = new OidcConfiguration()
        oidcConfiguration.setClientId(c.clientId)
        oidcConfiguration.setSecret(c.clientSecret)
        oidcConfiguration.setDiscoveryURI(c.discoveryURI)
        oidcConfiguration.setUseNonce(true)
        oidcConfiguration.setClientAuthenticationMethod(
            ClientAuthenticationMethod.CLIENT_SECRET_BASIC
        )
        val oidcClient = new OidcClient(oidcConfiguration)
        oidcClient.setAuthorizationGenerator(authorizationGenerator)
        oidcClient
    end oidcClient
end Pac4jConfigFactory

object Pac4jConfigFactory:
    val defaultAuthorizationGenerator: AuthorizationGenerator =
        new AuthorizationGenerator:
            override def generate(
                context: CallContext,
                profile: UserProfile
            ): Optional[UserProfile] = Optional.of(profile)
    end defaultAuthorizationGenerator
end Pac4jConfigFactory
