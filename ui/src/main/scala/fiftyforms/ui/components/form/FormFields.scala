package fiftyforms.ui.components.form

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom

object FormFields:
  def apply(
      mods: Modifier[ReactiveHtmlElement[dom.HTMLElement]]*
  ): HtmlElement =
    div(
      cls := "mt-6 sm:mt-5 space-y-6 sm:space-y-5",
      mods
    )
