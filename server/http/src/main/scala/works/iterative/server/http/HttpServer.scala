package works.iterative.server.http

import zio.*
import org.http4s.HttpRoutes
import org.http4s.server.websocket.WebSocketBuilder2

trait HttpServer:
    def serve[Env](app: WebSocketBuilder2[RIO[Env, *]] => HttpRoutes[RIO[Env, *]])
        : URIO[Env, Nothing]
end HttpServer

object HttpServer:
    def serve[Env](
        app: WebSocketBuilder2[RIO[Env, *]] => HttpRoutes[RIO[Env, *]]
    ): URIO[Env & HttpServer, Nothing] =
        ZIO.serviceWithZIO[HttpServer](_.serve(app))
end HttpServer
