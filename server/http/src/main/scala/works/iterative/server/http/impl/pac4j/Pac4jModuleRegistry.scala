package works.iterative.server.http
package impl.pac4j

import zio.*
import org.pac4j.core.profile.CommonProfile
import org.http4s.*
import cats.data.*
import org.http4s.server.Router
import zio.interop.catz.*
import cats.syntax.all.*

trait Pac4jModuleRegistry[R, U] extends ModuleRegistry[R]:
    def pac4jSecurity: Pac4jHttpSecurity[[A] =>> RIO[R, A]]
    def profileToUser(profile: List[CommonProfile]): Option[U]
    def clients: Option[String] = None

    protected def wrapModule(
        protectedPath: String,
        module: AuthedZIOWebModule[R, U]
    ): ZIOWebModule[R] =
        new ZIOWebModule[R]:
            override def routes =
                // Authenticated routes, secured by pac4j
                val aroutes: HttpRoutes[WebTask] = pac4jSecurity.secure(clients = clients).compose(
                    (service: AuthedRoutes[U, [A] =>> RIO[R, A]]) =>
                        Kleisli((req: AuthedRequest[[A] =>> RIO[R, A], List[CommonProfile]]) =>
                            profileToUser(req.context) match
                                case Some(context) => service.run(ContextRequest(context, req.req))
                                case _             => OptionT.none
                        )
                )(module.authedRoutes)

                // Composed with public routes
                Router(protectedPath -> aroutes) <+> module.publicRoutes
            end routes

    // Optional helper method to combine multiple authenticated modules
    protected def wrapModules(
        protectedPath: String,
        modules: AuthedZIOWebModule[R, U]*
    ): ZIOWebModule[R] =
        wrapModule(protectedPath, AuthedZIOWebModule.combine(modules*))
end Pac4jModuleRegistry
