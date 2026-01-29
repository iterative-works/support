package works.iterative.app

import zio.*
import com.raquo.laminar.api.L.*
import com.raquo.waypoint.*
import zio.json.*

// scalafix:off DisableSyntax.throw
// Waypoint Router API requires throwing for deserialization errors
class Routes[P: JsonCodec](
    base: String,
    connectors: List[Connector[?, P]],
    home: P,
    notFound: P,
    pageTitle: P => String
):
    val router: Router[P] = Router[P](
        routes = connectors.flatMap(_.routes(base)),
        serializePage = _.toJson,
        deserializePage = _.fromJson[P]
            .fold(s => throw IllegalStateException(s), identity),
        getPageTitle = pageTitle,
        routeFallback = _ => notFound,
        deserializeFallback = _ => home,
        popStateEvents = windowEvents(_.onPopState),
        owner = unsafeWindowOwner
    )
end Routes
// scalafix:on DisableSyntax.throw
