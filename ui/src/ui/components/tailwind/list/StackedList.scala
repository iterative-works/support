package works.iterative.ui.components.tailwind
package list

import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom.html.UList
import com.raquo.laminar.nodes.ReactiveHtmlElement

class StackedList[Item: AsListRow]:
  import StackedList.*
  def apply(items: List[Item]): ReactiveHtmlElement[UList] =
    ul(
      role := "list",
      cls := "divide-y divide-gray-200",
      items.map(d => d.asListRow)
    )

  def withMod(
      items: List[Item]
  ): Modifier[HtmlElement] => ReactiveHtmlElement[UList] = mods =>
    ul(
      role := "list",
      cls := "divide-y divide-gray-200",
      mods,
      items.map(d => d.asListRow)
    )

  def grouped(items: List[Item], groupBy: Item => String): List[HtmlElement] =
    items.groupBy(groupBy).to(List).sortBy(_._1).map { case (c, i) =>
      withHeader(c)(withMod(i))
    }

object StackedList:
  def withHeader(header: String)(
      content: Modifier[HtmlElement] => ReactiveHtmlElement[UList]
  ): HtmlElement =
    div(
      cls("relative"),
      h3(
        cls(
          "z-10 sticky top-0 border-t border-b border-gray-200 bg-gray-50 px-6 py-1 text-sm font-medium text-gray-500"
        ),
        header
      ),
      content(cls("relative"))
    )
