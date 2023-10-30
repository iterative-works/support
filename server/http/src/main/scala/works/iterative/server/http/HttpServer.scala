package works.iterative.server.http

import zio.*
import works.iterative.core.auth.service.AuthenticationService

trait HttpServer:
  def serve[Env <: AuthenticationService](
      app: HttpApplication[Env]
  ): URIO[Env, Nothing]

object HttpServer:
  def serve[Env <: AuthenticationService](
      app: HttpApplication[Env]
  ): URIO[Env & HttpServer, Nothing] =
    ZIO.serviceWithZIO[HttpServer](_.serve(app))
