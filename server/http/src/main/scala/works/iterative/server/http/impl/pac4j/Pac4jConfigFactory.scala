package works.iterative.server.http
package impl.pac4j

import org.pac4j.core.config.*
import org.pac4j.oidc.client.OidcClient
import org.pac4j.oidc.config.OidcConfiguration
import org.pac4j.http4s.DefaultHttpActionAdapter
import cats.effect.Sync
import org.pac4j.core.client.Clients
import works.iterative.tapir.BaseUri
import org.pac4j.core.authorization.generator.AuthorizationGenerator
import org.pac4j.core.profile.UserProfile
import java.util.Optional
import scala.jdk.CollectionConverters.*
import org.pac4j.http4s.Http4sCacheSessionStore
import scala.annotation.nowarn
import org.pac4j.core.context.CallContext

class Pac4jConfigFactory[F[_] <: AnyRef: Sync](
    baseUri: BaseUri,
    pac4jConfig: Pac4jSecurityConfig,
    authorizationGenerator: AuthorizationGenerator =
        Pac4jConfigFactory.defaultAuthorizationGenerator
) extends ConfigFactory:

    @nowarn("cat=deprecation")
    override def build(parameters: AnyRef*): Config =
        val clients = Clients(
            s"${pac4jConfig.urlBase}${baseUri.value.fold("/")(_.toString)}${pac4jConfig.callbackBase}/callback",
            (oidcClient(pac4jConfig.client) :: (pac4jConfig.clients.map: (name, conf) =>
                val client = oidcClient(conf)
                client.setName(name)
                client
            ).toList).asJava
            // new AnonymousClient
        )
        val config = new Config(clients)
        config.setHttpActionAdapter(DefaultHttpActionAdapter[F]())
        config.setSessionStoreFactory(_ =>
            Http4sCacheSessionStore[F](
                path = Some(baseUri.value.fold("/")(_.toString)),
                secure = pac4jConfig.callbackBase.startsWith("https://"),
                httpOnly = true
            )
        )

        config
    end build

    def oidcClient(c: OidcClientConfig): OidcClient =
        val oidcConfiguration = new OidcConfiguration()
        oidcConfiguration.setClientId(c.clientId)
        oidcConfiguration.setSecret(c.clientSecret)
        oidcConfiguration.setDiscoveryURI(c.discoveryURI)
        oidcConfiguration.setUseNonce(true)
        // oidcConfiguration.addCustomParam("prompt", "consent")
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
