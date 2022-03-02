package cz.e_bs.cmi.mdr.pdb.app.components

import com.raquo.laminar.api.L.{*, given}
import com.raquo.waypoint.Router
import fiftyforms.ui.components.tailwind.CustomAttrs.svg.ariaHidden
import fiftyforms.ui.components.tailwind.list.IconText.ViewModel
import fiftyforms.ui.components.tailwind.Icons
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

  def backIcon =
    Icons.solid
      .`arrow-narrow-left`()
      .amend(
        svg.cls := "flex-shrink-0 text-gray-400 group-hover:text-gray-600"
      )

  object Link:
    case class ViewModel(
        page: Page,
        icon: Option[SvgElement],
        extraClasses: String,
        text: String,
        textClasses: Option[String] = None
    )

    val baseClasses = "text-sm font-medium text-gray-500 hover:text-gray-700"

    def shortHome(p: Page) = ViewModel(
      p,
      Some(backIcon),
      "group inline-flex space-x-3 text-sm text-gray-400 hover:text-gray-600",
      "Zpět na úvodní stránku",
      None
    )

    def fullHome(p: Page) = ViewModel(
      p,
      Some(Icons.solid.home().amend(svg.cls := "flex-shrink-0")),
      "text-gray-400 hover:text-gray-500",
      "Domů",
      Some("sr-only")
    )

    def apply($m: Signal[ViewModel], actionBus: Observer[Action])(using
        Router[Page]
    ): HtmlElement =
      PageLink
        .container($m.map(_.page), actionBus)
        .amend(
          cls <-- $m.map(_.extraClasses),
          child.maybe <-- $m.map(_.icon),
          span(
            cls <-- $m.map(_.textClasses.getOrElse("")),
            child.text <-- $m.map(_.text)
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
                  if (p.isRoot) then Link.fullHome(p)
                  else
                    Link.ViewModel(
                      p,
                      None,
                      s"ml-4 max-w-xs truncate ${Link.baseClasses}",
                      p.title
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
          if target.isRoot then Link.shortHome(target)
          else
            Link.ViewModel(
              target,
              Some(backIcon),
              "group inline-flex space-x-3 text-sm text-gray-400 hover:text-gray-600",
              s"Zpět na ${target.title}"
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
