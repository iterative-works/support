package works.iterative.ui.components.laminar.forms

import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html

trait FormUIFactory:
  def form(mods: HtmlMod*)(sections: HtmlMod*)(
      actions: HtmlMod*
  ): ReactiveHtmlElement[html.Form]

  def section(title: HtmlMod, subtitle: Option[HtmlMod])(
      content: HtmlMod*
  ): HtmlElement

  def label(
      labelText: String,
      forId: Option[String] = None,
      required: Boolean = false
  )(
      mods: HtmlMod*
  ): ReactiveHtmlElement[html.Label]

  def field(label: HtmlMod)(content: HtmlMod*): HtmlElement

  def submit(label: HtmlMod)(mods: HtmlMod*): HtmlElement

  def validationError(text: HtmlMod): HtmlElement

  def fieldHelp(text: HtmlMod): HtmlElement

  def helpTextMods: HtmlMod

  def errorTextMods: HtmlMod

  def input(inError: Signal[Boolean])(mods: HtmlMod*): HtmlElement

  def fileInput(title: String)(
      buttonMods: HtmlMod*
  )(
      inputMods: Mod[ReactiveHtmlElement[org.scalajs.dom.HTMLInputElement]]*
  ): HtmlElement
