package portaly.forms.service.impl.rest

import works.iterative.tapir.CustomTapir.*
import works.iterative.tapir.endpoints.BaseEndpoint

trait ViesEndpoints(base: BaseEndpoint):
    object vies:
        val check: Endpoint[Unit, (String, String), Unit, Option[Boolean], Any] =
            base.get
                .in("vies" / path[String]("country") / path[String]("vatId"))
                .out(jsonBody[Option[Boolean]])
    end vies
end ViesEndpoints
