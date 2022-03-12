package mdr.pdb.proof.query
package api

import endpoints.Endpoints
import repo.ProofRepository
import works.iterative.tapir.CustomTapir
import works.iterative.tapir.InternalServerError

object ProofQueryApi extends CustomTapir:

  val forUser: ZServerEndpoint[ProofRepository, Any] =
    Endpoints.forUser.zServerLogic(osc =>
      ProofRepository
        .matching(ProofRepository.OfPerson(osc))
        .mapError(InternalServerError.fromThrowable)
    )
