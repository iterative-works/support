package cz.e_bs.cmi.mdr.pdb.app.pages

import com.raquo.laminar.api.L.{*, given}
import cz.e_bs.cmi.mdr.pdb.app.components.Icons
import cz.e_bs.cmi.mdr.pdb.app.Osoba
import cz.e_bs.cmi.mdr.pdb.app.Page
import com.raquo.waypoint.Router
import cz.e_bs.cmi.mdr.pdb.app.components.AppPage

case class DirectoryPage(data: EventStream[List[Osoba]])(using
    router: Router[Page]
) extends AppPage:

  def pageContent: HtmlElement =
    div(
      cls := "max-w-7xl mx-auto",
      //cls := "xl:order-first xl:flex xl:flex-col flex-shrink-0 w-96 border-r border-gray-200",
      form(
        cls := "p-4 mt-6 flex space-x-4",
        action := "#",
        div(
          cls := "flex-1 min-w-0",
          label(
            forId := "search",
            cls := "sr-only",
            """Search"""
          ),
          div(
            cls := "relative rounded-md shadow-sm",
            div(
              cls := "absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none",
              Icons.solid.search
            ),
            input(
              tpe := "search",
              name := "search",
              idAttr := "search",
              cls := "focus:ring-pink-500 focus:border-pink-500 block w-full pl-10 sm:text-sm border-gray-300 rounded-md",
              placeholder := "Search"
            )
          )
        ),
        button(
          tpe := "submit",
          cls := "inline-flex justify-center px-3.5 py-2 border border-gray-300 shadow-sm text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-pink-500",
          Icons.solid.filter,
          span(
            cls := "sr-only",
            """Search"""
          )
        )
      ),
      nav(
        cls := "flex-1 min-h-0 overflow-y-auto",
        aria.label := "Directory",
        div(
          cls := "relative",
          // TODO: group by surname
          div(
            cls := "z-10 sticky top-0 border-t border-b border-gray-200 bg-gray-50 px-6 py-1 text-sm font-medium text-gray-500",
            h3(
              """A"""
            )
          ),
          ul(
            role := "list",
            cls := "relative z-0 divide-y divide-gray-200",
            // TODO: zero / loading page
            children <-- data.map(_.map({ o =>
              val page = Page.Detail(o.osobniCislo)
              li(
                div(
                  cls := "relative px-6 py-5 flex items-center space-x-3 hover:bg-gray-50 focus-within:ring-2 focus-within:ring-inset focus-within:ring-pink-500",
                  div(
                    cls := "flex-shrink-0",
                    img(
                      cls := "h-10 w-10 rounded-full",
                      src := "https://images.unsplash.com/photo-1494790108377-be9c29b29330?ixlib=rb-1.2.1&ixid=eyJhcHBfaWQiOjEyMDd9&auto=format&fit=facearea&facepad=2&w=256&h=256&q=80",
                      alt := ""
                    )
                  ),
                  div(
                    cls := "flex-1 min-w-0",
                    a(
                      href := router.absoluteUrlForPage(page),
                      navigateTo(page),
                      cls := "focus:outline-none",
                      span(
                        cls := "absolute inset-0",
                        aria.hidden := true
                      ),
                      p(
                        cls := "text-sm font-medium text-gray-900",
                        o.jmeno
                      ),
                      p(
                        cls := "text-sm text-gray-500 truncate",
                        o.hlavniFunkce.nazev
                      )
                    )
                  )
                )
              )
            }))
          )
        )
      )
    )
