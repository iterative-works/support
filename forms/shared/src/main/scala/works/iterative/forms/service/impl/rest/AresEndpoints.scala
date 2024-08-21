package portaly.forms.service.impl.rest

import works.iterative.tapir.CustomTapir.*
import works.iterative.tapir.endpoints.BaseEndpoint
import works.iterative.core.czech.ICO
import portaly.forms.service.impl.Ares.EkonomickySubjekt
import works.iterative.tapir.codecs.Codecs.given

trait AresEndpoints(base: BaseEndpoint):
    val ares: Endpoint[Unit, ICO, Unit, Option[EkonomickySubjekt], Any] = base.get
        .in("ares" / path[ICO]("ico"))
        .out(jsonBody[Option[EkonomickySubjekt]])
end AresEndpoints
