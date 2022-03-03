package mdr.pdb.app

import com.raquo.laminar.api.L.{*, given}
import com.raquo.waypoint.*
import org.scalajs.dom
import zio.json.{*, given}
import mdr.pdb.OsobniCislo

import scala.scalajs.js
import mdr.pdb.UserInfo
import mdr.pdb.Parameter
import mdr.pdb.ParameterCriteria
import mdr.pdb.app.Page.Titled

// enum is not working with Waypoints' SplitRender collectStatic
sealed abstract class Page(
    val id: String,
    val title: String,
    val parent: Option[Page]
) {
  val path: Vector[Page] =
    parent match
      case None    => Vector(this)
      case Some(p) => p.path :+ this

  val isRoot: Boolean = parent.isEmpty
}

object Page:

  case class Titled[V](value: V, title: Option[String] = None):
    val show: String = title.getOrElse(value.toString)

  case object Directory extends Page("directory", "Adresář", None)

  case object Dashboard extends Page("dashboard", "Přehled", Some(Directory))

  case class Detail(osobniCislo: Titled[OsobniCislo])
      extends Page("user", osobniCislo.show, Some(Directory))

  object Detail {
    def apply(o: UserInfo): Detail = Detail(
      Titled(o.personalNumber, Some(o.name))
    )
  }

  case class DetailParametru(
      osobniCislo: Titled[OsobniCislo],
      parametr: Titled[String]
  ) extends Page(
        "parameter",
        parametr.show,
        Some(Detail(osobniCislo))
      )

  object DetailParametru {
    def apply(o: UserInfo, p: Parameter): DetailParametru =
      DetailParametru(
        Titled(o.personalNumber, Some(o.name)),
        Titled(p.id, Some(p.name))
      )
  }

  case class DetailKriteria(
      osobniCislo: Titled[OsobniCislo],
      parametr: Titled[String],
      kriterium: Titled[String]
  ) extends Page(
        "criteria",
        kriterium.show,
        Some(DetailParametru(osobniCislo, parametr))
      )

  object DetailKriteria {
    def apply(o: UserInfo, p: Parameter, k: ParameterCriteria): DetailKriteria =
      DetailKriteria(
        Titled(o.personalNumber, Some(o.name)),
        Titled(p.id, Some(p.name)),
        Titled(k.id, Some(k.id))
      )
  }

  case class UpravDukazKriteria(
      osobniCislo: Titled[OsobniCislo],
      parametr: Titled[String],
      kriterium: Titled[String]
  ) extends Page(
        "addProof",
        "Důkaz",
        Some(DetailKriteria(osobniCislo, parametr, kriterium))
      )

  object UpravDukazKriteria {
    def apply(
        o: UserInfo,
        p: Parameter,
        k: ParameterCriteria
    ): UpravDukazKriteria =
      UpravDukazKriteria(
        Titled(o.personalNumber, Some(o.name)),
        Titled(p.id, Some(p.name)),
        Titled(k.id, Some(k.id))
      )
  }

  case class NotFound(url: String) extends Page("404", "404", Some(Directory))

  case class UnhandledError(
      errorName: Option[String],
      errorMessage: Option[String]
  ) extends Page("500", "Unexpected error", Some(Directory))

object Routes:
  given JsonDecoder[OsobniCislo] = JsonDecoder.string.map(OsobniCislo.apply)
  given JsonEncoder[OsobniCislo] = JsonEncoder.string.contramap(_.toString)
  given [V: JsonEncoder]: JsonEncoder[Titled[V]] =
    DeriveJsonEncoder.gen[Titled[V]]
  given [V: JsonDecoder]: JsonDecoder[Titled[V]] =
    DeriveJsonDecoder.gen[Titled[V]]
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
        encode = _.osobniCislo.value.toString,
        decode = osc => Page.Detail(Titled(OsobniCislo(osc))),
        root / "osoba" / segment[String] / endOfSegments,
        basePath = base
      ),
      Route[Page.DetailParametru, (String, String)](
        encode = p => (p.osobniCislo.value.toString, p.parametr.value),
        decode =
          p => Page.DetailParametru(Titled(OsobniCislo(p._1)), Titled(p._2)),
        root / "osoba" / segment[String] / "parametr" / segment[
          String
        ] / endOfSegments,
        basePath = base
      ),
      Route[Page.DetailKriteria, (String, String, String)](
        encode = p =>
          (
            p.osobniCislo.value.toString,
            p.parametr.value,
            p.kriterium.value.replaceAll("\\.", "--")
          ),
        decode = p =>
          Page.DetailKriteria(
            Titled(OsobniCislo(p._1)),
            Titled(p._2),
            Titled(p._3.replaceAll("--", "."))
          ),
        root / "osoba" / segment[String] / "parametr" / segment[
          String
        ] / "kriterium" / segment[String] / endOfSegments,
        basePath = base
      ),
      Route[Page.UpravDukazKriteria, (String, String, String)](
        encode = p =>
          (
            p.osobniCislo.value.toString,
            p.parametr.value,
            p.kriterium.value.replaceAll("\\.", "--")
          ),
        decode = p =>
          Page.UpravDukazKriteria(
            Titled(OsobniCislo(p._1)),
            Titled(p._2),
            Titled(p._3.replaceAll("--", "."))
          ),
        root / "osoba" / segment[String] / "parametr" / segment[
          String
        ] / "kriterium" / segment[String] / "edit" / endOfSegments,
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
