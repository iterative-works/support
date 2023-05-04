package works.iterative.ui.components.laminar
package tailwindui

import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.{*, given}
import works.iterative.ui.components.tailwind.ComponentContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.scalajs.dom.html
import com.raquo.laminar.modifiers.KeyUpdater

trait LabelsOnLeftFormComponentsModule(using ctx: ComponentContext)
    extends FormComponentsModule:
  self: IconsModule =>
  override val forms = new FormComponents:

    override def section(
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

    override def label(labelText: String, forId: Option[String] = None)(
        mods: Modifier[HtmlElement]*
    ): HtmlElement =
      L.label(
        cls("block text-sm font-medium text-gray-700 sm:mt-px sm:pt-2"),
        forId.map(id => L.forId(id)),
        labelText,
        mods
      )

    override def field(
        label: Modifier[HtmlElement]
    )(content: Modifier[HtmlElement]*): HtmlElement =
      div(
        cls(
          "sm:grid sm:grid-cols-3 sm:gap-4 sm:items-start sm:border-t sm:border-gray-200 sm:pt-5"
        ),
        label,
        div(cls("mt-1 sm:mt-0 sm:col-span-2"), content)
      )

    override def field(
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

    override def form(mods: Modifier[HtmlElement]*)(
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

    override def inlineForm(
        mods: Modifier[HtmlElement]*
    ): HtmlElement =
      L.form(cls("flex space-x-4"), mods)

    override def searchField(
        id: String,
        placeholderText: Option[String] = None
    )(
        mods: Modifier[HtmlElement]*
    ): HtmlElement =
      div(
        cls := "flex-1 min-w-0",
        label(ctx.messages("forms.search.label"), Some(id))(cls("sr-only")),
        div(
          cls := "relative rounded-md shadow-sm",
          div(
            cls := "absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none",
            icons.`search-solid`()
          ),
          input(
            tpe := "search",
            nameAttr := "search",
            idAttr := id,
            cls := "focus:ring-pink-500 focus:border-pink-500 block w-full pl-10 sm:text-sm border-gray-300 rounded-md",
            placeholderText
              .orElse(
                ctx.messages
                  .opt(
                    s"forms.search.${id}.placeholder",
                    s"form.search.placeholder"
                  )
              )
              .map(placeholder(_)),
            mods
          )
        )
      )

    override def inputField(
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

    override def renderLocalDateSelect(
        id: String,
        labelText: Option[String],
        placeholderText: Option[String],
        mods: LocalDateSelect => Modifier[HtmlElement]
    ): HtmlElement =
      div(
        labelText,
        input(
          idAttr(id),
          nameAttr(id),
          autoComplete("date"),
          tpe("date"),
          cls(
            "ml-2 shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm border-gray-300 rounded-md"
          ),
          placeholderText.map(placeholder(_)),
          mods(localDateSelect)
        )
      )
