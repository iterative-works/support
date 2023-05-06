package works.iterative.ui.components.laminar
package tailwind
package ui

import com.raquo.laminar.api.L.{*, given}

trait ButtonComponentsModule:

  object buttons:

    private inline def srHelp(text: String): Modifier[HtmlElement] =
      span(cls := "sr-only", text)

    def primaryButton(
        id: String,
        text: Modifier[HtmlElement],
        icon: Option[SvgElement] = None,
        buttonType: String = "submit"
    )(mods: Modifier[HtmlElement]*): HtmlElement =
      button(
        tpe(buttonType),
        cls := "disabled:bg-indigo-300 ml-3 inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500",
        icon,
        text,
        mods
      )

    def secondaryButton(
        id: String,
        text: Modifier[HtmlElement],
        icon: Option[SvgElement] = None,
        buttonType: String = "button"
    )(mods: Modifier[HtmlElement]*): HtmlElement =
      button(
        tpe(buttonType),
        cls(
          "ml-2 bg-white inline-flex justify-center py-2 px-4 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500"
        ),
        icon,
        text,
        mods
      )

    def inlineButton(id: String, icon: SvgElement)(
        mods: Modifier[HtmlElement]*
    ): HtmlElement =
      button(
        tpe := "button",
        cls := "ml-1 inline-flex h-4 w-4 flex-shrink-0 rounded-full p-1 text-gray-400 hover:bg-gray-200 hover:text-gray-500",
        srHelp(id),
        icon
      )

    def iconButton(id: String, icon: SvgElement, srText: Option[String] = None)(
        mods: Modifier[HtmlElement]*
    ): HtmlElement =
      button(
        tpe := "button",
        cls := "inline-flex justify-center px-3.5 py-2 border border-gray-300 shadow-sm text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-pink-500",
        icon,
        srText.map(srHelp(_))
      )
