package works.iterative.server.http
package impl.blaze

import zio.*
import zio.interop.catz.*
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.*
import cats.*
import works.iterative.tapir.BaseUri
import org.http4s.server.Router
import org.http4s.server.websocket.WebSocketBuilder2

class BlazeHttpServer(config: BlazeServerConfig, baseUri: BaseUri) extends HttpServer:
    override def serve[Env](
        httpApp: WebSocketBuilder2[RIO[Env, *]] => HttpRoutes[RIO[Env, *]]
    ): URIO[Env, Nothing] =
        def withBaseUri(routes: HttpRoutes[RIO[Env, *]]): HttpRoutes[RIO[Env, *]] =
            baseUri.value match
                case Some(u) => Router(u.toString -> routes)
                case _       => routes

        ZIO.log(s"Starting Blaze server with config: $config") *>
            BlazeServerBuilder[RIO[Env, *]]
                .bindHttp(config.port, config.host)
                .withResponseHeaderTimeout(config.responseHeaderTimeout.asScala)
                .withIdleTimeout(config.idleTimeout.asScala)
                .withHttpWebSocketApp(wsb => withBaseUri(httpApp(wsb)).orNotFound)
                .serve
                .compile
                .drain
                .orDie *> ZIO.never
    end serve
end BlazeHttpServer

object BlazeHttpServer:
    val layer: TaskLayer[HttpServer] =
        ZLayer {
            for
                config <- ZIO.config(BlazeServerConfig.config)
                baseUri <- ZIO.config(BaseUri.config)
            yield BlazeHttpServer(config, baseUri)
        }
end BlazeHttpServer
