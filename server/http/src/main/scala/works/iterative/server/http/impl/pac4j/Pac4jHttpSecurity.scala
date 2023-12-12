package works.iterative.server.http
package impl.pac4j

import zio.*
import zio.interop.catz.*
import org.http4s.{HttpRoutes, Request, Response, ResponseCookie}
import org.http4s.server.AuthMiddleware
import org.pac4j.core.config.Config
import org.pac4j.core.profile.CommonProfile
import org.pac4j.http4s.*

import scala.concurrent.duration.given
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import works.iterative.tapir.BaseUri
import org.pac4j.core.engine.SecurityGrantedAccessAdapter
import org.pac4j.core.context.session.SessionStore
import org.pac4j.core.profile.UserProfile
import org.pac4j.core.context.WebContext
import java.{util as ju}
import org.http4s.ContextRequest
import org.pac4j.core.profile.ProfileManager

trait HttpSecurity

class Pac4jHttpSecurity[Env](
    baseUri: BaseUri,
    config: Pac4jSecurityConfig,
    pac4jConfig: Config
)(using runtime: Runtime[Env]) extends HttpSecurity:
    type AppTask[A] = RIO[Env, A]
    protected val dsl: Http4sDsl[AppTask] = new Http4sDsl[AppTask] {}
    import dsl.*

    val contextBuilder = (req: Request[AppTask], conf: Config) =>
        new Http4sWebContext[AppTask](
            req,
            conf.getSessionStore,
            t =>
                Unsafe.unsafely:
                    runtime.unsafe.run(t).getOrThrowFiberFailure()
        )

    private val sessionConfig = SessionConfig(
        cookieName = "session",
        mkCookie = ResponseCookie(_, _, path = Some(baseUri.value.fold("/")(_.toString))),
        secret = config.sessionSecret.getBytes.to(List),
        maxAge = 5.minutes
    )

    val callbackService = CallbackService[AppTask](pac4jConfig, contextBuilder)

    val localLogoutService = LogoutService[AppTask](
        pac4jConfig,
        contextBuilder,
        config.logoutUrl,
        localLogout = true,
        destroySession = true
    )

    val centralLogoutService = LogoutService[AppTask](
        pac4jConfig,
        contextBuilder,
        defaultUrl = config.logoutUrl,
        localLogout = true,
        destroySession = true,
        centralLogout = true
    )

    val sessionManagement = Session.sessionManagement[AppTask](sessionConfig)
    def baseSecurityFilter(
        authorizers: Option[String] = None,
        matchers: Option[String] = None,
        clients: Option[String] = None
    ) = SecurityFilterMiddleware
        .securityFilter[AppTask](
            pac4jConfig,
            contextBuilder,
            // TODO: this disables CSRF check, find out how to enable again
            authorizers = authorizers,
            matchers = matchers,
            clients = clients,
            securityGrantedAccessAdapter = service =>
                new SecurityGrantedAccessAdapter:
                    import scala.jdk.CollectionConverters.*
                    override def adapt(
                        context: WebContext,
                        sessionStore: SessionStore,
                        profiles: ju.Collection[UserProfile],
                        parameters: AnyRef*
                    ): AnyRef =
                        // Get profiles from context, if any, instead of just the empty ones
                        val manager = new ProfileManager(context, sessionStore)
                        manager.setConfig(pac4jConfig)
                        val sessionProfiles = manager.getProfiles()
                        service.orNotFound(
                            ContextRequest[AppTask, List[CommonProfile]](
                                sessionProfiles.asScala.toList.map(_.asInstanceOf[CommonProfile]),
                                context.asInstanceOf[Http4sWebContext[AppTask]].getRequest
                            )
                        )
                    end adapt
        )

    private val routes: HttpRoutes[AppTask] =
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

    def route: HttpRoutes[AppTask] =
        Router(s"${config.callbackBase}" -> sessionManagement(routes))

    def secure(
        authorizers: Option[String] = None,
        matchers: Option[String] = None,
        clients: Option[String] = None
    ): AuthMiddleware[AppTask, List[CommonProfile]] =
        sessionManagement.compose(baseSecurityFilter(
            authorizers = authorizers,
            matchers = matchers,
            clients = clients
        ))
end Pac4jHttpSecurity
