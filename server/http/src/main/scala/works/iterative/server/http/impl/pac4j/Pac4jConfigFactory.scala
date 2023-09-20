package works.iterative.server.http
package impl.pac4j

import org.pac4j.core.config.*
import org.pac4j.oidc.client.OidcClient
import org.pac4j.oidc.config.OidcConfiguration
import org.pac4j.core.authorization.generator.AuthorizationGenerator
import org.pac4j.http4s.{DefaultHttpActionAdapter, Http4sCacheSessionStore}
import org.pac4j.core.context.WebContext
import org.pac4j.core.context.session.SessionStore
import org.pac4j.core.profile.UserProfile
import java.util.Optional
import cats.effect.Sync
import org.pac4j.core.client.Clients

class Pac4jConfigFactory[F[_] <: AnyRef: Sync](pac4jConfig: Pac4jSecurityConfig)
    extends ConfigFactory:

  override def build(parameters: AnyRef*): Config =
    val clients = Clients(
      s"${pac4jConfig.urlBase}/${pac4jConfig.callbackBase}/callback",
      oidcClient()
    )
    val config = new Config(clients)
    config.setHttpActionAdapter(DefaultHttpActionAdapter[F]())
    config.setSessionStore(Http4sCacheSessionStore[F]())
    config

  def oidcClient(): OidcClient =
    val oidcConfiguration = new OidcConfiguration()
    oidcConfiguration.setClientId(pac4jConfig.clientId)
    oidcConfiguration.setSecret(pac4jConfig.clientSecret)
    oidcConfiguration.setDiscoveryURI(pac4jConfig.discoveryURI)
    oidcConfiguration.setUseNonce(true)
    // oidcConfiguration.addCustomParam("prompt", "consent")
    val oidcClient = new OidcClient(oidcConfiguration)

    val authorizationGenerator = new AuthorizationGenerator:
      override def generate(
          context: WebContext,
          sessionStore: SessionStore,
          profile: UserProfile
      ): Optional[UserProfile] = Optional.of(profile)

    oidcClient.setAuthorizationGenerator(authorizationGenerator)
    oidcClient