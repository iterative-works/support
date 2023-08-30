package works.iterative.ui.components.tailwind
package list

import com.raquo.laminar.api.L.*

object RowNext:
  def render: HtmlElement =
    div(
      cls := "flex-shrink-0",
      Icons.solid.`chevron-right`("h-5 w-5 text-gray-400")
    )
