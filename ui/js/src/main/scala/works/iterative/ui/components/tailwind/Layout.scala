package works.iterative
package ui.components.tailwind

import com.raquo.laminar.api.L.{*, given}

object Layout:
  def card(content: Modifier[HtmlElement]*)(using cctx: ComponentContext): Div =
    div(cls(cctx.style.card), content)
