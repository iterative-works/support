package works.iterative.server.http

import zio.*
import zio.interop.catz.*
import works.iterative.core.auth.service.AuthenticationService
import org.http4s.HttpRoutes

trait HttpServer:
    def serve[Env <: AuthenticationService](
        app: HttpApplication[Env],
        extraRoutes: HttpRoutes[RIO[Env, *]] = HttpRoutes.empty
    ): URIO[Env, Nothing]
end HttpServer

object HttpServer:
    def serve[Env <: AuthenticationService](
        app: HttpApplication[Env],
        extraRoutes: HttpRoutes[RIO[Env, *]] = HttpRoutes.empty
    ): URIO[Env & HttpServer, Nothing] =
        ZIO.serviceWithZIO[HttpServer](_.serve(app, extraRoutes))
end HttpServer
