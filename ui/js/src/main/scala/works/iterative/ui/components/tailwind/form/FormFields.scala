package works.iterative.ui.components.tailwind.form

import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom

object FormFields:
  @deprecated("use LabelsOnLeft.fields")
  def apply(
      mods: Modifier[ReactiveHtmlElement[dom.HTMLElement]]*
  ): HtmlElement =
    div(
      cls := "mt-6 sm:mt-5 space-y-6 sm:space-y-5",
      mods
    )
