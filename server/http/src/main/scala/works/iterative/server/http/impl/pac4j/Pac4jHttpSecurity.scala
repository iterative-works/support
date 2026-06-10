// PURPOSE: http4s middleware mounting Pac4j callback/logout routes and securing authenticated routes.
// PURPOSE: Cookie Path comes from Pac4jSecurityConfig.resolvedCookiePath; no BaseUri parameter.
package works.iterative.server.http
package impl.pac4j

import org.http4s.{HttpRoutes, Request, ResponseCookie}
import org.http4s.server.AuthMiddleware
import org.pac4j.core.config.Config
import org.pac4j.core.profile.CommonProfile
import org.pac4j.http4s.*
import org.pac4j.core.engine.DefaultSecurityLogic

import scala.concurrent.duration.given
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.pac4j.core.engine.SecurityGrantedAccessAdapter
import org.http4s.AuthedRoutes
import cats.effect.std.Dispatcher
import cats.effect.Sync

trait HttpSecurity

/** http4s middleware wrapping Pac4j's callback / logout routes and the security filter.
  *
  * **Breaking change vs. iw-support 0.1.15:** the `baseUri: BaseUri` constructor parameter has
  * been removed. The cookie `Path` attribute now comes from
  * [[Pac4jSecurityConfig.resolvedCookiePath]] (defaulting to `"/"`).
  */
class Pac4jHttpSecurity[F[_] <: AnyRef: Sync](
    config: Pac4jSecurityConfig,
    pac4jConfig: Config,
    dispatcher: Dispatcher[F]
) extends HttpSecurity:
    protected val dsl: Http4sDsl[F] = new Http4sDsl[F] {}
    import dsl.*

    // Updated contextBuilder to use new Http4sWebContext constructor
    val contextBuilder: Http4sContextBuilder[F] = (req: Request[F]) =>
        new Http4sWebContext[F](req, _ => dispatcher.unsafeRunSync(req.bodyText.compile.string))

    private val sessionConfig = SessionConfig(
        cookieName = "session",
        mkCookie = ResponseCookie(_, _, path = Some(config.resolvedCookiePath)),
        secret = config.sessionSecret.getBytes.to(List),
        maxAge = 5.minutes
    )

    // Updated CallbackService with defaultUrl parameter
    val callbackService = CallbackService[F](
        pac4jConfig,
        contextBuilder,
        defaultUrl = config.defaultUrl,
        renewSession = true,
        defaultClient = None
    )

    // Updated LogoutService with logoutUrlPattern parameter
    val localLogoutService = LogoutService[F](
        pac4jConfig,
        contextBuilder,
        defaultUrl = config.logoutUrl,
        logoutUrlPattern = config.logoutUrlPattern,
        localLogout = true,
        destroySession = true,
        centralLogout = false
    )

    // Updated LogoutService with logoutUrlPattern parameter
    val centralLogoutService = LogoutService[F](
        pac4jConfig,
        contextBuilder,
        defaultUrl = config.logoutUrl,
        logoutUrlPattern = config.logoutUrlPattern,
        localLogout = true,
        destroySession = true,
        centralLogout = true
    )

    val sessionManagement = Session.sessionManagement[F](sessionConfig)

    def baseSecurityFilter(
        authorizers: Option[String] = None,
        matchers: Option[String] = None,
        clients: Option[String] = None,
        securityGrantedAccessAdapter: Option[AuthedRoutes[
            List[CommonProfile],
            F
        ] => SecurityGrantedAccessAdapter] = None
    ) =
        // Create a SecurityLogic instead of using securityGrantedAccessAdapter directly
        val securityLogic = new DefaultSecurityLogic()

        // Updated call to securityFilter without securityGrantedAccessAdapter
        SecurityFilterMiddleware.securityFilter[F](
            pac4jConfig,
            contextBuilder,
            clients = clients,
            authorizers = authorizers,
            matchers = matchers,
            securityGrantedAccessAdapter = securityGrantedAccessAdapter.getOrElse(
                SecurityFilterMiddleware.defaultSecurityGrantedAccessAdapter
            ),
            securityLogic = securityLogic
        )
    end baseSecurityFilter

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
        clients: Option[String] = None,
        securityGrantedAccessAdapter: Option[AuthedRoutes[
            List[CommonProfile],
            F
        ] => SecurityGrantedAccessAdapter] = None
    ): AuthMiddleware[F, List[CommonProfile]] =
        sessionManagement.compose(baseSecurityFilter(
            authorizers = authorizers,
            matchers = matchers,
            clients = clients,
            securityGrantedAccessAdapter = securityGrantedAccessAdapter
        ))
end Pac4jHttpSecurity
