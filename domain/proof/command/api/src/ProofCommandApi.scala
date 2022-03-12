package mdr.pdb.proof.command
package api

import endpoints.Endpoints
import entity.ProofCommandBus
import works.iterative.tapir.CustomTapir
import works.iterative.tapir.InternalServerError

object ProofCommandApi extends CustomTapir:

  val submitCommand: ZServerEndpoint[ProofCommandBus, Any] =
    Endpoints.submitCommand.zServerLogic(cmd =>
      ProofCommandBus
        .submitCommand(cmd)
        .mapError(InternalServerError.fromThrowable)
    )
