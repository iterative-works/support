package works.iterative.ui.components.laminar
package tailwind
package ui

import com.raquo.laminar.api.L.{*, given}
import works.iterative.ui.components.tailwind.ComponentContext

trait LayoutModule:
  object layout:
    def card(content: Modifier[HtmlElement]*): HtmlElement =
      div(cls("bg-white shadow px-4 py-5 sm:rounded-lg sm:p-6"), content)
