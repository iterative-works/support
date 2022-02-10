package cz.e_bs.cmi.mdr.pdb.app

import com.raquo.laminar.api.L.{*, given}
import com.raquo.waypoint.*
import zio.json.{*, given}
import scala.scalajs.js

// enum is not working with Waypoints' SplitRender collectStatic
sealed abstract class Page(val title: String)

object Page:
  case object Directory extends Page("Directory")
  case object Dashboard extends Page("Dashboard")
  case class Detail(osobniCislo: String) extends Page("Detail")

object Routes:
  given JsonEncoder[Page] = DeriveJsonEncoder.gen[Page]
  given JsonDecoder[Page] = DeriveJsonDecoder.gen[Page]

  val base =
    js.`import`.meta.env.BASE_URL
      .asInstanceOf[String]
      .init // Drop the ending slash

  val router = Router[Page](
    routes = List(
      Route.static(Page.Directory, root / endOfSegments, basePath = base),
      Route.static(
        Page.Dashboard,
        root / "dashboard" / endOfSegments,
        basePath = base
      ),
      Route[Page.Detail, String](
        encode = _.osobniCislo,
        decode = Page.Detail(_),
        root / "osoba" / segment[String] / endOfSegments,
        basePath = base
      )
    ),
    serializePage = _.toJson,
    deserializePage = _.fromJson[Page]
      .fold(s => throw IllegalStateException(s), identity),
    getPageTitle = _.title,
    routeFallback = _ => Page.Dashboard,
    deserializeFallback = _ => Page.Dashboard
  )(
    $popStateEvent = windowEvents.onPopState,
    owner = unsafeWindowOwner
  )
