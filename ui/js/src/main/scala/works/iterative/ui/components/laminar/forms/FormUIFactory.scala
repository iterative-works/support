package works.iterative.ui.components.laminar.forms

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html

trait FormUIFactory:
  def form(mods: HtmlMod*)(sections: HtmlMod*)(
      actions: HtmlMod*
  ): ReactiveHtmlElement[html.Form]

  def section(title: HtmlMod, subtitle: Option[HtmlMod])(
      content: HtmlMod*
  ): HtmlElement

  def label(labelText: String, forId: Option[String] = None)(
      mods: HtmlMod*
  ): ReactiveHtmlElement[html.Label]

  def field(label: HtmlMod)(content: HtmlMod*): HtmlElement

  def submit(label: HtmlMod): HtmlElement

  def validationError(text: HtmlMod): HtmlElement

  def input(
      name: String,
      id: Option[String] = None,
      placeholder: Option[String] = None
  )(
      mods: HtmlMod*
  ): ReactiveHtmlElement[html.Input]
