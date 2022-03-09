package mdr.pdb.users.query
package endpoints

import fiftyforms.tapir.CustomTapir
import fiftyforms.tapir.ServerError
import mdr.pdb.users.query.codecs.Codecs

object Endpoints
    extends mdr.pdb.endpoints.Endpoints
    with CustomTapir
    with Codecs:

  val list: Endpoint[Unit, Unit, ServerError, List[UserInfo], Any] =
    endpoint
      .in("users")
      .out(jsonBody[List[UserInfo]])
      .errorOut(jsonBody[ServerError])
