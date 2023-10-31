package works.iterative.ui.components.laminar
package tailwind
package ui

import com.raquo.laminar.api.L.*

trait LayoutModule:
  object layout:
    def cardMod: HtmlMod = cls("bg-white shadow px-4 py-5 sm:rounded-lg sm:p-6")
    def card(content: Modifier[HtmlElement]*): HtmlElement =
      div(cardMod, content)
