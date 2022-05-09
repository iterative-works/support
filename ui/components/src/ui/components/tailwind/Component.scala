package works.iterative.ui.components.tailwind

import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom

trait HtmlComponent[Ref <: dom.html.Element, A]:
  extension (a: A) def element: ReactiveHtmlElement[Ref]

type BaseHtmlComponent[A] = HtmlComponent[dom.html.Element, A]
