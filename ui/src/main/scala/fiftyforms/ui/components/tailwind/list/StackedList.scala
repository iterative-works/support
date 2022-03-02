package fiftyforms.ui.components.tailwind
package list

import com.raquo.laminar.api.L.{*, given}

class StackedList[Item]:
  type ViewModel = List[Item]
  def apply(
      $m: Signal[ViewModel],
      keyF: Item => String
  )(f: Signal[Item] => Signal[ListRow.ViewModel]): HtmlElement =
    ul(
      role := "list",
      cls := "divide-y divide-gray-200",
      children <-- $m.split(keyF)((_, _, $d) => ListRow(f($d)))
    )
