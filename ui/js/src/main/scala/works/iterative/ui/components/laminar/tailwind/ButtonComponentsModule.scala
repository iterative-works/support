package works.iterative.ui.components.laminar
package tailwind
package ui

import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.*

trait ButtonComponentsModule:

  object buttons:

    private inline def srHelp(text: String): Modifier[HtmlElement] =
      span(cls := "sr-only", text)

    val sharedButtonClasses =
      "inline-flex justify-center py-2 px-4 border rounded-md shadow-sm text-sm font-medium focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500"

    val sharedButtonMod: HtmlMod = cls(sharedButtonClasses)

    val primaryButtonClasses =
      "border-transparent text-white bg-indigo-600 hover:bg-indigo-700 disabled:bg-indigo-300"
    val primaryButtonMod: HtmlMod = cls(primaryButtonClasses)

    val secondaryButtonClasses =
      "border-gray-300 text-gray-700 hover:bg-gray-50"
    val secondaryButtonMod: HtmlMod = cls(secondaryButtonClasses)

    val neutralButtonClasses =
      "border-gray-300 text-gray-700 bg-white hover:bg-gray-50"
    val neutralButtonMod: HtmlMod = cls(neutralButtonClasses)

    val positiveButtonClasses =
      "border-transparent text-white bg-green-600 hover:bg-green-700"
    val positiveButtonMod: HtmlMod = cls(positiveButtonClasses)

    val negativeButtonClasses =
      "border-transparent text-white bg-red-600 hover:bg-red-700"
    val negativeButtonMod: HtmlMod = cls(negativeButtonClasses)

    def button(
        text: Modifier[HtmlElement],
        id: Option[String],
        icon: Option[SvgElement] = None,
        buttonType: String = "submit",
        primary: Boolean = false
    )(mods: HtmlMod*): HtmlElement =
      L.button(
        id.map(idAttr(_)),
        tpe(buttonType),
        sharedButtonMod,
        if primary then primaryButtonMod else secondaryButtonMod,
        icon,
        text,
        mods
      )

    def primaryButton(
        text: Modifier[HtmlElement],
        id: Option[String] = None,
        icon: Option[SvgElement] = None,
        buttonType: String = "submit"
    )(mods: Modifier[HtmlElement]*): HtmlElement =
      button(text, id, icon, buttonType, true)(mods*)

    def secondaryButton(
        text: Modifier[HtmlElement],
        id: Option[String] = None,
        icon: Option[SvgElement] = None,
        buttonType: String = "button"
    )(mods: Modifier[HtmlElement]*): HtmlElement =
      button(text, id, icon, buttonType, false)(mods*)

    def inlineButton(
        icon: SvgElement,
        id: Option[String],
        srText: Option[String] = None
    )(
        mods: Modifier[HtmlElement]*
    ): HtmlElement =
      L.button(
        id.map(idAttr(_)),
        tpe := "button",
        cls := "ml-1 inline-flex h-4 w-4 flex-shrink-0 rounded-full p-1 text-gray-400 hover:bg-gray-200 hover:text-gray-500",
        srText.map(srHelp(_)),
        icon
      )

    def iconButton(
        icon: SvgElement,
        id: Option[String],
        srText: Option[String] = None
    )(
        mods: Modifier[HtmlElement]*
    ): HtmlElement =
      L.button(
        id.map(idAttr(_)),
        tpe := "button",
        cls := "inline-flex justify-center px-3.5 py-2 border border-gray-300 shadow-sm text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-pink-500",
        icon,
        srText.map(srHelp(_)),
        mods
      )
