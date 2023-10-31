package works.iterative.ui.components.laminar
package tailwind
package ui

import com.raquo.laminar.api.L.*

trait PageComponentsModule:

  object page:
    def container(
        children: Modifier[HtmlElement]*
    ): HtmlElement =
      div(
        cls("max-w-7xl mx-auto h-full px-4 sm:px-6 lg:px-8 overflow-y-auto"),
        children
      )

    def singleColumn(
        header: Modifier[HtmlElement]
    )(children: Modifier[HtmlElement]*): HtmlElement =
      div(
        cls("p-8 bg-gray-100 h-full"),
        header,
        children
      )

    def pageHeader(
        title: Modifier[HtmlElement],
        right: Modifier[HtmlElement],
        subtitle: Option[Modifier[HtmlElement]] = None
    ): HtmlElement =
      div(
        cls("pb-5 border-b border-gray-200"),
        div(cls("float-right"), right),
        h1(
          cls("text-2xl leading-6 font-medium text-gray-900"),
          title
        ),
        subtitle.map(
          p(
            cls("text-sm font-medium text-gray-500"),
            _
          )
        )
      )

    def clickable: Modifier[HtmlElement] =
      cls("text-sm font-medium text-indigo-600 hover:text-indigo-400")
