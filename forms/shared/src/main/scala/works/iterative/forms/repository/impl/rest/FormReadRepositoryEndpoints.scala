package portaly.forms
package repository
package impl.rest

import works.iterative.tapir.CustomTapir.*
import works.iterative.tapir.endpoints.BaseEndpoint
import service.impl.rest.FormPersistenceCodecs.given

trait FormReadRepositoryEndpoints(base: BaseEndpoint):
    val load: Endpoint[Unit, (String, Option[String]), Unit, Option[Form], Any] = base.get
        .in("forms" / path[String]("id"))
        .in(query[Option[String]]("version"))
        .out(jsonBody[Option[Form]])
end FormReadRepositoryEndpoints
