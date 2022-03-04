package mdr.pdb.server
package api

import zio.*
import zio.interop.catz.*
import zio.interop.catz.implicits.{*, given}
import org.http4s.AuthedRoutes
import mdr.pdb.api.Endpoints
import org.http4s.server.Router
import mdr.pdb.server.user.UserDirectory
import scala.util.control.NonFatal

class Routes():
  import CustomTapir.*
  import Endpoints.InternalServerError

  val alive: ZServerEndpoint[AppEnv, Any] =
    Endpoints.alive.zServerLogic(_ => ZIO.succeed("ok"))

  val users: ZServerEndpoint[AppEnv, Any] =
    Endpoints.users.zServerLogic(_ =>
      UserDirectory.list.mapError(InternalServerError.fromThrowable)
    )

  val serverEndpoints: List[ZServerEndpoint[AppEnv, Any]] = List(alive, users)

  val routes: AuthedRoutes[AppAuth, AppTask] =
    Router("pdb/api" -> CustomTapir.from(serverEndpoints).toRoutes).local(_.req)
