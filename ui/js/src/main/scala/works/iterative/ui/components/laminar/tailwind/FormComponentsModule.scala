package works.iterative.ui.components.laminar
package tailwind
package ui

import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html
import io.laminext.syntax.core.*

trait FormComponentsModule:
  self: IconsModule =>

  object forms:

    val inputClasses =
      "shadow-sm block focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm border border-gray-300 rounded-md"

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

    def label(
        labelText: String,
        forId: Option[String] = None,
        required: Boolean = false
    )(
        mods: Modifier[HtmlElement]*
    ): ReactiveHtmlElement[html.Label] =
      L.label(
        cls("block text-sm font-medium text-gray-700 sm:mt-px sm:pt-2"),
        forId.map(id => L.forId(id)),
        labelText,
        if required then sup(cls("text-gray-400"), "* povinné pole")
        else emptyMod,
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
    )(actions: Modifier[HtmlElement]*): ReactiveHtmlElement[html.Form] =
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

    def errorTextMods: Modifier[HtmlElement] =
      cls("mt-2 text-sm text-red-600")

    def validationError(text: Modifier[HtmlElement]): HtmlElement =
      p(errorTextMods, text)

    def helpTextMods: Modifier[HtmlElement] =
      cls("mt-2 text-sm text-gray-500")

    def fieldHelp(text: Modifier[HtmlElement]): HtmlElement =
      p(helpTextMods, text)

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
        input(id, inputType, placeholderText)(),
        helpText
      )

    def input(
        id: String,
        inputType: String = "text",
        placeholderText: Option[String] = None
    )(mods: HtmlMod*): HtmlElement =
      L.input(
        cls(inputClasses),
        idAttr(id),
        nameAttr(id),
        placeholderText.map(placeholder(_)),
        tpe(inputType),
        mods
      )

    def comboBoxSimple(
        options: List[(String, String)],
        selectedInitially: Option[String] = None,
        id: Option[String] = None,
        name: Option[String] = None
    ): HtmlElement =
      val expanded = Var(false)
      val selected = Var(selectedInitially)
      div(
        cls("relative mt-2"),
        L.input(
          cls(
            "w-full rounded-md border-0 bg-white py-1.5 pl-3 pr-12 text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-inset focus:ring-indigo-600 sm:text-sm sm:leading-6"
          ),
          id.map(idAttr(_)),
          name.map(nameAttr(_)),
          tpe("text"),
          role("combobox"),
          aria.controls("options"),
          aria.expanded <-- expanded
        ),
        button(
          cls(
            "absolute inset-y-0 right-0 flex items-center rounded-r-md px-2 focus:outline-none"
          ),
          tpe("button"),
          icons.chevronUpDown(svg.cls("h-5 w-5 text-gray-400")),
          onClick.preventDefault --> (_ => expanded.toggle())
        ),
        ul(
          cls(
            "absolute z-10 mt-1 max-h-60 w-full overflow-auto rounded-md bg-white py-1 text-base shadow-lg ring-1 ring-black ring-opacity-5 focus:outline-none sm:text-sm"
          ),
          cls.toggle("hidden") <-- expanded.signal.not,
          id.map(i => idAttr(s"${i}-options")),
          role := "listbox",
          // Combobox option, manage highlight styles based on mouseenter/mouseleave and keyboard navigation.
          // Active: "text-white bg-indigo-600", Not Active: "text-gray-900"
          for (((v, l), i) <- options.zipWithIndex)
            yield
              val active = Var(false)
              val isSelected = selected.signal.map(_.contains(v))
              li(
                cls(
                  "relative cursor-default select-none py-2 pl-3 pr-9 text-gray-900"
                ),
                cls.toggle("text-white bg-indigo-600") <-- active.signal,
                cls.toggle("text-gray-900") <-- active.signal.not,
                cls.toggle("font-semibold") <-- isSelected,
                id.map(cid => idAttr(s"${cid}-option-${i}")),
                role := "option",
                tabIndex := -1,
                // Selected: "font-semibold"
                span(cls("block truncate"), l),
                // Checkmark, only display for selected option.
                // Active: "text-white", Not Active: "text-indigo-600"
                isSelected.childWhenTrue(
                  span(
                    cls("absolute inset-y-0 right-0 flex items-center pr-4"),
                    cls.toggle("text-indigo-600") <-- active.signal.not,
                    cls.toggle("text-white") <-- active.signal,
                    icons.check(svg.cls("h-5 w-5"))
                  )
                ),
                onClick.preventDefault.mapTo(
                  v
                ) --> selected.writer.contramapSome,
                onMouseEnter --> (_ => active.set(true)),
                onMouseLeave --> (_ => active.set(false))
              )
          // More items...
        )
      )
