package portaly.forms

import com.raquo.laminar.api.L.*
import works.iterative.autocomplete.ui.laminar.AutocompleteViews

trait Components extends AutocompleteViews:
    /** The global layout of the form and menu */
    def formLayout(theForm: HtmlMod, menu: HtmlMod): HtmlElement

    /** The layout of other major parts of the form, to fit the structure in formLayout For example
      * the buttons.
      */
    def formPart(mods: HtmlMod*): HtmlElement

    def formTitle(titleMod: HtmlMod, mods: HtmlMod*): HtmlElement

    def form(id: String, titleMod: HtmlMod, formMods: Option[HtmlMod], mods: HtmlMod*): HtmlElement

    def segmentRemoveIcon(mods: SvgMod*): SvgElement
    def attachmentRemoveIcon(mods: SvgMod*): SvgElement
    def attachmentIcon(mods: SvgMod*): SvgElement
    def uploadIcon(mods: SvgMod*): SvgElement

    def section(
        id: String,
        level: Int,
        titleMod: Option[HtmlMod],
        subtitleMod: Option[HtmlMod],
        errors: Signal[List[Node]],
        mods: HtmlMod*
    ): HtmlElement

    def labeledField(
        id: String,
        labelMod: HtmlMod,
        helpMod: Option[HtmlMod],
        required: Signal[Boolean],
        errors: Signal[List[Node]],
        mods: HtmlMod*
    ): HtmlElement

    def fileInput(
        id: String,
        fieldName: String,
        multiple: Boolean,
        labelMod: HtmlMod,
        inputMod: Mod[Input]
    ): HtmlElement

    def fileInputField(
        id: String,
        name: String,
        multiple: Boolean,
        inError: Signal[Boolean],
        buttonMod: HtmlMod
    ): HtmlElement

    def checkbox(
        id: String,
        name: String,
        value: Signal[String],
        labelMod: HtmlMod,
        descriptionMod: Option[HtmlMod],
        inputMods: HtmlMod*
    ): HtmlElement

    def radio(
        id: String,
        name: String,
        labelMod: HtmlMod,
        descriptionMod: Option[HtmlMod],
        required: Signal[Boolean],
        value: Signal[String],
        values: List[Components.RadioOption],
        mods: HtmlMod*
    ): HtmlElement

    def select(
        id: String,
        name: String,
        labelMod: HtmlMod,
        descriptionMod: Option[HtmlMod],
        required: Signal[Boolean],
        value: Signal[String],
        values: List[Components.RadioOption],
        mods: HtmlMod*
    ): HtmlElement

    def flexRow(mods: HtmlMod*): HtmlElement

    def button(
        id: String,
        name: String,
        buttonMod: HtmlMod,
        buttonType: String,
        mods: HtmlMod*
    ): HtmlElement

    def buttonLike: HtmlMod

    def grid(mods: HtmlMod*): HtmlElement

    def gridCell(span: Int, mods: HtmlMod*): HtmlElement
end Components

object Components:
    final case class RadioOption(
        id: String,
        value: String,
        text: Node,
        help: Option[Node],
        mods: HtmlMod*
    )
end Components
