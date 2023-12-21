package works.iterative.ui.laminar

import com.raquo.laminar.api.L.*

trait InputFieldViews:
    def inputFieldContainer(
        inError: Signal[Boolean],
        input: HtmlElement,
        mods: HtmlMod*
    ): HtmlElement

    def inputField(
        id: String,
        fieldName: String,
        inError: Signal[Boolean],
        inputMods: HtmlMod*
    ): Input
end InputFieldViews
