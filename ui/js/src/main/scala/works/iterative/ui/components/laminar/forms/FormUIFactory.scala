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

  def cancel(label: HtmlMod)(mods: HtmlMod*): HtmlElement

  def validationError(text: HtmlMod): HtmlElement

  def fieldHelp(text: HtmlMod): HtmlElement

  def helpTextMods: HtmlMod

  def errorTextMods: HtmlMod

  def input(inError: Signal[Boolean], amendInput: Input => Input = identity)(
      mods: HtmlMod*
  ): HtmlElement

  def textarea(
      inError: Signal[Boolean],
      amendInput: TextArea => TextArea = identity
  )(mods: HtmlMod*): HtmlElement

  def select(inError: Signal[Boolean], amendInput: Select => Select = identity)(
      mods: HtmlMod*
  ): HtmlElement

  def combobox: FormUIFactory.ComboboxComponents

  def fileInput(title: String)(
      buttonMods: HtmlMod*
  )(
      inputMods: Mod[ReactiveHtmlElement[org.scalajs.dom.HTMLInputElement]]*
  ): HtmlElement

object FormUIFactory:
  trait ComboboxComponents:
    def container(
        inError: Signal[Boolean],
        amendInput: Input => Input = identity
    )(mods: HtmlMod*): HtmlElement

    def button(mods: HtmlMod*): HtmlElement
    def options(mods: HtmlMod*): HtmlElement

    def option(
        label: String,
        isActive: Signal[Boolean],
        isSelected: Signal[Boolean]
    )(mods: HtmlMod*): HtmlElement
