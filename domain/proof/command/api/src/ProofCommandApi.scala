package mdr.pdb.proof.command
package api

import endpoints.Endpoints
import entity.ProofCommandBus
import fiftyforms.tapir.CustomTapir
import fiftyforms.tapir.InternalServerError

object ProofCommandApi extends CustomTapir:

  val submitCommand: ZServerEndpoint[ProofCommandBus, Any] =
    Endpoints.submitCommand.zServerLogic(cmd =>
      ProofCommandBus
        .submitCommand(cmd)
        .mapError(InternalServerError.fromThrowable)
    )
