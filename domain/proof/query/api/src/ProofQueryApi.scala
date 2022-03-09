package mdr.pdb.proof.query
package api

import endpoints.Endpoints
import repo.ProofRepository
import fiftyforms.tapir.CustomTapir
import fiftyforms.tapir.InternalServerError

object ProofQueryApi extends CustomTapir:

  val forUser: ZServerEndpoint[ProofRepository, Any] =
    Endpoints.forUser.zServerLogic(osc =>
      ProofRepository
        .matching(ProofRepository.OfPerson(osc))
        .mapError(InternalServerError.fromThrowable)
    )
