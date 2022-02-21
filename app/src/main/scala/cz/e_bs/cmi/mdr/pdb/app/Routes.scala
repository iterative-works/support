package cz.e_bs.cmi.mdr.pdb.app

import com.raquo.laminar.api.L.{*, given}
import com.raquo.waypoint.*
import org.scalajs.dom
import zio.json.{*, given}
import cz.e_bs.cmi.mdr.pdb.OsobniCislo

import scala.scalajs.js
import cz.e_bs.cmi.mdr.pdb.UserInfo
import cz.e_bs.cmi.mdr.pdb.Parameter
import cz.e_bs.cmi.mdr.pdb.ParameterCriteria

// enum is not working with Waypoints' SplitRender collectStatic
sealed abstract class Page(val title: String, val parent: Option[Page])

object Page:

  case object Directory extends Page("Adresář", None)

  case object Dashboard extends Page("Přehled", Some(Directory))

  // TODO: refactor to some "NamedParameter" concept, where the tuples value + title are better managed
  case class Detail(osobniCislo: OsobniCislo, jmenoOsoby: Option[String] = None)
      extends Page(jmenoOsoby.getOrElse("Detail osoby"), Some(Directory))

  object Detail {
    def apply(o: UserInfo): Detail = Detail(o.personalNumber, Some(o.name))
  }

  case class DetailParametru(
      osobniCislo: OsobniCislo,
      idParametru: String,
      jmenoOsoby: Option[String] = None,
      nazevParametru: Option[String] = None
  ) extends Page(
        nazevParametru.getOrElse("Detail parametru"),
        Some(Detail(osobniCislo, jmenoOsoby))
      )

  object DetailParametru {
    def apply(o: UserInfo, p: Parameter): DetailParametru =
      DetailParametru(o.personalNumber, p.id, Some(o.name), Some(p.name))
  }

  case class DetailKriteria(
      osobniCislo: OsobniCislo,
      idParametru: String,
      idKriteria: String,
      jmenoOsoby: Option[String] = None,
      nazevParametru: Option[String] = None,
      nazevKriteria: Option[String] = None
  ) extends Page(
        nazevKriteria.getOrElse("Detail kriteria"),
        Some(
          DetailParametru(osobniCislo, idParametru, jmenoOsoby, nazevParametru)
        )
      )

  object DetailKriteria {
    def apply(o: UserInfo, p: Parameter, k: ParameterCriteria): DetailKriteria =
      DetailKriteria(
        o.personalNumber,
        p.id,
        k.id,
        Some(o.name),
        Some(p.name),
        Some(k.id)
      )
  }

  case class NotFound(url: String) extends Page("404", Some(Directory))

  case class UnhandledError(
      errorName: Option[String],
      errorMessage: Option[String]
  ) extends Page("Unexpected error", Some(Directory))

object Routes:
  given JsonDecoder[OsobniCislo] = JsonDecoder.string.map(OsobniCislo.apply)
  given JsonEncoder[OsobniCislo] = JsonEncoder.string.contramap(_.toString)
  given JsonEncoder[Page] = DeriveJsonEncoder.gen[Page]
  given JsonDecoder[Page] = DeriveJsonDecoder.gen[Page]

  val base =
    js.`import`.meta.env.BASE_URL
      .asInstanceOf[String] + "app"

  val homePage: Page = Page.Directory

  given router: Router[Page] = Router[Page](
    routes = List(
      Route.static(homePage, root / endOfSegments, basePath = base),
      Route.static(
        Page.Dashboard,
        root / "dashboard" / endOfSegments,
        basePath = base
      ),
      Route[Page.Detail, String](
        encode = _.osobniCislo.toString,
        decode = osc => Page.Detail(OsobniCislo(osc)),
        root / "osoba" / segment[String] / endOfSegments,
        basePath = base
      ),
      Route[Page.DetailParametru, (String, String)](
        encode = p => (p.osobniCislo.toString, p.idParametru),
        decode = p => Page.DetailParametru(OsobniCislo(p._1), p._2),
        root / "osoba" / segment[String] / "parametr" / segment[
          String
        ] / endOfSegments,
        basePath = base
      ),
      Route[Page.DetailKriteria, (String, String, String)](
        encode = p =>
          (
            p.osobniCislo.toString,
            p.idParametru,
            p.idKriteria.replaceAll("\\.", "--")
          ),
        decode = p =>
          Page.DetailKriteria(
            OsobniCislo(p._1),
            p._2,
            p._3.replaceAll("--", ".")
          ),
        root / "osoba" / segment[String] / "parametr" / segment[
          String
        ] / "kriterium" / segment[String] / endOfSegments,
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
