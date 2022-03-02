package fiftyforms.ui.components
package list

import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom
import com.raquo.laminar.builders.HtmlTag

object IconText:
  case class ViewModel(text: HtmlElement, icon: SvgElement)
  def render($m: Signal[ViewModel]): HtmlElement = render($m, div)
  def render(
      $m: Signal[ViewModel],
      container: HtmlTag[dom.html.Element]
  ): HtmlElement =
    container(
      cls := "flex items-center text-sm text-gray-500",
      child <-- $m.map(_.icon),
      child <-- $m.map(_.text)
    )
