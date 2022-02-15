package cz.e_bs.cmi.mdr.pdb.app.components

import com.raquo.laminar.api.L.{*, given}
import com.raquo.waypoint.Router
import CustomAttrs.svg.ariaHidden
import cz.e_bs.cmi.mdr.pdb.waypoint.components.Navigator
import cz.e_bs.cmi.mdr.pdb.app.Page

trait Breadcrumbs(using router: Router[Page]):
  self: Navigator[Page] =>

  def breadcrumbs: HtmlElement =
    val $p = router.$currentPage
    nav(
      cls := "flex",
      aria.label := "Breadcrumb",
      child <-- $p.map(renderShort),
      child <-- $p.map(renderFull)
    )

  private def renderFull(page: Page): HtmlElement =
    div(
      cls := "hidden sm:block",
      ol(
        role := "list",
        cls := "flex items-center space-x-4",
        renderItems(page)
      )
    )

  private def renderShort(page: Page): HtmlElement =
    div(
      cls := "flex sm:hidden",
      page.parent match {
        case None => renderHome(page)
        case Some(p) =>
          a(
            href := router.absoluteUrlForPage(p),
            navigateTo(p),
            cls := "group inline-flex space-x-3 text-sm font-medium text-gray-500 hover:text-gray-700",
            Icons.solid.`arrow-narrow-left`,
            span(p.title)
          )
      }
    )

  private def renderItems(page: Page): Seq[HtmlElement] =
    page.parent match {
      case None => Seq(li(div(renderHome(page))))
      case Some(p) =>
        renderItems(p) :+ li(
          div(
            cls := "flex items-center",
            slash,
            a(
              href := "#",
              cls := "ml-4 text-sm font-medium text-gray-500 hover:text-gray-700",
              page.title
            )
          )
        )
    }

  private def renderHome(page: Page) =
    a(
      href := router.absoluteUrlForPage(page),
      navigateTo(page),
      cls := "text-gray-400 hover:text-gray-500",
      Icons.solid.home,
      span(cls := "sr-only", "Home")
    )

  private def slash = {
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
  }
