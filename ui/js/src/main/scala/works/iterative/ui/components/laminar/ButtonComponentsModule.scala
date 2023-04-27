package works.iterative.ui.components.laminar

import com.raquo.laminar.api.L.{*, given}
import works.iterative.ui.components.tailwind.ComponentContext

trait ButtonComponentsModule:
  def buttons: ButtonComponents

  trait ButtonComponents:
    def inlineButton(id: String, icon: SvgElement)(
        mods: Modifier[HtmlElement]*
    ): HtmlElement
    def iconButton(id: String, icon: SvgElement)(
        mods: Modifier[HtmlElement]*
    ): HtmlElement

trait DefaultButtonComponentsModule(using ctx: ComponentContext)
    extends ButtonComponentsModule:

  override val buttons = new ButtonComponents:

    private inline def srHelp(id: String): Modifier[HtmlElement] =
      ctx.messages
        .opt(s"form.button.${id}.screenReaderHelp")
        .map(sr => span(cls := "sr-only", sr))

    override def inlineButton(id: String, icon: SvgElement)(
        mods: Modifier[HtmlElement]*
    ): HtmlElement =
      button(
        tpe := "button",
        cls := "ml-1 inline-flex h-4 w-4 flex-shrink-0 rounded-full p-1 text-gray-400 hover:bg-gray-200 hover:text-gray-500",
        srHelp(id),
        icon
      )

    override def iconButton(id: String, icon: SvgElement)(
        mods: Modifier[HtmlElement]*
    ): HtmlElement =
      button(
        tpe := "button",
        cls := "inline-flex justify-center px-3.5 py-2 border border-gray-300 shadow-sm text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-pink-500",
        icon,
        srHelp(id)
      )
