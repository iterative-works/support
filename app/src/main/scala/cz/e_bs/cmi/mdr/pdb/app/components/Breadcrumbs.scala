package cz.e_bs.cmi.mdr.pdb.app.components

import com.raquo.laminar.api.L.{*, given}
import com.raquo.waypoint.Router
import CustomAttrs.svg.ariaHidden
import cz.e_bs.cmi.mdr.pdb.app.components.list.IconText.ViewModel
import cz.e_bs.cmi.mdr.pdb.app.components.PageLink
import cz.e_bs.cmi.mdr.pdb.app.Page

import com.raquo.laminar.api.L.{*, given}
import io.laminext.syntax.core.{*, given}
import cz.e_bs.cmi.mdr.pdb.app.Action

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

  object Link:
    case class ViewModel(
        page: Page,
        icon: Option[SvgElement],
        text: String,
        extraClasses: String
    )
    def apply($m: Signal[ViewModel], actionBus: Observer[Action])(using
        Router[Page]
    ): HtmlElement =
      inline def alt[T](
          homeVariant: => T,
          pageVariant: ViewModel => T
      ): Signal[T] =
        $m.map { m =>
          if (m.page.isRoot) then homeVariant else pageVariant(m)
        }
      PageLink
        .container($m.map(_.page), actionBus)
        .amend(
          cls <-- alt(
            "text-gray-400 hover:text-gray-500",
            m =>
              s"${m.extraClasses} text-sm font-medium text-gray-500 hover:text-gray-700"
          ),
          child.maybe <-- alt(
            Some(Icons.solid.home),
            _.icon
          ),
          child <-- alt(
            span(cls := "sr-only", "Domů"),
            m => span(m.text)
          )
        )

  object FullBreadcrumbs:
    type ViewModel = Page
    def apply($m: Signal[ViewModel], actionBus: Observer[Action])(using
        Router[Page]
    ): HtmlElement =
      ol(
        role := "list",
        cls := "flex items-center space-x-4",
        children <-- $m.map(_.path).split(_.id)((_, _, $p) =>
          li(
            div(
              cls := "flex items-center",
              child.maybe <-- $p.map(_.isRoot).switch(None, Some(slash)),
              Link(
                $p.map(p =>
                  Link.ViewModel(
                    p,
                    None,
                    p.title,
                    "ml-4"
                  )
                ),
                actionBus
              )
            )
          )
        )
      )

  object ShortBreadcrumbs:
    type ViewModel = Page
    def apply($m: Signal[ViewModel], actionBus: Observer[Action])(using
        Router[Page]
    ): HtmlElement =
      Link(
        $m.map { p =>
          val target = p.parent.getOrElse(p)
          Link.ViewModel(
            target,
            Some(Icons.solid.`arrow-narrow-left`),
            s"Zpět na ${target.title}",
            "group inline-flex space-x-3"
          )
        },
        actionBus
      )

  def apply(actionBus: Observer[Action])(using
      router: Router[Page]
  ): HtmlElement =
    nav(
      cls := "flex",
      aria.label := "Breadcrumb",
      div(
        cls := "flex sm:hidden",
        ShortBreadcrumbs(router.$currentPage, actionBus)
      ),
      div(
        cls := "hidden sm:block",
        FullBreadcrumbs(router.$currentPage, actionBus)
      )
    )
