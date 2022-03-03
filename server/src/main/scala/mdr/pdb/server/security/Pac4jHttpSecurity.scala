package mdr.pdb.server
package security

import java.util.Optional
import org.http4s.HttpRoutes
import org.http4s.Request
import org.http4s.ResponseCookie
import org.http4s.server.AuthMiddleware
import org.pac4j.core.authorization.generator.AuthorizationGenerator
import org.pac4j.core.client.Clients
import org.pac4j.core.config.Config
import org.pac4j.core.context.WebContext
import org.pac4j.core.context.session.SessionStore
import org.pac4j.core.profile.CommonProfile
import org.pac4j.core.profile.UserProfile
import org.pac4j.http4s.*
import org.pac4j.oidc.client.OidcClient
import org.pac4j.oidc.config.OidcConfiguration
import scala.concurrent.duration.{*, given}
import zio.*
import zio.config.*
import zio.interop.catz.*
import zio.interop.catz.implicits.{*, given}

object Pac4jHttpSecurity:
  val layer: URLayer[Pac4jSecurityConfig & Runtime[AppEnv], HttpSecurity] =
    (Pac4jHttpSecurity(_, _)).toLayer[HttpSecurity]

class Pac4jHttpSecurity(
    config: Pac4jSecurityConfig,
    runtime: Runtime[AppEnv]
) extends HttpSecurity
    with CustomDsl:
  private val contextBuilder =
    (req: Request[AppTask], conf: org.pac4j.core.config.Config) =>
      new Http4sWebContext[AppTask](
        req,
        conf.getSessionStore,
        runtime.unsafeRun(_)
      )

  def oidcClient(): OidcClient = {
    val oidcConfiguration = new OidcConfiguration()
    oidcConfiguration.setClientId(config.clientId)
    oidcConfiguration.setSecret(config.clientSecret)
    oidcConfiguration.setDiscoveryURI(config.discoveryURI)
    oidcConfiguration.setUseNonce(true)
    // oidcConfiguration.addCustomParam("prompt", "consent")
    val oidcClient = new OidcClient(oidcConfiguration)

    val authorizationGenerator = new AuthorizationGenerator {
      override def generate(
          context: WebContext,
          sessionStore: SessionStore,
          profile: UserProfile
      ): Optional[UserProfile] = {
        // profile.addRole("ROLE_ADMIN")
        Optional.of(profile)
      }
    }
    oidcClient.setAuthorizationGenerator(authorizationGenerator)
    oidcClient
  }

  val pac4jConfig =
    val clients =
      Clients(
        s"${config.urlBase}/${config.callbackBase}/callback",
        oidcClient()
      )
    val conf = org.pac4j.core.config.Config(clients)
    conf.setHttpActionAdapter(DefaultHttpActionAdapter[AppTask]())
    conf.setSessionStore(Http4sCacheSessionStore[AppTask]())
    conf

  private val sessionConfig = SessionConfig(
    cookieName = "session",
    mkCookie = ResponseCookie(_, _, path = Some("/")),
    secret = "This is a secret",
    maxAge = 5.minutes
  )

  val callbackService =
    CallbackService[AppTask](pac4jConfig, contextBuilder)

  val localLogoutService = LogoutService[AppTask](
    pac4jConfig,
    contextBuilder,
    config.logoutUrl,
    destroySession = true
  )
  val centralLogoutService = LogoutService[AppTask](
    pac4jConfig,
    contextBuilder,
    defaultUrl = config.logoutUrl,
    logoutUrlPattern = Some(s"${config.logoutUrl}.*"),
    localLogout = false,
    destroySession = true,
    centralLogout = true
  )

  val sessionManagement = Session.sessionManagement[AppTask](sessionConfig)
  val securityFilter = SecurityFilterMiddleware
    .securityFilter[AppTask](pac4jConfig, contextBuilder)

  val routes: HttpRoutes[AppTask] =
    HttpRoutes.of {
      case req @ GET -> Root / "callback" =>
        callbackService.callback(req)
      case req @ POST -> Root / "callback" =>
        callbackService.callback(req)
      case req @ GET -> Root / "logout" =>
        localLogoutService.logout(req)
      case req @ GET -> Root / "centralLogout" =>
        centralLogoutService.logout(req)
    }

  override val route: (String, HttpRoutes[AppTask]) =
    s"/${config.callbackBase}" -> sessionManagement(routes)

  override def secure: AuthMiddleware[AppTask, List[CommonProfile]] =
    sessionManagement.compose(securityFilter)
