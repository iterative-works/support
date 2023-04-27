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
    def form(mods: Modifier[HtmlElement]*): HtmlElement

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

    override def form(
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
