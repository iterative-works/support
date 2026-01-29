package works.iterative.ui
package components
package laminar

import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveHtmlElement
import com.raquo.laminar.tags.HtmlTag
import org.scalajs.dom
import model.Computable

trait ComputableComponents:
    def renderComputable[Ref <: dom.html.Element](
        as: HtmlTag[Ref],
        mods: HtmlMod*
    )(
        c: Signal[Computable[HtmlElement]]
    ): ReactiveHtmlElement[Ref]
    def renderComputable(c: Signal[Computable[HtmlElement]]): Div =
        renderComputable(div)(c)
    def renderComputable(mods: HtmlMod)(c: Signal[Computable[HtmlElement]]): Div =
        renderComputable(div, mods)(c)
end ComputableComponents
