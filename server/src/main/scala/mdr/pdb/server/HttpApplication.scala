package mdr.pdb.server

import zio.*

import zio.interop.catz.*
import zio.interop.catz.implicits.{*, given}

import org.http4s.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.*
import org.http4s.implicits.{*, given}
import org.http4s.server.Router
import org.http4s.syntax.all.{*, given}

import org.pac4j.http4s.*
import org.pac4j.core.profile.CommonProfile

trait HttpApplication {
  def routes(): UIO[HttpRoutes[AppTask]]
}

object HttpApplicationLive {
  val layer: URLayer[AppConfig & HttpSecurity, HttpApplication] =
    (HttpApplicationLive(_, _)).toLayer[HttpApplication]
}

case class HttpApplicationLive(
    config: AppConfig,
    security: HttpSecurity
) extends HttpApplication:
  import dsl.*

  val files = static.Routes(config)

  def httpApp(appPath: String): HttpRoutes[AppTask] =
    Router(
      security.route,
      "/mdr" -> security.secure(files.routes)
    )

  override def routes(): UIO[HttpRoutes[AppTask]] =
    ZIO.succeed(httpApp(config.appPath))
