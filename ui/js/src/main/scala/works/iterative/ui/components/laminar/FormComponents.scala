package works.iterative.ui.components.laminar

import com.raquo.laminar.api.L.*
import works.iterative.ui.components.ComponentContext

trait FormComponents(using ctx: ComponentContext[_])
    extends LocalDateSelectModule:
  def searchIcon: SvgElement

  def renderLocalDateSelect(
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

  def searchField(
      id: String,
      placeholderText: Option[String] = None
  )(
      mods: Modifier[HtmlElement]*
  ): HtmlElement =
    div(
      cls := "flex-1 min-w-0",
      label(ctx.messages("forms.search.label"), forId(id), cls("sr-only")),
      div(
        cls := "relative rounded-md shadow-sm",
        div(
          cls := "absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none",
          searchIcon
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
