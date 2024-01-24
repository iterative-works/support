package works.iterative.app

import zio.*
import com.raquo.laminar.api.L.*
import com.raquo.waypoint.*
import zio.json.*
import works.iterative.app.Connector

class Routes[P: JsonCodec](
    baseUrl: String,
    connectors: List[Connector[?, P]],
    home: P,
    notFound: P,
    pageTitle: P => String
):
    val base = baseUrl + "app"

    val router: Router[P] = Router[P](
        routes = connectors.flatMap(_.routes(base)),
        serializePage = _.toJson,
        deserializePage = _.fromJson[P]
            .fold(s => throw IllegalStateException(s), identity),
        getPageTitle = pageTitle,
        routeFallback = _ => notFound,
        deserializeFallback = _ => home
    )(
        popStateEvents = windowEvents(_.onPopState),
        owner = unsafeWindowOwner
    )
end Routes
