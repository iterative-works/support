package mdr.pdb
package proof
package command
package endpoints

import fiftyforms.tapir.CustomTapir
import mdr.pdb.proof.command.codecs.Codecs
import fiftyforms.tapir.ServerError
import zio.json.JsonCodec
import zio.json.DeriveJsonCodec
import sttp.model.StatusCode

object Endpoints
    extends mdr.pdb.endpoints.Endpoints
    with CustomTapir
    with Codecs:

  val submitCommand: Endpoint[Unit, Command, ServerError, Unit, Any] =
    endpoint
      .in("command")
      .in("proof")
      .post
      .in(jsonBody[Command])
      .out(statusCode(StatusCode.Accepted))
      .errorOut(jsonBody[ServerError])
