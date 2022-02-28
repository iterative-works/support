package cz.e_bs.cmi.mdr.pdb.app.components.form

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement

object FormSection:
  def apply(
      header: HtmlElement,
      rows: HtmlElement*
  ): HtmlElement =
    div(
      cls := "space-y-6 sm:space-y-5",
      header,
      rows
    )
