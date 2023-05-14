package works.iterative
package ui.components
package tailwind

import com.raquo.laminar.api.L.{*, given}

object Layout:
  def card(content: Modifier[HtmlElement]*)(using
      cctx: ComponentContext[_]
  ): Div =
    div(cls("bg-white shadow sm:rounded-md overflow-hidden"), content)
