package mdr.pdb.server

import zio.*
import zio.interop.catz.*
import zio.interop.catz.implicits.{*, given}
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.HttpRoutes

trait HttpServer:
  def serve(): UIO[ExitCode]

object BlazeHttpServer {
  import zio.config.*

  case class BlazeServerConf(host: String, port: Int)

  val blazeServerConfig: ConfigDescriptor[BlazeServerConf] =
    import ConfigDescriptor.*
    nested("BLAZE")(
      string("HOST").default("localhost") zip int("PORT").default(8080)
    ).to[BlazeServerConf]

  val layer: RLayer[System & HttpApplication, HttpServer] =
    val configLayer = ZConfig.fromSystemEnv(
      blazeServerConfig,
      keyDelimiter = Some('_'),
      valueDelimiter = Some(',')
    )
    val routesLayer = ZLayer
      .environment[HttpApplication]
      .flatMap(a => ZLayer.fromZIO(a.get.routes()))
    val blazeLayer = (BlazeHttpServer(_, _)).toLayer[HttpServer]
    (configLayer ++ routesLayer) >>> blazeLayer
}

import BlazeHttpServer.*

case class BlazeHttpServer(
    config: BlazeServerConf,
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
