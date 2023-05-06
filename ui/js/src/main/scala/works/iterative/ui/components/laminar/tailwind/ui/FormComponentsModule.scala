package works.iterative.ui.components.laminar
package tailwind
package ui

import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.{*, given}
import works.iterative.ui.components.tailwind.ComponentContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.scalajs.dom.html
import com.raquo.laminar.modifiers.KeyUpdater

trait FormComponentsModule:
  self: IconsModule =>

  object forms:

    def section(
        title: Modifier[HtmlElement],
        subtitle: Option[Modifier[HtmlElement]]
    )(content: Modifier[HtmlElement]*): HtmlElement =
      div(
        cls("space-y-6 sm:space-y-5"),
        div(
          h3(cls("text-lg leading-6 font-medium text-gray-900"), title),
          subtitle.map(st => p(cls("mt-1 max-w-2xl text-sm text-gray-500"), st))
        ),
        div(cls("mt-6 sm:mt-5 space-y-6 sm:space-y-5"), content)
      )

    def label(labelText: String, forId: Option[String] = None)(
        mods: Modifier[HtmlElement]*
    ): HtmlElement =
      L.label(
        cls("block text-sm font-medium text-gray-700 sm:mt-px sm:pt-2"),
        forId.map(id => L.forId(id)),
        labelText,
        mods
      )

    def field(
        label: Modifier[HtmlElement]
    )(content: Modifier[HtmlElement]*): HtmlElement =
      div(
        cls(
          "sm:grid sm:grid-cols-3 sm:gap-4 sm:items-start sm:border-t sm:border-gray-200 sm:pt-5"
        ),
        label,
        div(cls("mt-1 sm:mt-0 sm:col-span-2"), content)
      )

    def field(
        id: String,
        labelText: String,
        input: HtmlElement,
        help: Option[String]
    ): HtmlElement =
      field(
        label(labelText, Some(id))()
      )(
        input.amend(idAttr(id)),
        help.map(h => p(cls("mt-2 text-sm text-gray-500"), h))
      )

    def form(mods: Modifier[HtmlElement]*)(
        sections: Modifier[HtmlElement]*
    )(actions: Modifier[HtmlElement]*): HtmlElement =
      L.form(
        cls("space-y-8 divide-y divide-gray-200"),
        mods,
        sections,
        div(
          cls("pt-5"),
          div(cls("flex justify-end"), actions)
        )
      )

    def inlineForm(
        mods: Modifier[HtmlElement]*
    ): HtmlElement =
      L.form(cls("flex space-x-4"), mods)

    def inputField(
        id: String,
        labelText: String,
        placeholderText: Option[String] = None,
        inputType: String = "text",
        helpText: Option[String] = None
    ): HtmlElement =
      field(
        id,
        labelText,
        input(
          tpe(inputType),
          cls(
            "block w-full rounded-md border-0 py-1.5 text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 placeholder:text-gray-400 focus:ring-2 focus:ring-inset focus:ring-indigo-600 sm:max-w-xs sm:text-sm sm:leading-6"
          ),
          placeholderText.map(placeholder(_))
        ),
        helpText
      )
