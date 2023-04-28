package works.iterative.ui.components.laminar

import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.{*, given}
import works.iterative.ui.components.tailwind.ComponentContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.raquo.laminar.keys.ReactiveProp
import com.raquo.domtypes.jsdom.defs.events.TypedTargetEvent
import org.scalajs.dom.html
import com.raquo.laminar.modifiers.KeyUpdater

trait FormComponentsModule extends LocalDateSelectModule:
  def forms: FormComponents

  trait FormComponents:
    def section(
        title: Modifier[HtmlElement],
        subtitle: Option[Modifier[HtmlElement]]
    )(
        content: Modifier[HtmlElement]*
    ): HtmlElement

    def field(label: Modifier[HtmlElement])(
        content: Modifier[HtmlElement]*
    ): HtmlElement

    def field(
        id: String,
        label: String,
        input: HtmlElement,
        help: Option[String]
    ): HtmlElement

    def form(mods: Modifier[HtmlElement]*)(sections: Modifier[HtmlElement]*)(
        actions: Modifier[HtmlElement]*
    ): HtmlElement

    def inlineForm(mods: Modifier[HtmlElement]*): HtmlElement

    def searchField(id: String, placeholderText: Option[String] = None)(
        mods: Modifier[HtmlElement]*
    ): HtmlElement

    def renderLocalDateSelect(
        id: String,
        labelText: Option[String],
        placeholderText: Option[String],
        mods: LocalDateSelect => Modifier[HtmlElement]
    ): HtmlElement

trait DefaultFormComponentsModule(using ctx: ComponentContext)
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
        label: String,
        input: HtmlElement,
        help: Option[String]
    ): HtmlElement =
      field(
        L.label(
          cls("block text-sm font-medium text-gray-700 sm:mt-px sm:pt-2"),
          forId(id),
          label
        )
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
        label(
          forId := id,
          cls := "sr-only",
          "Hledat"
        ),
        div(
          cls := "relative rounded-md shadow-sm",
          div(
            cls := "absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none",
            icons.`search-solid`()
          ),
          input(
            tpe := "search",
            name := "search",
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
          name(id),
          autoComplete("date"),
          tpe("date"),
          cls(
            "ml-2 shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm border-gray-300 rounded-md"
          ),
          placeholderText.map(placeholder(_)),
          mods(localDateSelect)
        )
      )
