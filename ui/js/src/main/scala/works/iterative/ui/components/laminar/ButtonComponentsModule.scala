package works.iterative.ui.components.laminar

import com.raquo.laminar.api.L.{*, given}
import works.iterative.ui.components.tailwind.ComponentContext

trait ButtonComponentsModule:
  def buttons: ButtonComponents

  trait ButtonComponents:
    def primaryButton(
        id: String,
        text: Modifier[HtmlElement],
        icon: Option[SvgElement] = None,
        buttonType: String = "submit"
    )(
        mods: Modifier[HtmlElement]*
    ): HtmlElement

    def secondaryButton(
        id: String,
        text: Modifier[HtmlElement],
        icon: Option[SvgElement] = None,
        buttonType: String = "button"
    )(
        mods: Modifier[HtmlElement]*
    ): HtmlElement

    def inlineButton(id: String, icon: SvgElement)(
        mods: Modifier[HtmlElement]*
    ): HtmlElement

    def iconButton(id: String, icon: SvgElement)(
        mods: Modifier[HtmlElement]*
    ): HtmlElement
