package works.iterative.ui.components.tailwind

import com.raquo.laminar.nodes.ReactiveHtmlElement
import com.raquo.laminar.nodes.ReactiveSvgElement
import org.scalajs.dom

trait HtmlComponent[Ref <: dom.html.Element, -A]:
    extension (a: A) def element: ReactiveHtmlElement[Ref] = render(a)
    def render(a: A): ReactiveHtmlElement[Ref]

type BaseHtmlComponent[-A] = HtmlComponent[dom.html.Element, A]

trait SvgComponent[Ref <: dom.svg.Element, -A]:
    extension (a: A) def element: ReactiveSvgElement[Ref] = render(a)
    def render(a: A): ReactiveSvgElement[Ref]
