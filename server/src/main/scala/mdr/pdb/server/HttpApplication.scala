package mdr.pdb.server

import zio.*

import zio.interop.catz.*
import zio.interop.catz.implicits.{*, given}

import org.http4s.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*
import org.http4s.implicits.{*, given}
import org.http4s.server.Router
import org.http4s.syntax.all.{*, given}

import sttp.tapir.*
import sttp.tapir.server.http4s.ztapir.ZHttp4sServerInterpreter

import org.pac4j.http4s.*

import org.pac4j.core.authorization.generator.AuthorizationGenerator
import org.pac4j.core.client.Clients
import org.pac4j.core.config.Config
import org.pac4j.core.context.WebContext
import org.pac4j.core.context.session.SessionStore
import org.pac4j.core.profile.CommonProfile
import org.pac4j.core.profile.UserProfile
import org.pac4j.oidc.client.OidcClient
import org.pac4j.oidc.config.OidcConfiguration

import scala.concurrent.duration.{*, given}
import java.util.Optional

trait HttpApplication {
  def routes(): UIO[HttpRoutes[AppTask]]
}

object HttpApplicationLive {
  import zio.config.*

  case class AppConfig(appPath: String, urlBase: String)

  val appConfigDesc: ConfigDescriptor[AppConfig] =
    import ConfigDescriptor.*
    nested("APP")(
      string("PATH") zip string("BASE").default("http://localhost:8080")
    ).to[AppConfig]

  def layer(
      contextBuilder: (Request[AppTask], Config) => Http4sWebContext[AppTask]
  ): RLayer[System, HttpApplication] =
    val configLayer = ZConfig.fromSystemEnv(
      appConfigDesc,
      keyDelimiter = Some('_'),
      valueDelimiter = Some(',')
    )
    val appLayer =
      (HttpApplicationLive(_, contextBuilder)).toLayer[HttpApplication]
    configLayer >>> appLayer
}

import HttpApplicationLive.AppConfig

case class HttpApplicationLive(
    config: AppConfig,
    contextBuilder: (Request[AppTask], Config) => Http4sWebContext[AppTask]
) extends HttpApplication:
  val dsl: Http4sDsl[AppTask] = new Http4sDsl[AppTask] {}
  import dsl.*

  // TODO: zio-config
  def oidcClient(): OidcClient = {
    val oidcConfiguration = new OidcConfiguration()
    oidcConfiguration.setClientId("mdrpdbtest")
    oidcConfiguration.setSecret("aCZqYp2aGl1C2MbGDvglZXbJEUwRHV02")
    oidcConfiguration.setDiscoveryURI(
      "https://login.cmi.cz/auth/realms/MDRTest/.well-known/openid-configuration"
    )
    oidcConfiguration.setUseNonce(true)
    // oidcConfiguration.addCustomParam("prompt", "consent")
    val oidcClient = new OidcClient(oidcConfiguration)

    val authorizationGenerator = new AuthorizationGenerator {
      override def generate(
          context: WebContext,
          sessionStore: SessionStore,
          profile: UserProfile
      ): Optional[UserProfile] = {
        profile.addRole("ROLE_ADMIN")
        Optional.of(profile)
      }
    }
    oidcClient.setAuthorizationGenerator(authorizationGenerator)
    oidcClient
  }

  val pac4jConfig =
    val clients =
      Clients(s"${config.urlBase}/mdr/pdb/auth/callback", oidcClient())
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
    Some(config.urlBase),
    destroySession = true
  )
  val centralLogoutService = LogoutService[AppTask](
    pac4jConfig,
    contextBuilder,
    defaultUrl = Some(config.urlBase),
    logoutUrlPattern = Some(s"${config.urlBase}.*"),
    localLogout = false,
    destroySession = true,
    centralLogout = true
  )

  def filesService(appPath: String): HttpRoutes[AppTask] =
    ZHttp4sServerInterpreter()
      .from(
        List(
          fileGetServerEndpoint("pdb" / "app")(
            s"${appPath}/index.html"
          ),
          filesGetServerEndpoint("pdb")(appPath)
        )
      )
      .toRoutes

  val smMW = Session.sessionManagement[AppTask](sessionConfig)
  val sfMW = SecurityFilterMiddleware
    .securityFilter[AppTask](pac4jConfig, contextBuilder)

  def authedProtectedPages(appPath: String): HttpRoutes[AppTask] =
    smMW.compose(sfMW)(
      filesService(appPath).local(
        (req: ContextRequest[AppTask, List[CommonProfile]]) => req.req
      )
    )

  val rootRoutes: HttpRoutes[AppTask] = HttpRoutes.of {
    case req @ GET -> Root / "callback" =>
      callbackService.callback(req)
    case req @ POST -> Root / "callback" =>
      callbackService.callback(req)
    case req @ GET -> Root / "logout" =>
      localLogoutService.logout(req)
    case req @ GET -> Root / "centralLogout" =>
      centralLogoutService.logout(req)
  }

  def httpApp(appPath: String): HttpRoutes[AppTask] =
    Router(
      "/mdr/pdb/auth" -> smMW(rootRoutes),
      "/mdr" -> authedProtectedPages(appPath)
    )

  override def routes(): UIO[HttpRoutes[AppTask]] =
    ZIO.succeed(httpApp(config.appPath))
