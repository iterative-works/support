package portaly.forms

import com.raquo.laminar.api.L.*
import com.raquo.laminar.api.L

trait ReadOnlyComponents:
    def form(id: String, titleMod: HtmlMod, mods: HtmlMod*): HtmlElement

    def section(
        id: String,
        level: Int,
        titleMod: Option[HtmlMod],
        subtitleMod: Option[HtmlMod],
        mods: HtmlMod*
    ): HtmlElement

    def labeledField(
        id: String,
        labelMod: HtmlMod,
        required: Boolean,
        inline: Boolean,
        mods: HtmlMod*
    ): HtmlElement

    def inputValue(id: String, raw: String, v: Node): Node

    def fileValue(id: String, v: Node): Node

    def flexRow(mods: HtmlMod*): HtmlElement

    def grid(mods: HtmlMod*): HtmlElement

    def gridCell(span: Int, mods: HtmlMod*): HtmlElement
end ReadOnlyComponents
