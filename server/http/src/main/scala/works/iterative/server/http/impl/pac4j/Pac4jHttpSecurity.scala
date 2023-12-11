package works.iterative.server.http
package impl.pac4j

import org.http4s.{HttpRoutes, Request, Response, ResponseCookie}
import org.http4s.server.AuthMiddleware
import org.pac4j.core.config.Config
import org.pac4j.core.profile.CommonProfile
import org.pac4j.http4s.*
import cats.effect.Sync

import scala.concurrent.duration.given
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import works.iterative.tapir.BaseUri

trait HttpSecurity

class Pac4jHttpSecurity[F[_] <: AnyRef: Sync](
    baseUri: BaseUri,
    config: Pac4jSecurityConfig,
    contextBuilder: (Request[F], Config) => Http4sWebContext[F],
    updateConfig: Config => Config = identity
) extends HttpSecurity:
    protected val dsl: Http4sDsl[F] = new Http4sDsl[F] {}
    import dsl.*

    private val sessionConfig = SessionConfig(
        cookieName = "session",
        mkCookie = ResponseCookie(_, _, path = baseUri.value.map(_.toString)),
        secret = config.sessionSecret.getBytes.to(List),
        maxAge = 5.minutes
    )

    val pac4jConfig = updateConfig(Pac4jConfigFactory[F](config).build())

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
    def baseSecurityFilter(
        authorizers: Option[String] = None,
        matchers: Option[String] = None,
        clients: Option[String] = None
    ) = SecurityFilterMiddleware
        .securityFilter[F](
            pac4jConfig,
            contextBuilder,
            // TODO: this disables CSRF check, find out how to enable again
            authorizers = authorizers,
            matchers = clients,
            clients = clients
        )

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

    def secure(
        authorizers: Option[String] = None,
        matchers: Option[String] = None,
        clients: Option[String] = None
    ): AuthMiddleware[F, List[CommonProfile]] =
        sessionManagement.compose(baseSecurityFilter(authorizers, matchers, clients))
end Pac4jHttpSecurity
