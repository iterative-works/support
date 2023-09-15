package works.iterative.server.http
package impl.blaze

import zio.*
import zio.interop.catz.*
import org.http4s.blaze.server.BlazeServerBuilder
import works.iterative.tapir.Http4sCustomTapir
import org.http4s.HttpRoutes

class BlazeHttpServer(config: BlazeServerConfig) extends HttpServer:
  override def serve[Env](app: HttpApplication[Env]): URIO[Env, Nothing] =
    type AppEnv[A] = RIO[Env, A]
    val interpreter = new Http4sCustomTapir[Env] {}
    val routes: HttpRoutes[AppEnv] = interpreter.from(app.endpoints).toRoutes
    BlazeServerBuilder[AppEnv]
      .bindHttp(config.port, config.host)
      .withHttpApp(routes.orNotFound)
      .serve
      .compile
      .drain
      .orDie *> ZIO.never

object BlazeHttpServer:
  val layer: RLayer[BlazeServerConfig, HttpServer] = ZLayer {
    for config <- ZIO.service[BlazeServerConfig]
    yield BlazeHttpServer(config)
  }
