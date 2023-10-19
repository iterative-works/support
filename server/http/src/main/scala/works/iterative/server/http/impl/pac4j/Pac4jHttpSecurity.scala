package works.iterative.server.http
package impl.pac4j

import cats.data.{Kleisli, OptionT}
import org.http4s.{AuthedRequest, HttpRoutes, Request, Response, ResponseCookie}
import org.http4s.server.{AuthMiddleware, Middleware}
import org.pac4j.core.config.Config
import org.pac4j.core.profile.CommonProfile
import org.pac4j.http4s.*
import cats.effect.Sync

import scala.concurrent.duration.given
import works.iterative.core.auth.CurrentUser
import org.http4s.dsl.Http4sDsl
import works.iterative.core.auth.UserId
import org.http4s.server.Router
import works.iterative.core.UserName
import works.iterative.core.auth.UserRole
import works.iterative.core.Email
import works.iterative.core.Avatar
import works.iterative.core.auth.*

trait HttpSecurity

class Pac4jHttpSecurity[F[_] <: AnyRef: Sync](
    config: Pac4jSecurityConfig,
    contextBuilder: (Request[F], Config) => Http4sWebContext[F],
    updateProfile: (CommonProfile, BasicProfile) => BasicProfile
) extends HttpSecurity:
  protected val dsl: Http4sDsl[F] = new Http4sDsl[F] {}
  import dsl.*

  private val sessionConfig = SessionConfig(
    cookieName = "session",
    mkCookie = ResponseCookie(_, _, path = Some("/")),
    secret = config.sessionSecret.getBytes.to(List),
    maxAge = 5.minutes
  )

  val pac4jConfig = Pac4jConfigFactory[F](config).build()

  val callbackService = CallbackService[F](pac4jConfig, contextBuilder)

  val localLogoutService = LogoutService[F](
    pac4jConfig,
    contextBuilder,
    config.logoutUrl,
    localLogout = true,
    destroySession = true
  )

  val centralLogoutService = LogoutService[F](
    pac4jConfig,
    contextBuilder,
    defaultUrl = config.logoutUrl,
    localLogout = true,
    destroySession = true,
    centralLogout = true
  )

  val sessionManagement = Session.sessionManagement[F](sessionConfig)
  val baseSecurityFilter = SecurityFilterMiddleware
    .securityFilter[F](
      pac4jConfig,
      contextBuilder,
      // TODO: this disables CSRF check, find out how to enable again
      authorizers = Some("none")
    )

  // TODO: factor this middleware out to make this Pac4J service general
  val currentUserSecurityFilter
      : Middleware[OptionT[F, *], AuthedRequest[F, CurrentUser], Response[
        F
      ], AuthedRequest[F, List[CommonProfile]], Response[F]] =
    service =>
      Kleisli { (r: AuthedRequest[F, List[CommonProfile]]) =>
        def loggedInUser(p: CommonProfile): CurrentUser =
          import scala.jdk.CollectionConverters.*
          CurrentUser(
            updateProfile(
              p,
              BasicProfile(
                UserId.unsafe(p.getUsername()),
                Option(p.getDisplayName()).flatMap(UserName(_).toOption),
                Option(p.getEmail()).flatMap(Email(_).toOption),
                Option(p.getPictureUrl()).flatMap(Avatar(_).toOption),
                Option(p.getRoles())
                  .map(_.asScala.toSet)
                  .getOrElse(Set.empty)
                  .flatMap(UserRole(_).toOption)
              )
            )
          )
        r.context match {
          case profile :: _ =>
            service(AuthedRequest(loggedInUser(profile), r.req))
          // TODO: Report error properly
          case _ => OptionT.none
        }
      }

  private val routes: HttpRoutes[F] =
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

  def route: HttpRoutes[F] =
    Router(s"${config.callbackBase}" -> sessionManagement(routes))

  def secure: AuthMiddleware[F, CurrentUser] =
    sessionManagement
      .compose(baseSecurityFilter)
      .compose(currentUserSecurityFilter)
