package works.iterative.ui.components.tailwind.form

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement

object FormSection:
  @deprecated("use specific form's section method (see LabelsOnLeft)")
  def apply(
      header: HtmlElement,
      rows: HtmlElement*
  ): HtmlElement =
    div(
      cls := "space-y-6 sm:space-y-5",
      header,
      rows
    )
