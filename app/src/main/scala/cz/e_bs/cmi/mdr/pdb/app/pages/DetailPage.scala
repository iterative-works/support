package cz.e_bs.cmi.mdr.pdb.app.pages

import com.raquo.airstream.core.EventStream
import com.raquo.laminar.api.L.{_, given}
import com.raquo.waypoint.Router
import cz.e_bs.cmi.mdr.pdb.app.Osoba
import cz.e_bs.cmi.mdr.pdb.app.Page
import cz.e_bs.cmi.mdr.pdb.app.Parametr
import cz.e_bs.cmi.mdr.pdb.app.components.AppPage
import cz.e_bs.cmi.mdr.pdb.app.components.Avatar
import cz.e_bs.cmi.mdr.pdb.app.components.CustomAttrs.datetime
import cz.e_bs.cmi.mdr.pdb.app.components.Icons
import cz.e_bs.cmi.mdr.pdb.app.components.Loading
import cz.e_bs.cmi.mdr.pdb.app.components.OsobaView
import cz.e_bs.cmi.mdr.pdb.app.components.list.BaseList
import cz.e_bs.cmi.mdr.pdb.app.components.list.Navigable
import cz.e_bs.cmi.mdr.pdb.app.components.list.NavigableList
import cz.e_bs.cmi.mdr.pdb.waypoint.components.Navigator
import cz.e_bs.cmi.mdr.pdb.app.Action
import cz.e_bs.cmi.mdr.pdb.app.FetchUserDetails

case class DetailPage(
    $input: EventStream[Osoba],
    actionBus: Observer[Action],
    $page: Signal[Page.Detail]
)(using router: Router[Page])
    extends AppPage:
  override def pageContent: HtmlElement =
    val $oscChangeSignal = $page.splitOne(_.osobniCislo)((osc, _, _) => osc)
    // TODO: filter the value based on the current osc
    // OSC change will fetch new data, but still
    // - we need to be sure that what we got is really what we ought to display
    // - we want to display stale data accordingly (at least with loading indicator)
    val $data = $input.startWithNone
    val $maybeOsoba =
      $data.split(_ => ())((_, _, s) => renderView(s))
    val $pageChangeSignal =
      $oscChangeSignal.map(FetchUserDetails.apply)
    div(
      cls := "max-w-7xl mx-auto px-4 py-6 sm:px-6 lg:px-8",
      $pageChangeSignal --> actionBus,
      // $fetchedData --> (o => router.replaceState(Page.Detail(o))),
      child <-- $maybeOsoba.map(_.getOrElse(Loading))
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
