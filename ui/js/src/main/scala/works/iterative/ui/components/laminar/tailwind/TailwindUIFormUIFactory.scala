package works.iterative.ui.components.laminar.tailwind.ui

import works.iterative.ui.components.laminar.forms.FormUIFactory
import com.raquo.laminar.nodes.ReactiveHtmlElement
import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.*
import TailwindUICatalogue.{forms, buttons, icons}
import org.scalajs.dom.html
import io.laminext.syntax.core.*
import com.raquo.laminar.tags.HtmlTag

trait TailwindUIFormUIFactory extends FormUIFactory:
  override def form(mods: HtmlMod*)(sections: HtmlMod*)(
      actions: HtmlMod*
  ): ReactiveHtmlElement[html.Form] =
    forms.form(mods*)(sections*)(actions*)

  override def section(title: HtmlMod, subtitle: Option[HtmlMod])(
      content: HtmlMod*
  ): L.HtmlElement =
    forms.section(title, subtitle)(content*)

  override def label(
      labelText: String,
      forId: Option[String] = None,
      required: Boolean = false
  )(
      mods: HtmlMod*
  ): ReactiveHtmlElement[html.Label] =
    forms.label(labelText, forId, required)(mods*)

  override def field(label: HtmlMod)(content: HtmlMod*): HtmlElement =
    forms.field(label)(content*)

  override def submit(text: HtmlMod)(mods: HtmlMod*): HtmlElement =
    buttons.primaryButton(text, None, None, "submit")(mods)

  override def cancel(text: HtmlMod)(mods: HtmlMod*): HtmlElement =
    buttons.secondaryButton(text, None, None, "button")(cls("mr-3"), mods)

  override def validationError(text: HtmlMod): HtmlElement =
    forms.validationError(text)

  override def errorTextMods: HtmlMod = forms.errorTextMods

  override def fieldHelp(text: HtmlMod): HtmlElement =
    forms.fieldHelp(text)

  override def helpTextMods: HtmlMod = forms.helpTextMods

  private def renderInput[Ref <: org.scalajs.dom.HTMLElement](
      as: HtmlTag[Ref],
      inError: Signal[Boolean],
      mods: HtmlMod,
      amendInput: ReactiveHtmlElement[Ref] => ReactiveHtmlElement[Ref]
  ): Div =
    div(
      cls("relative w-full sm:max-w-xs rounded-md shadow-sm"),
      amendInput(
        as(
          cls.toggle(
            "text-red-900 ring-red-300 placeholder:text-red-300 focus:ring-red-500"
          ) <-- inError,
          cls.toggle(
            "text-gray-900 ring-gray-300 placeholder:text-gray-400 focus:ring-indigo-600"
          ) <-- inError.not,
          cls(
            "block w-full rounded-md border-0 py-1.5 shadow-sm ring-1 ring-inset focus:ring-2 focus:ring-inset sm:max-w-xs sm:text-sm sm:leading-6"
          ),
          mods
        )
      ),
      inError.childWhenTrue(
        div(
          cls(
            "pointer-events-none absolute inset-y-0 right-0 pr-3 flex items-center"
          ),
          icons.`exclamation-circle-solid`(svg.cls("h-5 w-5 text-red-500"))
        )
      )
    )

  override def textarea(
      inError: Signal[Boolean],
      amendInput: TextArea => TextArea = identity
  )(mods: HtmlMod*): HtmlElement =
    renderInput(L.textArea, inError, mods, amendInput)

  override def input(
      inError: Signal[Boolean],
      amendInput: Input => Input = identity
  )(
      mods: HtmlMod*
  ): HtmlElement =
    renderInput(L.input, inError, mods, amendInput)

  override def select(
      inError: Signal[Boolean],
      amendInput: Select => Select = identity
  )(mods: HtmlMod*): HtmlElement =
    renderInput(L.select, inError, mods, amendInput)
  override object combobox extends FormUIFactory.ComboboxComponents:
    import works.iterative.ui.components.laminar.tailwind.ui.TailwindUICatalogue.combobox.simple
    override def container(
        inError: Signal[Boolean],
        amendInput: Input => Input = identity
    )(mods: HtmlMod*): HtmlElement =
      simple.container(cls("sm:max-w-xs"), input(inError, amendInput)(), mods)

    override def button(mods: HtmlMod*): HtmlElement = simple.button(mods)

    override def options(mods: HtmlMod*): HtmlElement = simple.options(mods)

    override def option(
        label: String,
        isActive: Signal[Boolean],
        isSelected: Signal[Boolean]
    )(mods: HtmlMod*): HtmlElement =
      simple.option(isActive, isSelected)(
        simple.optionValue(label),
        isSelected.childWhenTrue(simple.checkmark(isActive))
      )

  override def fileInput(title: String)(
      buttonMods: HtmlMod*
  )(
      inputMods: Mod[ReactiveHtmlElement[org.scalajs.dom.HTMLInputElement]]*
  ): HtmlElement =
    val selectedFile: Var[Option[String]] = Var(None)
    div(
      cls := "mt-4 sm:mt-0 sm:flex-none",
      L.label(
        cls("block w-full"),
        div(
          buttonMods,
          cls("cursor-pointer"),
          buttons.sharedButtonMod,
          buttons.secondaryButtonMod,
          child <-- selectedFile.signal
            .map(_.isDefined)
            .switch(
              icons.`paper-clip-solid`(svg.cls("w-6 h-6 mr-2")),
              icons.upload(svg.cls("w-6 h-6 mr-2"))
            ),
          span(child.text <-- selectedFile.signal.map(_.getOrElse(title)))
        ),
        L.input(
          cls("hidden"),
          tpe("file"),
          inputMods,
          inContext(thisNode =>
            onInput
              .mapTo(
                thisNode.ref.files.headOption.map(_.name)
              ) --> selectedFile.writer
          )
        )
      )
    )

object TailwindUIFormUIFactory extends TailwindUIFormUIFactory
