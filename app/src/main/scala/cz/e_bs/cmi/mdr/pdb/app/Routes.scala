package cz.e_bs.cmi.mdr.pdb.app

import com.raquo.laminar.api.L.{*, given}
import com.raquo.waypoint.*
import org.scalajs.dom
import zio.json.{*, given}

import scala.scalajs.js

// enum is not working with Waypoints' SplitRender collectStatic
sealed abstract class Page(val title: String, val parent: Option[Page])

object Page:
  case object Directory extends Page("Directory", None)
  case object Dashboard extends Page("Dashboard", Some(Directory))
  case class Detail(osobniCislo: String, name: Option[String] = None)
      extends Page(name.getOrElse("Detail"), Some(Directory))
  object Detail {
    def apply(o: Osoba): Detail = Detail(o.osobniCislo, Some(o.jmeno))
  }
  case class NotFound(url: String) extends Page("404", Some(Directory))
  case class UnhandledError(
      errorName: Option[String],
      errorMessage: Option[String]
  ) extends Page("Unexpected error", Some(Directory))

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
    routeFallback = url => Page.NotFound(url),
    deserializeFallback = _ => Page.Dashboard
  )(
    $popStateEvent = windowEvents.onPopState,
    owner = unsafeWindowOwner
  )

  // TODO: evaluate dangers of a global router in a SPA
  def navigateTo(page: Page)(using router: Router[Page]): Binder[HtmlElement] =
    Binder { el =>

      val isLinkElement = el.ref.isInstanceOf[dom.html.Anchor]

      if (isLinkElement) {
        el.amend(href(router.absoluteUrlForPage(page)))
      }

      // If element is a link and user is holding a modifier while clicking:
      //  - Do nothing, browser will open the URL in new tab / window / etc. depending on the modifier key
      // Otherwise:
      //  - Perform regular pushState transition
      (onClick
        .filter(ev =>
          !(isLinkElement && (ev.ctrlKey || ev.metaKey || ev.shiftKey || ev.altKey))
        )
        .preventDefault
        --> (_ => router.pushState(page))).bind(el)
    }
