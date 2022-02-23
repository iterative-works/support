package cz.e_bs.cmi.mdr.pdb.app.components

import com.raquo.laminar.api.L.{*, given}
import com.raquo.waypoint.Router
import CustomAttrs.svg.ariaHidden
import cz.e_bs.cmi.mdr.pdb.waypoint.components.Navigator
import cz.e_bs.cmi.mdr.pdb.app.Page

import com.raquo.laminar.api.L.{*, given}
import cz.e_bs.cmi.mdr.pdb.app.components.list.IconText.ViewModel

object Breadcrumbs:

  private def slash =
    import svg.{*, given}
    svg(
      cls := "flex-shrink-0 h-5 w-5 text-gray-300",
      xmlns := "http://www.w3.org/2000/svg",
      fill := "currentColor",
      viewBox := "0 0 20 20",
      ariaHidden := true,
      path(
        d := "M5.555 17.776l8-16 .894.448-8 16-.894-.448z"
      )
    )

  object Home:
    type ViewModel = Page
    def apply($m: Signal[ViewModel])(using Router[Page]): HtmlElement =
      a(
        Navigator.navigateTo($m),
        cls := "text-gray-400 hover:text-gray-500",
        Icons.solid.home,
        span(cls := "sr-only", "Home")
      )

  object Segment:
    type ViewModel = Page
    def apply($m: Signal[ViewModel])(using Router[Page]): HtmlElement =
      li(
        div(
          cls := "flex items-center",
          slash,
          a(
            Navigator.navigateTo($m),
            cls := "ml-4 text-sm font-medium text-gray-500 hover:text-gray-700",
            child.text <-- $m.map(_.title)
          )
        )
      )

  object FullBreadcrumbs:
    type ViewModel = Page
    def apply($m: Signal[ViewModel])(using Router[Page]): HtmlElement =
      div(
        cls := "hidden sm:block",
        ol(
          role := "list",
          cls := "flex items-center space-x-4",
          Home($m.map(_.path.head)),
          children <-- $m.map(_.path.tail)
            .split(_.id)((_, _, $p) => Segment($p))
        )
      )

  object ShortBreadcrumbs:
    type ViewModel = Page
    def apply($m: Signal[ViewModel])(using Router[Page]): HtmlElement =
      div(
        cls := "flex sm:hidden",
        child <-- $m.map(
          _.parent match
            case None => Home($m)
            case Some(p) =>
              a(
                Navigator.navigateTo($m),
                cls := "group inline-flex space-x-3 text-sm font-medium text-gray-500 hover:text-gray-700",
                Icons.solid.`arrow-narrow-left`,
                span(p.title)
              )
        )
      )

  def apply()(using router: Router[Page]): HtmlElement =
    nav(
      cls := "flex",
      aria.label := "Breadcrumb",
      ShortBreadcrumbs(router.$currentPage),
      FullBreadcrumbs(router.$currentPage)
    )
