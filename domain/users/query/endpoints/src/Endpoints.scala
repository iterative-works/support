package mdr.pdb.users.query
package endpoints

import works.iterative.tapir.CustomTapir
import works.iterative.tapir.ServerError
import mdr.pdb.users.query.codecs.Codecs

object Endpoints
    extends mdr.pdb.endpoints.Endpoints
    with CustomTapir
    with Codecs:

  val matching: Endpoint[Unit, Criteria, ServerError, List[UserInfo], Any] =
    endpoint
      .in("users")
      .post
      .in(jsonBody[Criteria])
      .out(jsonBody[List[UserInfo]])
      .errorOut(jsonBody[ServerError])
