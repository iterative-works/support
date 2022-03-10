package mdr.pdb.users.query
package api

import endpoints.Endpoints
import repo.UsersRepository
import fiftyforms.tapir.CustomTapir
import fiftyforms.tapir.InternalServerError

object UsersApi extends CustomTapir:

  val list: ZServerEndpoint[UsersRepository, Any] =
    Endpoints.matching.zServerLogic(criteria =>
      UsersRepository
        .matching(criteria)
        .mapError(InternalServerError.fromThrowable)
    )
