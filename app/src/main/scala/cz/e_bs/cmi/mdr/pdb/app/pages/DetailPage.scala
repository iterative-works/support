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

val datetime = customHtmlAttr("datetime", StringAsIsCodec)

def DetailPage(fetch: String => EventStream[Osoba])(
    $page: Signal[Page.Detail]
)(using router: Router[Page]): HtmlElement =
  // TODO: proper loader
  val loading =
    div(
      cls := "bg-gray-50 overflow-hidden rounded-lg",
      div(
        cls := "px-4 py-5 sm:p-6",
        "Loading..."
      )
    )
  val data = Var[Option[Osoba]](None)
  val $maybeOsoba = data.signal.split(_ => ())((_, _, s) => OsobaView(s))
  val $fetchedData = $page.splitOne(_.osobniCislo)((osc, _, _) => osc)
    .flatMap(fetch)
    .debugLog()
  div(
    cls := "max-w-7xl mx-auto px-4 py-6 sm:px-6 lg:px-8",
    $fetchedData --> data.writer.contramapSome,
    $fetchedData --> (o => router.replaceState(Page.Detail(o))),
    child <-- $maybeOsoba.map(_.getOrElse(loading))
  )

def OsobaView($osoba: Signal[Osoba]): HtmlElement =
  def funkce($fce: Signal[Funkce]) =
    p(
      cls := "text-sm font-medium text-gray-500",
      child.text <-- $fce.map(_.nazev),
      span(
        cls := "hidden md:inline",
        " @ ",
        child.text <-- $fce.map(_.stredisko),
        ", ",
        child.text <-- $fce.map(_.voj)
      )
    )

  def pp($pp: Signal[PracovniPomer]) =
    p(
      cls := "text-sm font-medium text-gray-500",
      child.text <-- $pp.map(_.druh),
      " od ",
      time(
        datetime <-- $pp.map(_.pocatek.toString),
        child.text <-- $pp.map(_.pocatek.toString)
      )
    )

  div(
    cls := "flex flex-col space-y-4",
    div(
      cls := "md:flex md:items-center md:justify-between md:space-x-5",
      div(
        cls := "flex items-start space-x-5",
        div(
          cls := "flex-shrink-0",
          Avatar($osoba.map(_.img), 16)
        ),
        div(
          h1(
            cls := "text-2xl font-bold text-gray-900",
            child.text <-- $osoba.map(_.jmeno)
          ),
          funkce($osoba.map(_.hlavniFunkce)),
          pp($osoba.map(_.pracovniPomer))
        )
      )
    ),
    div(
      cls := "bg-white shadow overflow-hidden sm:rounded-md",
      ul(
        role := "list",
        cls := "divide-y divide-gray-200",
        li(
          a(
            href := "#",
            cls := "block hover:bg-gray-50",
            div(
              cls := "px-4 py-4 sm:px-6 items-center flex",
              div(
                cls := "min-w-0 flex-1 pr-4",
                div(
                  cls := "flex items-center justify-between",
                  p(
                    cls := "text-sm font-medium text-indigo-600 truncate",
                    "Komise pro pověřování pracovníků"
                  ),
                  div(
                    cls := "ml-2 flex-shrink-0 flex",
                    p(
                      cls := "px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-green-100 text-green-800",
                      """Splněno"""
                    )
                  )
                ),
                div(
                  cls := "mt-2 sm:flex sm:justify-between",
                  div(),
                  div(
                    cls := "mt-2 flex items-center text-sm text-gray-500 sm:mt-0",
                    Icons.solid.calendar,
                    p(
                      """do """,
                      time(
                        datetime := "2020-01-07",
                        "01.07.2020"
                      )
                    )
                  )
                )
              ),
              div(
                cls := "flex-shrink-0",
                Icons.solid.`chevron-right`
              )
            )
          )
        ),
        li(
          a(
            href := "#",
            cls := "block hover:bg-gray-50",
            div(
              cls := "px-4 py-4 sm:px-6",
              div(
                cls := "flex items-center justify-between",
                p(
                  cls := "text-sm font-medium text-indigo-600 truncate",
                  """Front End Developer"""
                ),
                div(
                  cls := "ml-2 flex-shrink-0 flex",
                  p(
                    cls := "px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-green-100 text-green-800",
                    """Full-time"""
                  )
                )
              ),
              div(
                cls := "mt-2 sm:flex sm:justify-between",
                div(
                  cls := "sm:flex",
                  p(
                    cls := "flex items-center text-sm text-gray-500",
                    Icons.solid.users,
                    """Engineering"""
                  ),
                  p(
                    cls := "mt-2 flex items-center text-sm text-gray-500 sm:mt-0 sm:ml-6",
                    Icons.solid.`location-marker`,
                    """Remote"""
                  )
                ),
                div(
                  cls := "mt-2 flex items-center text-sm text-gray-500 sm:mt-0",
                  Icons.solid.calendar,
                  p(
                    """Closing on""",
                    time(
                      datetime := "2020-01-07",
                      """January 7, 2020"""
                    )
                  )
                )
              )
            )
          )
        ),
        li(
          a(
            href := "#",
            cls := "block hover:bg-gray-50",
            div(
              cls := "px-4 py-4 sm:px-6",
              div(
                cls := "flex items-center justify-between",
                p(
                  cls := "text-sm font-medium text-indigo-600 truncate",
                  """User Interface Designer"""
                ),
                div(
                  cls := "ml-2 flex-shrink-0 flex",
                  p(
                    cls := "px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-green-100 text-green-800",
                    """Full-time"""
                  )
                )
              ),
              div(
                cls := "mt-2 sm:flex sm:justify-between",
                div(
                  cls := "sm:flex",
                  p(
                    cls := "flex items-center text-sm text-gray-500",
                    Icons.solid.users,
                    """Design"""
                  ),
                  p(
                    cls := "mt-2 flex items-center text-sm text-gray-500 sm:mt-0 sm:ml-6",
                    Icons.solid.`location-marker`,
                    """Remote"""
                  )
                ),
                div(
                  cls := "mt-2 flex items-center text-sm text-gray-500 sm:mt-0",
                  Icons.solid.calendar,
                  p(
                    """Closing on""",
                    time(
                      datetime := "2020-01-14",
                      """January 14, 2020"""
                    )
                  )
                )
              )
            )
          )
        )
      )
    )
  )
