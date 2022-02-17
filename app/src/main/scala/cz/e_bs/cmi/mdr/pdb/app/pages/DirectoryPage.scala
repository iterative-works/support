package cz.e_bs.cmi.mdr.pdb.app.pages

import com.raquo.laminar.api.L.{*, given}
import cz.e_bs.cmi.mdr.pdb.app.components.Icons
import cz.e_bs.cmi.mdr.pdb.app.Page
import com.raquo.waypoint.Router
import cz.e_bs.cmi.mdr.pdb.app.components.{AppPage, Loading}
import cz.e_bs.cmi.mdr.pdb.UserInfo
import cz.e_bs.cmi.mdr.pdb.app.components.Avatar

case class DirectoryPage(fetch: () => EventStream[List[UserInfo]])(using
    router: Router[Page]
) extends AppPage:

  override def pageContent: HtmlElement =
    val data = Var[Option[List[UserInfo]]](None)
    val $maybeDirectory =
      data.signal.split(_ => ())((_, _, s) => renderDirectory(s))
    div(
      cls := "max-w-7xl mx-auto",
      //cls := "xl:order-first xl:flex xl:flex-col flex-shrink-0 w-96 border-r border-gray-200",
      searchForm,
      fetch().delay(1000) --> data.writer.contramapSome,
      child <-- $maybeDirectory.map(_.getOrElse(Loading))
    )

  private def renderDirectory(data: Signal[List[UserInfo]]): HtmlElement =
    val byLetter = for {
      d <- data
    } yield for {
      (letter, users) <- d.groupBy(_.surname.head).to(List).sortBy(_._1)
    } yield (letter.toString, users.sortBy(_.surname))

    val rendered = byLetter
      .split(_._1)((_, _, s) =>
        div(
          cls := "relative",
          // TODO: group by surname
          div(
            cls := "z-10 sticky top-0 border-t border-b border-gray-200 bg-gray-50 px-6 py-1 text-sm font-medium text-gray-500",
            h3(child.text <-- s.map(_._1))
          ),
          ul(
            role := "list",
            cls := "relative z-0 divide-y divide-gray-200",
            // TODO: zero / loading page
            children <-- s.map(_._2.map(renderUser))
          )
        )
      )

    nav(
      cls := "flex-1 min-h-0 overflow-y-auto",
      aria.label := "Directory",
      children <-- rendered
    )

  private def renderUser(o: UserInfo) =
    inline def avatarImage =
      Avatar(Val(o.img).signal).avatarImage(10)

    val page = Page.Detail(o.personalNumber)
    li(
      div(
        cls := "relative px-6 py-5 flex items-center space-x-3 hover:bg-gray-50 focus-within:ring-2 focus-within:ring-inset focus-within:ring-pink-500",
        div(
          cls := "flex-shrink-0",
          child <-- avatarImage
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
              o.name
            ),
            p(
              cls := "text-sm text-gray-500 truncate",
              o.mainFunction
            )
          )
        )
      )
    )

  private def searchForm: HtmlElement =
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
    )
