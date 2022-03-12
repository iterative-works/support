package works.iterative.ui.components.tailwind
package list

import com.raquo.laminar.api.L.{*, given}

object RowNext:
  def render: HtmlElement =
    div(
      cls := "flex-shrink-0",
      Icons.solid.`chevron-right`().amend(svg.cls := "text-gray-400")
    )
