package works.iterative.ui.components.laminar.tailwind.ui

import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveHtmlElement

trait BreadcrumbsModule:
  self: IconsModule =>

  object breadcrumbs:
    def container(mods: HtmlMod*): HtmlElement =
      navTag(cls("flex"), aria.label("Breadcrumb"), mods)

    def list(mods: HtmlMod*): HtmlElement =
      ol(cls("flex items-center space-x-4"), role("list"), mods)

    def homeItem(mods: HtmlMod*): HtmlElement =
      li(
        div(
          a(
            href("#"),
            cls("text-gray-400 hover:text-gray-500"),
            icons.home(svg.cls("h-5 w-5 flex-shrink-0")),
            span(cls("sr-only"), "Home"),
            mods
          )
        )
      )

    def item(mods: HtmlMod*): HtmlElement =
      li(
        div(
          cls("flex items-center"),
          icons.`chevron-right-solid`(
            svg.cls("h-5 w-5 flex-shrink-0 text-gray-400")
          ),
          a(
            href("#"),
            cls("ml-4 text-sm font-medium text-gray-500 hover:text-gray-700"),
            mods
          )
        )
      )
