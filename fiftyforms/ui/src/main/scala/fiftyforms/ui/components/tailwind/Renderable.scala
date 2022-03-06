package fiftyforms.ui.components.tailwind

import com.raquo.laminar.api.L.{*, given}

trait Renderable[A]:
  extension (a: A) def toHtml: HtmlElement
