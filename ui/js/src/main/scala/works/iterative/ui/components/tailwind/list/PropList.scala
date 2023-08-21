package works.iterative.ui.components.tailwind
package list

import com.raquo.laminar.api.L.*

object PropList:
  type ViewModel = List[HtmlElement]
  def render($m: Signal[ViewModel]): HtmlElement =
    div(
      cls := "sm:flex",
      children <-- $m.map(_.zipWithIndex.map { case (i, idx) =>
        i.amend(
          cls := Map("mt-2 sm:mt-0 sm:ml-6" -> (idx == 0))
        )
      })
    )
