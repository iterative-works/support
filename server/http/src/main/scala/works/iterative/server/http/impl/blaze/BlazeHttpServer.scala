package works.iterative.server.http
package impl.blaze

import zio.*
import zio.interop.catz.*
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.*
import cats.*
import zio.interop.catz.*
import works.iterative.tapir.BaseUri
import org.http4s.server.Router
import org.http4s.server.websocket.WebSocketBuilder2

class BlazeHttpServer(config: BlazeServerConfig, baseUri: BaseUri) extends HttpServer:
    def withBaseUri[Env](routes: HttpRoutes[RIO[Env, *]]): HttpRoutes[RIO[Env, *]] =
        baseUri.value match
            case Some(u) => Router(u.toString -> routes)
            case _       => routes

    override def build[Env](
        httpApp: WebSocketBuilder2[RIO[Env, *]] => HttpRoutes[RIO[Env, *]]
    ): RIO[Env & Scope, org.http4s.server.Server] =
        for
            _ <- ZIO.log(s"Starting Blaze server with config: $config")
            executor <- ZIO.executor
            server <- BlazeServerBuilder[RIO[Env, *]]
                .withExecutionContext(executor.asExecutionContext)
                .bindHttp(config.port, config.host)
                .withResponseHeaderTimeout(config.responseHeaderTimeout.asScala)
                .withIdleTimeout(config.idleTimeout.asScala)
                .withHttpWebSocketApp(wsb => withBaseUri(httpApp(wsb)).orNotFound)
                .resource
                .toScopedZIO
        yield server
    end build

    override def serve[Env](
        httpApp: WebSocketBuilder2[RIO[Env, *]] => HttpRoutes[RIO[Env, *]]
    ): URIO[Env, Nothing] =
        for
            _ <- ZIO.log(s"Starting Blaze server with config: $config")
            executor <- ZIO.executor
            _ <- BlazeServerBuilder[RIO[Env, *]]
                .withExecutionContext(executor.asExecutionContext)
                .bindHttp(config.port, config.host)
                .withResponseHeaderTimeout(config.responseHeaderTimeout.asScala)
                .withIdleTimeout(config.idleTimeout.asScala)
                .withHttpWebSocketApp(wsb => withBaseUri(httpApp(wsb)).orNotFound)
                .serve
                .compile
                .drain
                .orDie
            result <- ZIO.never
        yield result
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
