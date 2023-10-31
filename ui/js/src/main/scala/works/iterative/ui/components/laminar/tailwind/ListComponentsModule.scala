package works.iterative.ui.components.laminar
package tailwind
package ui

import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveHtmlElement
import works.iterative.ui.components.laminar.tailwind.color.ColorKind

trait ListComponentsModule:
  self: BadgeComponentsModule =>

  object list:
    def label(
        text: String,
        color: ColorKind
    ): HtmlElement = badges.pill(text, color)

    def item(
        title: String,
        subtitle: Option[String],
        right: Modifier[HtmlElement] = emptyMod,
        avatar: Option[Modifier[HtmlElement]] = None,
        contentMod: Modifier[HtmlElement] = emptyMod
    ): LI =
      li(
        cls("group"),
        div(
          contentMod,
          cls(
            "relative px-6 py-5 flex items-center space-x-3 hover:bg-gray-50 focus-within:ring-2 focus-within:ring-inset focus-within:ring-pink-500"
          ),
          avatar.map(a =>
            div(
              cls("flex-shrink-0"),
              div(
                cls(
                  "rounded-full text-indigo-200 bg-indigo-600 flex items-center justify-center w-10 h-10"
                ),
                a
              )
            )
          ),
          div(
            cls("flex-1 min-w-0"),
            p(
              cls("text-sm font-medium text-gray-900"),
              title,
              span(cls("float-right"), right)
            ),
            subtitle.map(st =>
              p(
                cls("text-sm text-gray-500 truncate"),
                st
              )
            )
          )
        )
      )

    def unordered(
        children: Modifier[HtmlElement]
    ): ReactiveHtmlElement[org.scalajs.dom.html.UList] =
      ul(
        cls("relative z-0 divide-y divide-gray-200"),
        role("list"),
        children
      )

    def listSection(
        header: String,
        list: HtmlElement
    ): Div =
      div(
        cls("relative"),
        div(
          cls(
            "z-10 sticky top-0 border-t border-b border-gray-200 bg-gray-50 px-6 py-1 text-sm font-medium text-gray-500"
          ),
          header
        ),
        list
      )

    def navigation(sections: Modifier[HtmlElement]): HtmlElement =
      navTag(
        cls("flex-1 min-h-0 overflow-y-auto"),
        sections
      )
