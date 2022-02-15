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

  case class Detail(osobniCislo: String, jmenoOsoby: Option[String] = None)
      extends Page(jmenoOsoby.getOrElse("Detail"), Some(Directory))

  object Detail {
    def apply(o: Osoba): Detail = Detail(o.osobniCislo, Some(o.jmeno))
  }

  case class DetailParametru(
      osobniCislo: String,
      idParametru: String,
      jmenoOsoby: Option[String] = None,
      nazevParametru: Option[String] = None
  ) extends Page(
        jmenoOsoby.getOrElse("Detail parametru"),
        Some(Detail(osobniCislo, jmenoOsoby))
      )

  object DetailParametru {
    def apply(o: Osoba, p: Parametr): DetailParametru =
      DetailParametru(o.osobniCislo, p.id, Some(o.jmeno), Some(p.nazev))
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

  val homePage: Page = Page.Directory

  val router = Router[Page](
    routes = List(
      Route.static(homePage, root / endOfSegments, basePath = base),
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
      ),
      Route[Page.DetailParametru, (String, String)](
        encode = p => (p.osobniCislo, p.idParametru),
        decode = p => Page.DetailParametru(p._1, p._2),
        root / "osoba" / segment[String] / "parametr" / segment[
          String
        ] / endOfSegments,
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
