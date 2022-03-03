package mdr.pdb.server
package blaze

import zio.*
import zio.interop.catz.*
import zio.interop.catz.implicits.{*, given}
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.HttpRoutes

object BlazeHttpServer {
  val layer: URLayer[BlazeServerConfig & HttpApplication, HttpServer] =
    val routesLayer = ZLayer
      .environment[HttpApplication]
      .flatMap(a => ZLayer.fromZIO(a.get.routes()))
    val blazeLayer = (BlazeHttpServer(_, _)).toLayer[HttpServer]
    (ZLayer.environment[BlazeServerConfig] ++ routesLayer) >>> blazeLayer
}

import BlazeHttpServer.*

case class BlazeHttpServer(
    config: BlazeServerConfig,
    httpApp: HttpRoutes[AppTask]
) extends HttpServer:
  override def serve(): UIO[ExitCode] =
    BlazeServerBuilder[AppTask]
      .bindHttp(config.port, config.host)
      .withHttpApp(httpApp.orNotFound)
      .serve
      .compile
      .drain
      .fold(_ => ExitCode.failure, _ => ExitCode.success)
      .provideEnvironment(ZEnvironment.default)
