package mdr.pdb.app

import zio.*
import com.raquo.laminar.api.L.{*, given}
import com.raquo.waypoint.*
import org.scalajs.dom
import zio.json.{*, given}
import scala.scalajs.js
import mdr.pdb.*

object Routes:

  val layer: URLayer[AppConfig, Routes] = (Routes(_)).toLayer

  val router: URLayer[AppConfig, Router[Page]] = layer.project(_.router)

class Routes(appConfig: AppConfig):
  import Page.*
  import Routes.*

  given JsonDecoder[OsobniCislo] = JsonDecoder.string.map(OsobniCislo.apply)
  given JsonEncoder[OsobniCislo] = JsonEncoder.string.contramap(_.toString)
  given [V: JsonEncoder]: JsonEncoder[Titled[V]] =
    DeriveJsonEncoder.gen[Titled[V]]
  given [V: JsonDecoder]: JsonDecoder[Titled[V]] =
    DeriveJsonDecoder.gen[Titled[V]]
  given JsonEncoder[Page] = DeriveJsonEncoder.gen[Page]
  given JsonDecoder[Page] = DeriveJsonDecoder.gen[Page]

  val base = appConfig.baseUrl + "app"

  given router: Router[Page] = Router[Page](
    routes = List(
      Route.static(Page.homePage, root / endOfSegments, basePath = base),
      Route.static(
        Dashboard,
        root / "dashboard" / endOfSegments,
        basePath = base
      ),
      Route[Detail, String](
        encode = _.osobniCislo.value.toString,
        decode = osc => Detail(Titled(OsobniCislo(osc))),
        root / "osoba" / segment[String] / endOfSegments,
        basePath = base
      ),
      Route[DetailParametru, (String, String)](
        encode = p => (p.osobniCislo.value.toString, p.parametr.value),
        decode = p => DetailParametru(Titled(OsobniCislo(p._1)), Titled(p._2)),
        root / "osoba" / segment[String] / "parametr" / segment[
          String
        ] / endOfSegments,
        basePath = base
      ),
      Route[DetailKriteria, (String, String, String)](
        encode = p =>
          (
            p.osobniCislo.value.toString,
            p.parametr.value,
            p.kriterium.value.replaceAll("\\.", "--")
          ),
        decode = p =>
          DetailKriteria(
            Titled(OsobniCislo(p._1)),
            Titled(p._2),
            Titled(p._3.replaceAll("--", "."))
          ),
        root / "osoba" / segment[String] / "parametr" / segment[
          String
        ] / "kriterium" / segment[String] / endOfSegments,
        basePath = base
      ),
      Route[UpravDukazKriteria, (String, String, String)](
        encode = p =>
          (
            p.osobniCislo.value.toString,
            p.parametr.value,
            p.kriterium.value.replaceAll("\\.", "--")
          ),
        decode = p =>
          UpravDukazKriteria(
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
    routeFallback = url => NotFound(url),
    deserializeFallback = _ => Dashboard
  )(
    $popStateEvent = windowEvents.onPopState,
    owner = unsafeWindowOwner
  )
