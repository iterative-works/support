package works.iterative.ui.components.tailwind

import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom

trait HtmlRenderable[A]:
  extension (a: A) def render: Modifier[HtmlElement]
