package works.iterative.autocomplete.ui.laminar

import com.raquo.laminar.api.L.*
import works.iterative.ui.laminar.InputFieldViews

trait AutocompleteViews extends InputFieldViews:
    def comboContainer(mods: HtmlMod*): HtmlElement

    def comboOptionsContainer(mods: HtmlMod*): HtmlElement

    def comboOption(
        active: Signal[Boolean],
        selected: Signal[Boolean],
        text: Node,
        description: Option[Node],
        mods: HtmlMod*
    ): HtmlElement

    def comboButton(mods: HtmlMod*): HtmlElement
end AutocompleteViews
