package mdr.pdb.server
package blaze

import zio.*
import zio.interop.catz.*
import zio.interop.catz.implicits.{*, given}
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.HttpRoutes

object BlazeHttpServer:
  val layer: URLayer[BlazeServerConfig & HttpApplication, HttpServer] =
    (BlazeHttpServer(_, _)).toLayer[HttpServer]

case class BlazeHttpServer(
    config: BlazeServerConfig,
    httpApp: HttpApplication
) extends HttpServer:
  override def serve(): URIO[AppEnv, ExitCode] =
    for
      routes <- httpApp.routes()
      server <- BlazeServerBuilder[AppTask]
        .bindHttp(config.port, config.host)
        .withHttpApp(routes.orNotFound)
        .serve
        .compile
        .drain
        .fold(_ => ExitCode.failure, _ => ExitCode.success)
    yield server
