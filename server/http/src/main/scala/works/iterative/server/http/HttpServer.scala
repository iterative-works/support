package works.iterative.server.http

import zio.*

trait HttpServer:
  def serve[Env](app: HttpApplication[Env]): URIO[Env, Nothing]

object HttpServer:
  def serve[Env](app: HttpApplication[Env]): URIO[Env & HttpServer, Nothing] =
    ZIO.serviceWithZIO[HttpServer](_.serve(app))
