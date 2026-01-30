package works.iterative
package ui.components.headless

import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveHtmlElement

object Toggle:

    final case class Ctx(
        trigger: Modifier[HtmlElement],
        toggle: Seq[HtmlElement] => Signal[Seq[HtmlElement]]
    )

    def apply[U <: org.scalajs.dom.html.Element](
        children: Ctx => ReactiveHtmlElement[U]
    ): ReactiveHtmlElement[U] = apply(true)(children)

    def apply[U <: org.scalajs.dom.html.Element](initialValue: Boolean)(
        children: Ctx => ReactiveHtmlElement[U]
    ): ReactiveHtmlElement[U] =
        val state: Var[Boolean] = Var(initialValue)
        children(
            Ctx(
                onClick.compose(_.sample(state).map(v => !v)) --> state,
                el =>
                    state.signal.map {
                        case true => el
                        case _    => Nil
                    }
            )
        )
    end apply
end Toggle
