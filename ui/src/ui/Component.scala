package ui

import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom

trait Component[Ref <: dom.html.Element]:
  def element: ReactiveHtmlElement[Ref]

object Component:
  given [Ref <: dom.html.Element]
      : Conversion[Component[Ref], ReactiveHtmlElement[Ref]] with
    def apply(component: Component[Ref]): ReactiveHtmlElement[Ref] =
      component.element
