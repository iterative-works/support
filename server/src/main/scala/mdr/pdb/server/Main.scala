package mdr.pdb.server

import zio._
import zio.interop.catz.*
import zio.interop.catz.implicits.{*, given}
import org.http4s.HttpRoutes
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.implicits.{*, given}
import org.http4s.blaze.server.*
import org.http4s.syntax.all.{*, given}
import scala.concurrent.ExecutionContext.global
import org.http4s.dsl.Http4sDsl
import sttp.tapir.*
import sttp.tapir.server.http4s.ztapir.ZHttp4sServerInterpreter

import org.pac4j.oidc.client.OidcClient
import org.pac4j.oidc.config.OidcConfiguration
import org.pac4j.core.authorization.generator.AuthorizationGenerator
import org.pac4j.core.context.WebContext
import org.pac4j.core.context.session.SessionStore
import org.pac4j.core.profile.UserProfile
import java.util.Optional
import org.pac4j.core.client.Clients
import org.pac4j.http4s.Http4sCacheSessionStore
import org.pac4j.http4s.DefaultHttpActionAdapter
import org.pac4j.http4s.SessionConfig
import org.http4s.ResponseCookie
import org.pac4j.http4s.CallbackService
import scala.concurrent.duration.{*, given}
import org.pac4j.http4s.{Http4sWebContext, *}
import org.pac4j.core.profile.CommonProfile
import org.http4s.server.Router

object Main extends ZIOAppDefault:
  type AppTask = RIO[ZEnv, *]
  protected val dsl: Http4sDsl[AppTask] = new Http4sDsl[AppTask] {}
  import dsl.*

  private val contextBuilder =
    (req: Request[AppTask], conf: org.pac4j.core.config.Config) =>
      new Http4sWebContext[AppTask](
        req,
        conf.getSessionStore,
        runtime.unsafeRun(_)
      )

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
    val clients = Clients("http://localhost:8080/callback", oidcClient())
    val config = org.pac4j.core.config.Config(clients)
    config.setHttpActionAdapter(DefaultHttpActionAdapter[AppTask]())
    config.setSessionStore(Http4sCacheSessionStore[AppTask]())
    config

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
    Some("/?defaulturlafterlogout"),
    destroySession = true
  )
  val centralLogoutService = LogoutService[AppTask](
    pac4jConfig,
    contextBuilder,
    defaultUrl = Some("http://localhost:8080/?defaulturlafterlogoutafteridp"),
    logoutUrlPattern = Some("http://localhost:8080/.*"),
    localLogout = false,
    destroySession = true,
    centralLogout = true
  )

  val filesService: HttpRoutes[AppTask] =
    ZHttp4sServerInterpreter()
      .from(
        List(
          fileGetServerEndpoint("pdb" / "app")(
            "app/target/vite/index.html"
          ),
          filesGetServerEndpoint("pdb")("app/target/vite")
        )
      )
      .toRoutes

  val authedProtectedPages: HttpRoutes[AppTask] =
    Session
      .sessionManagement[AppTask](sessionConfig)
      .compose(
        SecurityFilterMiddleware
          .securityFilter[AppTask](pac4jConfig, contextBuilder)
      ) {
        filesService.local(
          (req: ContextRequest[AppTask, List[CommonProfile]]) => req.req
        )
      }

  val root: HttpRoutes[AppTask] = HttpRoutes.of {
    case req @ GET -> Root / "callback" =>
      callbackService.callback(req)
    case req @ POST -> Root / "callback" =>
      callbackService.callback(req)
    case req @ GET -> Root / "logout" =>
      localLogoutService.logout(req)
    case req @ GET -> Root / "centralLogout" =>
      centralLogoutService.logout(req)
  }

  def serve: URIO[ZEnv, ExitCode] =
    BlazeServerBuilder[AppTask]
      .bindHttp(8080, "localhost")
      .withHttpApp(
        Router(
          "/mdr" -> authedProtectedPages,
          "/" -> (Session
            .sessionManagement[AppTask](sessionConfig)
            .apply) { root }
        ).orNotFound
      )
      .serve
      .compile
      .drain
      .fold(_ => ExitCode.failure, _ => ExitCode.success)

  override def run =
    for {
      _ <- serve
    } yield ()
