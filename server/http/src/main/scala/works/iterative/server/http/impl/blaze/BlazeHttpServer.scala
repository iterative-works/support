package works.iterative.server.http
package impl.blaze

import zio.*
import zio.interop.catz.*
import org.http4s.blaze.server.BlazeServerBuilder

class BlazeHttpServer(config: BlazeServerConfig) extends HttpServer:
  override def serve[Env](app: HttpApplication[Env]): URIO[Env, Nothing] =
    BlazeServerBuilder[RIO[Env, *]]
      .bindHttp(config.port, config.host)
      .serve
      .compile
      .drain
      .orDie *> ZIO.never

object BlazeHttpServer:
  val layer: RLayer[BlazeServerConfig, HttpServer] = ZLayer {
    for config <- ZIO.service[BlazeServerConfig]
    yield BlazeHttpServer(config)
  }
