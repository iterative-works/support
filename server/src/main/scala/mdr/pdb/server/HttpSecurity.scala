package mdr.pdb.server

import org.http4s.HttpRoutes
import org.http4s.server.AuthMiddleware

trait HttpSecurity:
  def route: (String, HttpRoutes[AppTask])
  def secure: AuthMiddleware[AppTask, AppAuth]
