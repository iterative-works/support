package cz.e_bs.cmi.mdr.pdb.app.pages

import com.raquo.laminar.api.L.{*, given}
import com.raquo.domtypes.generic.codecs.StringAsIsCodec
import cz.e_bs.cmi.mdr.pdb.app.components.Icons
import cz.e_bs.cmi.mdr.pdb.app.{Osoba, PracovniPomer, Funkce}
import cz.e_bs.cmi.mdr.pdb.app.components.Avatar
import cz.e_bs.cmi.mdr.pdb.app.Page
import cz.e_bs.cmi.mdr.pdb.app.services.DataFetcher
import com.raquo.airstream.core.EventStream
import com.raquo.waypoint.Router
import cz.e_bs.cmi.mdr.pdb.app.components.CustomAttrs.datetime
import cz.e_bs.cmi.mdr.pdb.app.components.AppPage
import cz.e_bs.cmi.mdr.pdb.app.components.OsobaView
import cz.e_bs.cmi.mdr.pdb.app.Parametr
import cz.e_bs.cmi.mdr.pdb.app.components.list.BaseList
import cz.e_bs.cmi.mdr.pdb.app.components.list.NavigableList
import cz.e_bs.cmi.mdr.pdb.app.components.list.Navigable
import cz.e_bs.cmi.mdr.pdb.waypoint.components.Navigator

case class DetailPage(fetch: String => EventStream[Osoba])(
    $page: Signal[Page.Detail]
)(using router: Router[Page])
    extends AppPage:
  // TODO: proper loader
  private val loading =
    div(
      cls := "bg-gray-50 overflow-hidden rounded-lg",
      div(
        cls := "px-4 py-5 sm:p-6",
        "Loading..."
      )
    )

  override def pageContent: HtmlElement =
    val data = Var[Option[Osoba]](None)
    val $maybeOsoba =
      data.signal.split(_ => ())((_, _, s) => renderView(s))
    val $fetchedData = $page.splitOne(_.osobniCislo)((osc, _, _) => osc)
      .flatMap(fetch)
      .debugLog()
    div(
      cls := "max-w-7xl mx-auto px-4 py-6 sm:px-6 lg:px-8",
      $fetchedData --> data.writer.contramapSome,
      $fetchedData --> (o => router.replaceState(Page.Detail(o))),
      child <-- $maybeOsoba.map(_.getOrElse(loading))
    )

  private def renderView($osoba: Signal[Osoba]): HtmlElement =
    given BaseList.AsRow[(Osoba, Parametr)] with
      extension (d: (Osoba, Parametr))
        def asRow = d match {
          case (os, param) =>
            BaseList.Row(
              param.id,
              param.nazev,
              BaseList.Tag("Splněno", BaseList.Color.Green),
              Nil,
              BaseList.IconText(
                p(
                  """do """,
                  time(
                    datetime := "2020-01-07",
                    "01.07.2020"
                  )
                ),
                Icons.solid.calendar
              )
            )
        }

    given Navigable[(Osoba, Parametr)] with
      extension (x: (Osoba, Parametr))
        def navigate: Modifier[HtmlElement] =
          Navigator.navigateTo[Page](
            Page.DetailParametru(x._1, x._2)
          )

    import BaseList.Row.given

    val parameterList = new BaseList[(Osoba, Parametr)]
      with NavigableList[(Osoba, Parametr), Page]

    div(
      cls := "flex flex-col space-y-4",
      OsobaView($osoba),
      div(
        cls := "bg-white shadow overflow-hidden sm:rounded-md",
        parameterList.render(
          for { o <- $osoba } yield for { p <- o.parametry } yield o -> p
        )
      )
    )
