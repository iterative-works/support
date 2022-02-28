package cz.e_bs.cmi.mdr.pdb.app.components.form

import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom
import com.raquo.laminar.nodes.ReactiveHtmlElement

object Form:
  val Body = FormBody
  val Section = FormSection
  val Row = FormRow

  def apply(body: HtmlElement, buttons: HtmlElement): HtmlElement =
    form(
      cls := "space-y-8 divide-y divide-gray-200",
      body,
      buttons
    )
