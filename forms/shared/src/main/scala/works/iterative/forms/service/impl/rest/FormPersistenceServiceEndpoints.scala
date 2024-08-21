package portaly.forms
package service
package impl
package rest

import works.iterative.tapir.CustomTapir.*
import works.iterative.tapir.endpoints.BaseEndpoint
import FormPersistenceCodecs.given
import works.iterative.tapir.codecs.Codecs.given
import sttp.model.StatusCode

trait FormPersistenceServiceEndpoints(base: BaseEndpoint):
    val put =
        base.put.in("forms" / "drafts" / path[FormIdent]("key")).in(jsonBody[SavedForm]).out(
            statusCode(StatusCode.Accepted)
        )

    val get =
        base.get.in("forms" / "drafts" / path[FormIdent]("key")).out(jsonBody[Option[SavedForm]])

    val delete =
        base.delete.in("forms" / "drafts" / path[FormIdent]("key")).out(statusCode(
            StatusCode.Accepted
        ))
end FormPersistenceServiceEndpoints
