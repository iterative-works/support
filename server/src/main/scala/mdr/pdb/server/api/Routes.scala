package mdr.pdb.server
package api

import zio.*
import zio.interop.catz.*
import zio.interop.catz.implicits.{*, given}
import org.http4s.AuthedRoutes
import org.http4s.server.Router
import scala.util.control.NonFatal
import mdr.pdb.endpoints.Endpoints
import mdr.pdb.users.query.api.UsersApi
import mdr.pdb.proof.query.api.ProofQueryApi
import mdr.pdb.proof.command.api.ProofCommandApi

object Routes extends CustomTapir:
  import fiftyforms.tapir.InternalServerError

  val alive: ZServerEndpoint[AppEnv, Any] =
    Endpoints.alive.zServerLogic(_ => ZIO.succeed("ok"))

  val serverEndpoints: List[ZServerEndpoint[AppEnv, Any]] =
    List(
      alive,
      UsersApi.list.widen[AppEnv],
      ProofQueryApi.forUser.widen[AppEnv],
      ProofCommandApi.submitCommand.widen[AppEnv]
    )

  val routes: AuthedRoutes[AppAuth, AppTask] =
    Router("pdb/api" -> from(serverEndpoints).toRoutes).local(_.req)
