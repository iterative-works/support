package mdr.pdb.server
package api

import zio.*
import zio.interop.catz.*
import zio.interop.catz.implicits.{*, given}
import org.http4s.AuthedRoutes
import mdr.pdb.api.Endpoints
import org.http4s.server.Router

class Routes():
  import CustomTapir.*

  val alive: ZServerEndpoint[AppEnv, Any] =
    Endpoints.alive.zServerLogic(_ => ZIO.succeed("ok"))

  val serverEndpoints: List[ZServerEndpoint[AppEnv, Any]] = List(alive)

  val routes: AuthedRoutes[AppAuth, AppTask] =
    Router("pdb/api" -> CustomTapir.from(serverEndpoints).toRoutes).local(_.req)
