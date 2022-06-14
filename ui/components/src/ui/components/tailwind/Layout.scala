package works.iterative
package ui.components.tailwind

import com.raquo.laminar.api.L.{*, given}

object Layout:
  def card(content: Modifier[HtmlElement]*): Div =
    div(cls("bg-white shadow overflow-hidden sm:rounded-md"), content)
